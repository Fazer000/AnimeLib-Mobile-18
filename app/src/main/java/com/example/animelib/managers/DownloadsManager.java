package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;

import com.example.animelib.api.ApiService;
import com.example.animelib.api.KodikLinksExtractor;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.DownloadedAnimeEntity;
import com.example.animelib.data.entity.DownloadedEpisodeEntity;
import com.example.animelib.models.DownloadTask;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.ui.VideoUrlHelper;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Менеджер скачивания серий в локальный кэш приложения
 */
public class DownloadsManager {
    private static final String TAG = "DownloadsManager";
    private static final int BUFFER_SIZE = 64 * 1024;

    public interface DownloadCallback {
        void onProgress(int percent);
        void onFinished(String localPath);
        void onError(String message);
    }

    private final Context context;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final ApiService apiService;
    private final DatabaseManager databaseManager;
    private final KodikLinksExtractor kodikExtractor;

    private volatile boolean running;
    private volatile boolean cancelled;

    public DownloadsManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.apiService = new ApiService(this.context);
        this.databaseManager = new DatabaseManager(this.context);
        this.kodikExtractor = new KodikLinksExtractor(this.client, new Gson());
    }

    public boolean isRunning() {
        return running;
    }

    public void cancel() {
        cancelled = true;
    }

    /**
     * Скачивает указанную задачу скачивания эпизода
     */
    public void downloadTask(DownloadTask task, DownloadCallback callback) {
        if (running) {
            callback.onError("Скачивание уже идёт");
            return;
        }
        running = true;
        cancelled = false;

        executor.execute(() -> {
            File outputFile = null;
            int maxRetries = 5;
            int attempt = 0;
            String lastError = null;

            while (attempt < maxRetries && !cancelled) {
                attempt++;
                try {
                    // 1. Получаем данные о ссылке на видео
                    String videoUrl = resolveVideoUrl(task);
                    if (videoUrl == null || videoUrl.isEmpty()) {
                        finish(callback, null, "Не удалось получить ссылку на видео для серии " + task.getEpisodeNumber());
                        return;
                    }

                    // Нормализация схемы URL
                    if (videoUrl.startsWith("//")) {
                        videoUrl = "https:" + videoUrl;
                    } else if (videoUrl.startsWith("/")) {
                        videoUrl = "https://kodik.info" + videoUrl;
                    } else if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                        videoUrl = "https://" + videoUrl;
                    }

                    // 2. Создаем файл в кэше приложения
                    File dir = new File(context.getExternalFilesDir("cached_episodes"), task.getAnimeId());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String sanitizeEpNum = task.getEpisodeNumber() != null ? task.getEpisodeNumber().replaceAll("[^a-zA-Z0-9_.]", "_") : "0";
                    String sanitizeTeam = task.getTeamName() != null ? task.getTeamName().replaceAll("[^a-zA-Z0-9А-Яа-я_]", "_") : "team";
                    String fileName = "ep_" + task.getEpisodeId() + "_" + sanitizeEpNum + "_" + sanitizeTeam + ".mp4";
                    outputFile = new File(dir, fileName);

                    Log.d(TAG, "Download attempt " + attempt + "/" + maxRetries + " to: " + outputFile.getAbsolutePath() + " url: " + videoUrl);

                    // 3. Выполняем скачивание (HLS .m3u8 или прямой MP4)
                    if (videoUrl.contains(".m3u8")) {
                        downloadHlsStream(videoUrl, outputFile, callback);
                    } else {
                        downloadDirectFile(videoUrl, outputFile, "https://animelib.me/", callback);
                    }

                    if (cancelled) {
                        if (outputFile.exists()) outputFile.delete();
                        finish(callback, null, "Скачивание отменено");
                        return;
                    }

                    // 4. Сохраняем информацию в базу данных Room
                    long now = System.currentTimeMillis();
                    String localPosterPath = downloadAndSavePoster(task.getAnimeId(), task.getPosterUrl());
                    DownloadedAnimeEntity animeEntity = new DownloadedAnimeEntity(
                            task.getAnimeId(),
                            task.getAnimeTitle() != null ? task.getAnimeTitle() : "Аниме #" + task.getAnimeId(),
                            localPosterPath != null ? localPosterPath : task.getPosterUrl(),
                            now
                    );
                    databaseManager.saveDownloadedAnime(animeEntity);

                    String epId = task.getAnimeId() + "_" + task.getEpisodeNumber() + "_" + task.getTeamName();
                    DownloadedEpisodeEntity episodeEntity = new DownloadedEpisodeEntity(
                            epId,
                            task.getAnimeId(),
                            task.getAnimeTitle(),
                            task.getEpisodeNumber(),
                            task.getEpisodeName() != null ? task.getEpisodeName() : "Серия " + task.getEpisodeNumber(),
                            task.getTeamName(),
                            task.getPlayerType(),
                            outputFile.getAbsolutePath(),
                            outputFile.length(),
                            now,
                            task.getQuality()
                    );
                    databaseManager.saveDownloadedEpisode(episodeEntity);

                    Log.d(TAG, "Successfully downloaded and stored episode: " + epId);
                    finish(callback, outputFile.getAbsolutePath(), null);
                    return;

                } catch (Exception e) {
                    Log.w(TAG, "Download attempt " + attempt + " failed: " + e.getMessage(), e);
                    lastError = e.getMessage() != null ? e.getMessage() : "Ошибка скачивания";
                    if (cancelled) {
                        if (outputFile != null && outputFile.exists()) outputFile.delete();
                        finish(callback, null, "Скачивание отменено");
                        return;
                    }
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }

            if (outputFile != null && outputFile.exists() && outputFile.length() == 0) {
                outputFile.delete();
            }
            finish(callback, null, lastError != null ? lastError : "Не удалось скачать после " + maxRetries + " попыток");
        });
    }

    private String resolveVideoUrl(DownloadTask task) throws Exception {
        final String[] resolvedUrl = new String[1];
        final Exception[] error = new Exception[1];
        CountDownLatch latch = new CountDownLatch(1);

        apiService.fetchEpisodeData(task.getEpisodeId(), new ApiService.EpisodeDataCallback() {
            @Override
            public void onEpisodeDataReceived(EpisodeResponse response) {
                try {
                    if (response != null && response.getData() != null && response.getData().getPlayers() != null) {
                        List<EpisodeResponse.PlayerData> players = response.getData().getPlayers();
                        for (EpisodeResponse.PlayerData p : players) {
                            boolean matchPlayer = task.getPlayerType().equalsIgnoreCase(p.getPlayer());
                            boolean matchTeam = p.getTeam() != null &&
                                    (p.getTeam().getId() == task.getTeamId() || task.getTeamName().equalsIgnoreCase(p.getTeam().getName()));

                            if (matchPlayer && matchTeam) {
                                if ("kodik".equalsIgnoreCase(task.getPlayerType())) {
                                    if (p.getSrc() != null && !p.getSrc().isEmpty()) {
                                        KodikResponse kodikRes = kodikExtractor.getLinks(p.getSrc());
                                        if (kodikRes != null && kodikRes.getData() != null) {
                                            String qualKey = task.getQuality();
                                            KodikResponse.VideoQuality[] vq = kodikRes.getData().get(qualKey);
                                            if (vq == null || vq.length == 0) {
                                                // Fallback to highest available quality
                                                for (String k : new String[]{"1080", "720", "480", "360"}) {
                                                    if (kodikRes.getData().containsKey(k)) {
                                                        vq = kodikRes.getData().get(k);
                                                        break;
                                                    }
                                                }
                                            }
                                            if (vq != null && vq.length > 0 && vq[0].getSrc() != null) {
                                                resolvedUrl[0] = vq[0].getSrc();
                                            }
                                        }
                                    }
                                } else {
                                    // AnimeLib player
                                    if (p.getVideo() != null && p.getVideo().getQuality() != null) {
                                        for (EpisodeResponse.QualityData qd : p.getVideo().getQuality()) {
                                            if (String.valueOf(qd.getQuality()).contains(task.getQuality())) {
                                                resolvedUrl[0] = VideoUrlHelper.toAbsoluteVideoUrl(qd.getHref());
                                                break;
                                            }
                                        }
                                        if (resolvedUrl[0] == null && !p.getVideo().getQuality().isEmpty()) {
                                            resolvedUrl[0] = VideoUrlHelper.toAbsoluteVideoUrl(p.getVideo().getQuality().get(0).getHref());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    error[0] = e;
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(String err) {
                error[0] = new Exception(err);
                latch.countDown();
            }
        });

        latch.await(20, TimeUnit.SECONDS);
        if (error[0] != null) throw error[0];

        if (resolvedUrl[0] != null && !resolvedUrl[0].isEmpty()) {
            if (resolvedUrl[0].startsWith("//")) {
                resolvedUrl[0] = "https:" + resolvedUrl[0];
            } else if (resolvedUrl[0].startsWith("/")) {
                resolvedUrl[0] = "https://kodik.info" + resolvedUrl[0];
            } else if (!resolvedUrl[0].startsWith("http://") && !resolvedUrl[0].startsWith("https://")) {
                resolvedUrl[0] = "https://" + resolvedUrl[0];
            }
        }

        return resolvedUrl[0];
    }

    private void downloadDirectFile(String url, File outputFile, String referer, DownloadCallback callback) throws IOException {
        long existingLength = outputFile.exists() ? outputFile.length() : 0;
        Request.Builder request = new Request.Builder().url(url);
        for (Map.Entry<String, String> header : VideoUrlHelper.getVideoHeaders(referer).entrySet()) {
            request.header(header.getKey(), header.getValue());
        }

        if (existingLength > 0) {
            request.header("Range", "bytes=" + existingLength + "-");
            Log.d(TAG, "Requesting range bytes=" + existingLength + "- for resume");
        }

        try (Response response = client.newCall(request.build()).execute()) {
            boolean isPartial = (response.code() == 206);
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Пустой ответ сервера");
            }

            long total = isPartial ? existingLength + body.contentLength() : body.contentLength();
            long written = isPartial ? existingLength : 0;
            boolean append = isPartial;
            int lastPercent = -1;
            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream in = body.byteStream(); FileOutputStream out = new FileOutputStream(outputFile, append)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelled) return;
                    out.write(buffer, 0, read);
                    written += read;
                    if (total > 0) {
                        int percent = (int) (written * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            int v = percent;
                            mainHandler.post(() -> callback.onProgress(v));
                        }
                    }
                }
                out.flush();
            }
        }
    }

    private void downloadHlsStream(String m3u8Url, File outputFile, DownloadCallback callback) throws IOException {
        Log.d(TAG, "Fetching HLS master playlist: " + m3u8Url);
        Request request = new Request.Builder().url(m3u8Url).build();
        String playlistContent;
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code() + " loading M3U8");
            }
            playlistContent = response.body().string();
        }

        String mediaPlaylistUrl = m3u8Url;
        if (playlistContent.contains("#EXT-X-STREAM-INF")) {
            // Master playlist: pick last stream (highest resolution)
            String bestSubUrl = null;
            String[] lines = playlistContent.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    bestSubUrl = resolveUrl(m3u8Url, line);
                }
            }
            if (bestSubUrl != null) {
                mediaPlaylistUrl = bestSubUrl;
                Log.d(TAG, "Selected sub-playlist URL: " + mediaPlaylistUrl);
                Request subReq = new Request.Builder().url(mediaPlaylistUrl).build();
                try (Response response = client.newCall(subReq).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        playlistContent = response.body().string();
                    }
                }
            }
        }

        // Extract TS segments
        List<String> segmentUrls = new ArrayList<>();
        for (String line : playlistContent.split("\n")) {
            line = line.trim();
            if (!line.startsWith("#") && !line.isEmpty()) {
                segmentUrls.add(resolveUrl(mediaPlaylistUrl, line));
            }
        }

        if (segmentUrls.isEmpty()) {
            throw new IOException("Не найдено сегментов в M3U8 плейлисте");
        }

        Log.d(TAG, "Total TS segments to download: " + segmentUrls.size());
        byte[] buffer = new byte[BUFFER_SIZE];
        int totalSegments = segmentUrls.size();

        // Use temporary folder for TS segments to enable resume
        File segsDir = new File(outputFile.getParentFile(), outputFile.getName() + "_segs");
        if (!segsDir.exists()) {
            segsDir.mkdirs();
        }

        // Validate that segsDir belongs to the same stream URL and segment count
        File infoFile = new File(segsDir, "playlist.info");
        boolean isSamePlaylist = false;
        if (infoFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(infoFile)))) {
                String savedUrl = reader.readLine();
                String savedCount = reader.readLine();
                if (mediaPlaylistUrl.equals(savedUrl) && String.valueOf(totalSegments).equals(savedCount)) {
                    isSamePlaylist = true;
                }
            } catch (Exception ignored) {}
        }

        if (!isSamePlaylist) {
            // Delete stale segments from a different episode or stream quality
            deleteDirContents(segsDir);
            try (FileOutputStream fos = new FileOutputStream(infoFile)) {
                fos.write((mediaPlaylistUrl + "\n" + totalSegments + "\n").getBytes());
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < totalSegments; i++) {
            if (cancelled) {
                return;
            }
            File segFile = new File(segsDir, String.format(java.util.Locale.US, "seg_%05d.ts", i));
            File tempSegFile = new File(segsDir, String.format(java.util.Locale.US, "seg_%05d.ts.tmp", i));

            if (!segFile.exists() || segFile.length() == 0) {
                String segUrl = segmentUrls.get(i);
                Request segReq = new Request.Builder().url(segUrl).build();
                try (Response response = client.newCall(segReq).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("HTTP " + response.code() + " downloading segment " + i);
                    }
                    try (InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(tempSegFile)) {
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            if (cancelled) {
                                tempSegFile.delete();
                                return;
                            }
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                    } catch (Exception e) {
                        if (tempSegFile.exists()) {
                            tempSegFile.delete();
                        }
                        throw e;
                    }

                    if (tempSegFile.exists()) {
                        if (segFile.exists()) segFile.delete();
                        tempSegFile.renameTo(segFile);
                    }
                }
            }
            int percent = (int) ((i + 1) * 100 / totalSegments);
            int p = percent;
            mainHandler.post(() -> callback.onProgress(p));
        }

        if (cancelled) return;

        // Concatenate segments into final outputFile
        try (FileOutputStream out = new FileOutputStream(outputFile, false)) {
            for (int i = 0; i < totalSegments; i++) {
                if (cancelled) {
                    if (outputFile.exists()) outputFile.delete();
                    return;
                }
                File segFile = new File(segsDir, String.format(java.util.Locale.US, "seg_%05d.ts", i));
                if (segFile.exists() && segFile.length() > 0) {
                    try (java.io.FileInputStream in = new java.io.FileInputStream(segFile)) {
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                } else {
                    if (outputFile.exists()) outputFile.delete();
                    throw new IOException("Отсутствует или поврежден сегмент #" + i + " при сборке видеофайла");
                }
            }
            out.flush();
        } catch (Exception e) {
            if (outputFile.exists()) outputFile.delete();
            throw e;
        }

        // Delete temporary segment directory
        deleteDir(segsDir);
    }

    private void deleteDirContents(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDir(f);
                    else f.delete();
                }
            }
        }
    }

    private void deleteDir(File dir) {
        if (dir != null && dir.exists()) {
            deleteDirContents(dir);
            dir.delete();
        }
    }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return baseUrl;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        if (relativeUrl.startsWith("//")) {
            return "https:" + relativeUrl;
        }
        if (relativeUrl.startsWith("/")) {
            try {
                java.net.URL base = new java.net.URL(baseUrl);
                return base.getProtocol() + "://" + base.getHost() + relativeUrl;
            } catch (Exception e) {
                return "https://kodik.info" + relativeUrl;
            }
        }
        int lastSlash = baseUrl.lastIndexOf('/');
        if (lastSlash != -1) {
            return baseUrl.substring(0, lastSlash + 1) + relativeUrl;
        }
        return relativeUrl;
    }

    private void finish(DownloadCallback callback, String localPath, String error) {
        running = false;
        mainHandler.post(() -> {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onFinished(localPath);
            }
        });
    }

    public String downloadAndSavePoster(String animeId, String posterUrl) {
        if (posterUrl == null || posterUrl.trim().isEmpty()) return posterUrl;
        if (posterUrl.startsWith("/")) {
            File f = new File(posterUrl);
            if (f.exists() && f.length() > 0) return posterUrl;
        }
        try {
            File dir = new File(context.getExternalFilesDir("cached_posters"), "");
            if (!dir.exists()) dir.mkdirs();
            File posterFile = new File(dir, animeId + ".jpg");
            if (posterFile.exists() && posterFile.length() > 0) {
                return posterFile.getAbsolutePath();
            }

            String url = posterUrl;
            if (url.startsWith("//")) {
                url = "https:" + url;
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(posterFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.flush();
                    }
                    return posterFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download poster image: " + e.getMessage());
        }
        return posterUrl;
    }

    public void cleanup() {
        cancel();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
