package com.example.animelib.util;

import com.example.animelib.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static volatile ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ImageLoader() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // 1/8 памяти под кэш
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static ImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) instance = new ImageLoader();
            }
        }
        return instance;
    }

    public void loadInto(ImageView imageView, String url, int placeholderResId) {
        int placeholder = placeholderResId != 0 ? placeholderResId : R.drawable.skeleton_placeholder;
        imageView.setImageResource(placeholder);
        if (url == null || url.isEmpty()) return;

        imageView.setTag(url);
        Bitmap cached = memoryCache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        executor.submit(() -> {
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap != null) memoryCache.put(url, bitmap);
            mainHandler.post(() -> {
                Object tag = imageView.getTag();
                if (tag != null && tag.equals(url) && bitmap != null) {
                    imageView.setAlpha(0f);
                    imageView.setImageBitmap(bitmap);
                    imageView.animate().alpha(1f).setDuration(200).start();
                }
            });
        });
    }

    private Bitmap downloadBitmap(String src) {
        if (src == null || src.trim().isEmpty()) return null;
        
        // Handle local file paths
        if (src.startsWith("/")) {
            try {
                java.io.File f = new java.io.File(src);
                if (f.exists() && f.length() > 0) {
                    return BitmapFactory.decodeFile(src);
                }
            } catch (Exception ignored) {}
            return null;
        }
        if (src.startsWith("file://")) {
            try {
                String path = src.substring(7);
                java.io.File f = new java.io.File(path);
                if (f.exists() && f.length() > 0) {
                    return BitmapFactory.decodeFile(path);
                }
            } catch (Exception ignored) {}
            return null;
        }

        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            String urlStr = src;
            if (urlStr.startsWith("//")) {
                urlStr = "https:" + urlStr;
            }
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }
}


