package com.example.animelib.data;

import android.content.Context;
import android.util.Log;

import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер для всех операций с базой данных Room
 * Выделен из ApiService для разделения ответственности
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    private final AppDatabase db;
    private final ExecutorService executor;
    private final Context context;

    public DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getDatabase(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // ========== AppSettings операции ==========

    /**
     * Получает URL сайта из базы данных. Вызывать вне главного потока
     */
    public String getSiteUrl() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            if (settings != null && settings.getSiteUrl() != null) {
                String url = settings.getSiteUrl();
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get site URL from DB", e);
        }
        return "https://" + context.getString(R.string.site_url);
    }
    
    /**
     * Сохраняет настройку 4K
     */
    public void save4KSetting(boolean enable4K) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnable4K(enable4K);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved 4K setting: " + enable4K);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save 4K setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку 4K
     */
    public boolean load4KSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnable4K();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 4K setting", e);
            return false;
        }
    }
    
    /**
     * Сохраняет настройку ambient light
     */
    public void saveAmbientLightSetting(boolean enableAmbientLight) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnableAmbientLight(enableAmbientLight);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved ambient light setting: " + enableAmbientLight);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save ambient light setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку ambient light
     */
    public boolean loadAmbientLightSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnableAmbientLight();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load ambient light setting", e);
            return false;
        }
    }

    /**
     * Сохраняет настройку объемного звука 5.1
     */
    public void saveSurroundSoundSetting(boolean enableSurroundSound) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnableSurroundSound(enableSurroundSound);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved surround sound setting: " + enableSurroundSound);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save surround sound setting", e);
            }
        });
    }

    /**
     * Сохраняет детальные параметры 3D пространственного звука
     */
    public void saveSurround3DSettings(int mode, float spatialWidth, float dialogueBoost, float bassBoost, float trebleBoost) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setSurroundMode(mode);
                settings.setSurroundSpatialWidth(spatialWidth);
                settings.setSurroundDialogueBoost(dialogueBoost);
                settings.setSurroundBassBoost(bassBoost);
                settings.setSurroundTrebleBoost(trebleBoost);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved 3D surround settings: mode=" + mode + ", width=" + spatialWidth + ", dialogue=" + dialogueBoost + ", bass=" + bassBoost + ", treble=" + trebleBoost);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save 3D surround settings", e);
            }
        });
    }

    /**
     * Загружает настройку объемного звука 5.1
     */
    public boolean loadSurroundSoundSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null || settings.isEnableSurroundSound(); // Default true
        } catch (Exception e) {
            Log.e(TAG, "Failed to load surround sound setting", e);
            return true;
        }
    }

    public int loadSurround3DMode() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null ? 0 : settings.getSurroundMode();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 3D surround mode", e);
            return 0;
        }
    }

    public float loadSurroundSpatialWidth() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null ? 1.0f : settings.getSurroundSpatialWidth();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 3D spatial width", e);
            return 1.0f;
        }
    }

    public float loadSurroundDialogueBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null ? 1.0f : settings.getSurroundDialogueBoost();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 3D dialogue boost", e);
            return 1.0f;
        }
    }

    public float loadSurroundBassBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null ? 1.0f : settings.getSurroundBassBoost();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 3D bass boost", e);
            return 1.0f;
        }
    }

    public float loadSurroundTrebleBoost() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings == null ? 1.0f : settings.getSurroundTrebleBoost();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 3D treble boost", e);
            return 1.0f;
        }
    }
    
    /**
     * Сохраняет настройку автовоспроизведения
     */
    public void saveAutoPlaySetting(boolean autoPlay) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setAutoPlay(autoPlay);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved autoPlay setting: " + autoPlay);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save autoPlay setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку автовоспроизведения
     */
    public boolean loadAutoPlaySetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isAutoPlay();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load autoPlay setting", e);
            return true; // Default to true
        }
    }
    
    /**
     * Сохраняет настройку длительности длинного пропуска
     */
    public void saveLongSkipDurationSetting(int duration) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setLongSkipDuration(duration);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved longSkipDuration setting: " + duration);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save longSkipDuration setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку длительности длинного пропуска
     */
    public int loadLongSkipDurationSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getLongSkipDuration() : 85; // Default to 85 seconds
        } catch (Exception e) {
            Log.e(TAG, "Failed to load longSkipDuration setting", e);
            return 85; // Default to 85 seconds
        }
    }
    
    /**
     * Сохраняет настройку темы
     */
    public void saveThemeSetting(int themeMode) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setThemeMode(themeMode);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved theme setting: " + themeMode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save theme setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку темы
     */
    public int loadThemeSetting() {

        AppSettings settings = db.appSettingsDao().getSettingsSync();
        return settings != null ? settings.getThemeMode() : 0; // Default to light theme (0)
    }
    
    // ========== Token операции ==========
    
    /**
     * Сохраняет токен в базу данных
     */
    public void saveToken(TokenEntity token) {
        executor.execute(() -> {
            try {
                db.tokenDao().insertOrUpdateToken(token);
                Log.d(TAG, "Saved token to database successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to save token", e);
            }
        });
    }
    
    /**
     * Получает токен из базы данных
     */
    public TokenEntity getToken() {
        try {
            return db.tokenDao().getToken();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get token from database", e);
            return null;
        }
    }
    
    /**
     * Проверяет есть ли токен в базе данных
     */
    public boolean hasToken() {
        try {
            return db.tokenDao().getTokenCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check token existence", e);
            return false;
        }
    }
    
    /**
     * Удаляет токен из базы данных
     */
    public void deleteToken() {
        executor.execute(() -> {
            try {
                db.tokenDao().deleteToken();
                Log.d(TAG, "Deleted token from database");
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete token", e);
            }
        });
    }
    
    // ========== CurrentEpisode операции ==========
    
    // ========== PlayerPreferences операции ==========
    
    /**
     * Сохраняет предпочтения по выбору плеера и озвучки
     */
    public void savePlayerPreferences(String player, Integer teamId) {
        executor.execute(() -> {
            try {
                // Загружаем существующую запись или создаем новую
                com.example.animelib.data.entity.PlayerPreferences preferences = 
                    db.playerPreferencesDao().getPreferencesSync();
                
                if (preferences == null) {
                    preferences = new com.example.animelib.data.entity.PlayerPreferences();
                }
                
                // Обновляем данные
                preferences.setPlayer(player);
                preferences.setTeamId(teamId);
                
                db.playerPreferencesDao().upsert(preferences);
                Log.d(TAG, "Saved player preferences: player=" + player + ", teamId=" + teamId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save player preferences", e);
            }
        });
    }
    
    /**
     * Сохраняет предпочтения по выбору плеера, озвучки и качества
     */
    public void savePlayerPreferences(String player, Integer teamId, String preferredQuality) {
        executor.execute(() -> {
            try {
                // Загружаем существующую запись или создаем новую
                com.example.animelib.data.entity.PlayerPreferences preferences = 
                    db.playerPreferencesDao().getPreferencesSync();
                
                if (preferences == null) {
                    preferences = new com.example.animelib.data.entity.PlayerPreferences();
                }
                
                // Обновляем данные
                preferences.setPlayer(player);
                preferences.setTeamId(teamId);
                preferences.setPreferredQuality(preferredQuality);
                
                db.playerPreferencesDao().upsert(preferences);
                Log.d(TAG, "Saved player preferences: player=" + player + ", teamId=" + teamId + 
                      ", quality=" + preferredQuality);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save player preferences", e);
            }
        });
    }
    
    /**
     * Загружает предпочтения по выбору плеера и озвучки
     */
    public com.example.animelib.data.entity.PlayerPreferences loadPlayerPreferences() {
        try {
            com.example.animelib.data.entity.PlayerPreferences prefs = db.playerPreferencesDao().getPreferencesSync();
            if (prefs != null) {
                Log.d(TAG, "Loaded player preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
            } else {
                Log.d(TAG, "No player preferences found in database");
            }
            return prefs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load player preferences", e);
            return null;
        }
    }

    // ========== Downloaded Anime & Episode операции ==========

    public void saveDownloadedAnime(com.example.animelib.data.entity.DownloadedAnimeEntity anime) {
        executor.execute(() -> {
            try {
                db.downloadedAnimeDao().insertAnime(anime);
                Log.d(TAG, "Saved downloaded anime: " + anime.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to save downloaded anime", e);
            }
        });
    }

    public void saveDownloadedEpisode(com.example.animelib.data.entity.DownloadedEpisodeEntity episode) {
        executor.execute(() -> {
            try {
                db.downloadedAnimeDao().insertEpisode(episode);
                Log.d(TAG, "Saved downloaded episode: " + episode.getEpisodeNumber() + " for anime " + episode.getAnimeTitle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to save downloaded episode", e);
            }
        });
    }

    public androidx.lifecycle.LiveData<java.util.List<com.example.animelib.data.entity.DownloadedAnimeEntity>> getAllDownloadedAnimeLiveData() {
        return db.downloadedAnimeDao().getAllDownloadedAnimeLiveData();
    }

    public androidx.lifecycle.LiveData<java.util.List<com.example.animelib.data.entity.DownloadedEpisodeEntity>> getEpisodesForAnimeLiveData(String animeId) {
        return db.downloadedAnimeDao().getEpisodesForAnimeLiveData(animeId);
    }

    public java.util.List<com.example.animelib.data.entity.DownloadedEpisodeEntity> getEpisodesForAnimeSync(String animeId) {
        return db.downloadedAnimeDao().getEpisodesForAnimeSync(animeId);
    }

    public com.example.animelib.data.entity.DownloadedEpisodeEntity findDownloadedEpisode(String animeId, String episodeNumber, String teamName) {
        return db.downloadedAnimeDao().findDownloadedEpisode(animeId, episodeNumber, teamName);
    }

    public com.example.animelib.data.entity.DownloadedEpisodeEntity findEpisodeByPath(String path) {
        return db.downloadedAnimeDao().findEpisodeByPath(path);
    }

    public void deleteDownloadedEpisode(String episodeId, String animeId) {
        executor.execute(() -> {
            try {
                com.example.animelib.data.entity.DownloadedEpisodeEntity ep = db.downloadedAnimeDao().getEpisodeById(episodeId);
                if (ep != null && ep.getLocalFilePath() != null) {
                    java.io.File file = new java.io.File(ep.getLocalFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                    java.io.File segsDir = new java.io.File(file.getParentFile(), file.getName() + "_segs");
                    if (segsDir.exists()) {
                        deleteDir(segsDir);
                    }
                }
                db.downloadedAnimeDao().deleteEpisodeById(episodeId);
                int count = db.downloadedAnimeDao().getEpisodeCountForAnime(animeId);
                if (count == 0) {
                    db.downloadedAnimeDao().deleteAnimeById(animeId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete downloaded episode: " + episodeId, e);
            }
        });
    }

    public void deleteDownloadedAnime(String animeId) {
        executor.execute(() -> {
            try {
                java.util.List<com.example.animelib.data.entity.DownloadedEpisodeEntity> episodes = db.downloadedAnimeDao().getEpisodesForAnimeSync(animeId);
                for (com.example.animelib.data.entity.DownloadedEpisodeEntity ep : episodes) {
                    if (ep.getLocalFilePath() != null) {
                        java.io.File file = new java.io.File(ep.getLocalFilePath());
                        if (file.exists()) {
                            file.delete();
                        }
                        java.io.File segsDir = new java.io.File(file.getParentFile(), file.getName() + "_segs");
                        if (segsDir.exists()) {
                            deleteDir(segsDir);
                        }
                    }
                }
                db.downloadedAnimeDao().deleteEpisodesForAnime(animeId);
                db.downloadedAnimeDao().deleteAnimeById(animeId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete downloaded anime: " + animeId, e);
            }
        });
    }

    private void deleteDir(java.io.File dir) {
        if (dir != null && dir.exists()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory()) deleteDir(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }
    
    /**
     * Закрывает executor при завершении работы
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
