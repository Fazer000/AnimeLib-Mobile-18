package com.example.animelib.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.AmbientLightView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер для управления ambient-подсветкой видеоплеера.
 * Оптимизированный анализ цветов с краев видео без лишних аллокаций.
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLight";
    
    // Интервал обновления ambient эффекта (мс)
    private static final long UPDATE_INTERVAL = 750;
    
    // Размер сэмпла для анализа цветов
    private static final int SAMPLE_SIZE = 30;
    
    // Количество источников света на каждой стороне
    private static final int LIGHTS_PER_SIDE = 3;
    
    private final Context context;
    private final PlayerView playerView;
    private final AmbientLightView ambientView;
    private final Handler handler;
    private final Runnable updateRunnable;
    private final ExecutorService backgroundExecutor;
    
    // Переиспользуемые структуры данных (0 аллокаций в рантайме)
    private final Bitmap sampleBitmap;
    private final int[] samplePixels;
    
    private ExoPlayer player;
    private boolean isEnabled = false;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isSuspended = false; // Временная приостановка во время UI interactions
    
    // Массивы цветов для каждой стороны
    private final int[] leftColors = new int[LIGHTS_PER_SIDE];
    private final int[] topColors = new int[LIGHTS_PER_SIDE];
    private final int[] rightColors = new int[LIGHTS_PER_SIDE];
    private final int[] bottomColors = new int[LIGHTS_PER_SIDE];
    
    private Player.Listener playerListener;

    public AmbientLightManager(@NonNull Context context, 
                               @NonNull PlayerView playerView, 
                               @NonNull AmbientLightView ambientView) {
        this.context = context;
        this.playerView = playerView;
        this.ambientView = ambientView;
        this.handler = new Handler(Looper.getMainLooper());
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        
        this.sampleBitmap = Bitmap.createBitmap(SAMPLE_SIZE, SAMPLE_SIZE, Bitmap.Config.RGB_565);
        this.samplePixels = new int[SAMPLE_SIZE * SAMPLE_SIZE];
        
        this.updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && isEnabled && !isSuspended) {
                    updateAmbientColors();
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
    }

    /**
     * Устанавливает ExoPlayer для захвата кадров
     */
    public void setPlayer(ExoPlayer player) {
        if (this.player != null && playerListener != null) {
            this.player.removeListener(playerListener);
        }
        
        this.player = player;
        
        if (player != null) {
            playerListener = new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePauseState(!isPlaying);
                }
            };
            player.addListener(playerListener);
        }
        
        if (isEnabled && player != null && !isRunning) {
            start();
            Log.d(TAG, "Started ambient light after player set");
        }
    }

    /**
     * Включает/выключает ambient эффект
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        
        if (enabled) {
            ambientView.setVisibility(View.VISIBLE);
            start();
        } else {
            ambientView.setVisibility(View.GONE);
            stop();
        }
        
        Log.d(TAG, "Ambient light " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    private void start() {
        if (!isRunning && player != null && !isSuspended) {
            isRunning = true;
            handler.removeCallbacks(updateRunnable);
            handler.post(updateRunnable);
            Log.d(TAG, "Ambient light started");
        }
    }

    private void stop() {
        isRunning = false;
        handler.removeCallbacks(updateRunnable);
        Log.d(TAG, "Ambient light stopped");
    }

    /**
     * Обновляет цвета ambient эффекта на основе текущего кадра
     */
    private void updateAmbientColors() {
        if (player == null || !player.isPlaying() || isPaused || isSuspended) {
            return;
        }

        try {
            SurfaceView surfaceView = findSurfaceView(playerView);
            if (surfaceView == null) {
                return;
            }

            PixelCopy.request(surfaceView, sampleBitmap, result -> {
                if (result == PixelCopy.SUCCESS && isRunning && !isSuspended) {
                    backgroundExecutor.execute(() -> {
                        synchronized (sampleBitmap) {
                            sampleBitmap.getPixels(samplePixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE);
                        }
                        extractColorsFromPixels();
                        updateAmbientView();
                    });
                }
            }, handler);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating ambient colors", e);
        }
    }

    /**
     * Извлекает доминирующие цвета с краев массива пикселей
     * Выполняется во вспомогательном потоке, 0 новых аллокаций памяти!
     */
    private void extractColorsFromPixels() {
        try {
            int segmentWidth = SAMPLE_SIZE / LIGHTS_PER_SIDE;
            int segmentHeight = SAMPLE_SIZE / LIGHTS_PER_SIDE;
            int edgeThickness = 2; // 2 пикселя
            
            // Левый край
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int y = i * segmentHeight;
                leftColors[i] = getAverageColor(samplePixels, SAMPLE_SIZE, 0, y, edgeThickness, segmentHeight);
            }
            
            // Верхний край
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int x = i * segmentWidth;
                topColors[i] = getAverageColor(samplePixels, SAMPLE_SIZE, x, 0, segmentWidth, edgeThickness);
            }
            
            // Правый край
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int y = i * segmentHeight;
                rightColors[i] = getAverageColor(samplePixels, SAMPLE_SIZE, SAMPLE_SIZE - edgeThickness, y, edgeThickness, segmentHeight);
            }
            
            // Нижний край
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int x = i * segmentWidth;
                bottomColors[i] = getAverageColor(samplePixels, SAMPLE_SIZE, x, SAMPLE_SIZE - edgeThickness, segmentWidth, edgeThickness);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting colors", e);
        }
    }

    private int getAverageColor(int[] pixels, int width, int startX, int startY, int rectWidth, int rectHeight) {
        long redSum = 0, greenSum = 0, blueSum = 0;
        int count = 0;
        int endX = Math.min(width, startX + rectWidth);
        int endY = Math.min(width, startY + rectHeight);
        
        for (int y = startY; y < endY; y++) {
            int rowOffset = y * width;
            for (int x = startX; x < endX; x++) {
                int pixel = pixels[rowOffset + x];
                redSum += Color.red(pixel);
                greenSum += Color.green(pixel);
                blueSum += Color.blue(pixel);
                count++;
            }
        }
        
        if (count == 0) return 0xFF000000;
        return Color.rgb((int) (redSum / count), (int) (greenSum / count), (int) (blueSum / count));
    }

    private void updateAmbientView() {
        if (ambientView != null && !isSuspended) {
            handler.post(() -> {
                try {
                    if (!isSuspended) {
                        ambientView.setColors(leftColors, topColors, rightColors, bottomColors);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating ambient view", e);
                }
            });
        }
    }

    private SurfaceView findSurfaceView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            if (child instanceof SurfaceView) {
                return (SurfaceView) child;
            } else if (child instanceof ViewGroup) {
                SurfaceView result = findSurfaceView((ViewGroup) child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void updatePauseState(boolean paused) {
        if (isPaused == paused) {
            return;
        }
        
        isPaused = paused;
        
        if (paused) {
            ambientView.dimToIntensity(0.3f);
        } else {
            ambientView.dimToIntensity(1.0f);
        }
    }
    
    /**
     * Временно приостанавливает ambient подсветку (при drag жестах, открытии штор)
     */
    public void suspend() {
        if (!isSuspended) {
            isSuspended = true;
            handler.removeCallbacks(updateRunnable);
            if (ambientView != null) {
                ambientView.suspend();
            }
            Log.d(TAG, "Ambient light suspended");
        }
    }
    
    /**
     * Возобновляет ambient подсветку
     */
    public void resume() {
        if (isSuspended) {
            isSuspended = false;
            if (ambientView != null) {
                ambientView.resume();
            }
            if (isEnabled && player != null) {
                isRunning = true;
                handler.removeCallbacks(updateRunnable);
                handler.post(updateRunnable);
            }
            Log.d(TAG, "Ambient light resumed");
        }
    }
    
    public void cleanup() {
        stop();
        backgroundExecutor.shutdown();
        
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
            playerListener = null;
        }
        
        player = null;
        Log.d(TAG, "Cleanup completed");
    }
}
