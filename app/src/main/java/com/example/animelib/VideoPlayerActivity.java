package com.example.animelib;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.SkeletonHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.C;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.models.AnimeInfoResponse;

import com.example.animelib.api.ApiService;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.managers.BookmarkManager;
import com.example.animelib.managers.CommentsManager;
import com.example.animelib.managers.EpisodesManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.managers.VerticalGesturesManager;
import com.example.animelib.managers.TimecodeManager;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.adapters.HorizontalRelatedTitlesAdapter;
import com.example.animelib.managers.RelatedTitlesManager;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.models.RelatedTitlesResponse;
import com.example.animelib.services.DownloadService;
import com.example.animelib.settings.QualityBottomSheet;
import com.example.animelib.ui.VideoUrlHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;


@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_VIDEO_URL = "video_url";
    public static final String EXTRA_ANIME_URL = "anime_url";

    private PlayerView playerView;
    private ExoPlayer player;
    private View loadingOverlay;
    private androidx.media3.ui.DefaultTimeBar timeBar;
    private View controllerView;

    private String currentVideoUrl;
    private String animeUrl;
    private Map<String, String> videoQualities;

    private ExecutorService executor;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;
    private ApiService apiService;

    // Menu components
    private ImageButton ibClosePlayer;
    private ImageButton menuToggleButton;
    private ImageButton settingsButton;
    private TextView settingsQualityTag;
    private ImageButton menuToggleFullscreen;
    private LinearLayout slidingMenuPanel;
    private View menuLoadingIndicator;
    private View menuLoadingOverlay;
    
    // Draggable panels
    private FrameLayout playerContainer;
    private float currentCornerRadiusPx = 0f;
    private float currentOutlineLeft = 0f;
    private float currentOutlineTop = 0f;
    private float currentOutlineRight = 0f;
    private float currentOutlineBottom = 0f;
    private com.example.animelib.ui.DraggableSidePanel menuPanelContainer;
    private com.example.animelib.ui.DraggableSidePanel commentsPanelContainer;
    
    // Anime info placeholder
    private View animeInfoPlaceholder;
    private boolean hasShownInitialAnimeInfo = false;
    private String currentPosterUrl = "";
    private ImageView animeInfoPoster;
    private TextView animeInfoTitle;
    private TextView animeInfoOriginalTitle;
    private TextView animeInfoYear;
    private TextView animeInfoType;
    private TextView animeInfoStatus;
    private TextView animeInfoRating;
    private TextView animeInfoEpisodes;
    private TextView animeInfoAge;
    private TextView animeInfoReleaseDate;
    private TextView animeInfoShikimori;
    
    // Next episode overlay
    private View nextEpisodeOverlay;
    private TextView nextEpisodeNumber;
    private TextView nextEpisodeCountdown;
    private com.google.android.material.button.MaterialButton cancelNextEpisodeButton;
    private com.google.android.material.button.MaterialButton playNextEpisodeButton;
    private Handler nextEpisodeHandler;
    private Runnable nextEpisodeRunnable;
    private int countdownSeconds = 7;
    
    // UI components for managers
    private View menuOverlay;
    private ImageButton closeMenuButton;
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private View commentsOptionsButton;
    private TextView emptyCommentsText;
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private ImageButton pipButton;
    private RecyclerView episodesHorizontalRecyclerView;
    private ImageButton commentsButton;
    private ImageButton bookmarkButton;
    private View skipIndicatorLeft;
    private View skipIndicatorRight;

    // Comments manager
    private CommentsManager commentsManager;
    private SettingsBottomSheet currentSettingsBottomSheet;
    private androidx.appcompat.app.AlertDialog currentErrorDialog;

    // Episodes manager
    private EpisodesManager episodesManager;
    
    // Episodes UI components (for EpisodesManager)
    private ImageButton episodesMenuButton;
    
    // Related titles components
    private FrameLayout relatedTitlesOverlay;
    private View relatedTitlesDimOverlay;
    private RecyclerView relatedTitlesRecyclerView;
    private HorizontalRelatedTitlesAdapter relatedTitlesAdapter;
    private RelatedTitlesManager relatedTitlesManager;

    // Players manager
    private PlayersManager playersManager;
    
    // Gestures manager
    private GesturesManager gesturesManager;
    private VerticalGesturesManager verticalGesturesManager;
    
    // Timecode manager
    private TimecodeManager timecodeManager;
    
    // Ambient light manager
    private AmbientLightManager ambientLightManager;
    private com.example.animelib.ui.AmbientLightView ambientLightView;

    // Surround sound manager
    private com.example.animelib.managers.SurroundSoundManager surroundSoundManager;
    private boolean enableSurroundSound = true;
    private int surroundMode = 0;
    private float surroundSpatialWidth = 1.0f;
    private float surroundDialogueBoost = 1.0f;
    private float surroundBassBoost = 1.0f;
    private float surroundTrebleBoost = 1.0f;

    // Picture-in-Picture support
    private static final String ACTION_PIP_PLAY_PAUSE = "com.example.animelib.PIP_PLAY_PAUSE";
    private static final String ACTION_PIP_REWIND = "com.example.animelib.PIP_REWIND";
    private static final String ACTION_PIP_FAST_FORWARD = "com.example.animelib.PIP_FAST_FORWARD";

    private static final int PIP_REQ_PLAY_PAUSE = 101;
    private static final int PIP_REQ_REWIND = 102;
    private static final int PIP_REQ_FAST_FORWARD = 103;

    private BroadcastReceiver pipBroadcastReceiver;
    private boolean isInPictureInPictureMode = false;
    private boolean wasCommentsVisibleBeforePiP = false;
    private boolean wasPlayingBeforeBackground = false;

    // Episode navigation buttons (for EpisodesManager)
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;
    
    // Other UI components
    private View playersControlBar;
    private TextView animeTitleView;
    private TextView currentEpisodeNumberView;
    private TextView currentTeamName;
    private TextView currentEpisodeName;

    // Controller visibility state
    private boolean isControllerVisible = false;
    
    // Fullscreen state
    private boolean isFullscreenMode = false;
    private int currentResizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;

    // Player data is now managed by PlayersManager
    private boolean isOfflineMode = false;
    private final java.util.Map<String, com.example.animelib.data.entity.DownloadedEpisodeEntity> offlineEpisodesMap = new java.util.HashMap<>();
    private KodikResponse currentKodikResponse;
    private String currentAnimeId;
    private long bookmarkTimecode = 0; // Таймкод из закладки в миллисекундах
    private long savedPlayerPosition = 0; // Сохраненная позиция при смене плеера
    private boolean autoBookmarkSaved = false; // Флаг для предотвращения дублирования автосохранения

    // Portrait UI components
    private View portraitVoiceoverPlayerButton;
    private TextView tvPortraitVoiceover;
    private TextView tvPortraitPlayer;
    private ImageButton portraitBookmarkButton;
    private RecyclerView portraitEpisodesRecyclerView;
    private TextView tvPortraitAnimeTitle;
    private TextView tvPortraitEpisodeTitle;
    private View btnPortraitSortComments;
    private TextView tvPortraitSortComments;
    private View btnPortraitCommentRules;
    private EditText portraitCommentInputField;
    private RecyclerView portraitCommentsRecyclerView;
    private TextView tvPortraitEmptyComments;
    private View portraitCommentsLoadingOverlay;
    private androidx.core.widget.NestedScrollView portraitScrollView;

    // User preferences are now managed by PlayersManager
    private String preferredQuality;
    private ImageButton downloadButton;
    private ImageButton downloadButtonTop;
    private ImageButton btnDownloadFromMenu;
    private TextView downloadProgressText;
    private boolean enable4K = false;
    private boolean enableAmbientLight = false;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int currentTheme = ThemeUtils.THEME_SYSTEM;
    private boolean autoPlayOnPrepare = true;

    // Menu state
    // isMenuVisible is now managed by PlayersManager
    private int menuWidth = 300; // dp

    private final int controllerShowTimeoutMs = 4000;
    private boolean shouldAutoHideControls = true; // Контроль автоматического скрытия

    public static void startFromAnimePage(Activity context, String animeUrl) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_ANIME_URL, animeUrl);
        context.startActivity(intent);
    }

    public static void startForOfflineEpisode(Context context, String localFilePath, String animeTitle, String episodeTitle, String animeId, String episodeNumber) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra("EXTRA_LOCAL_FILE_PATH", localFilePath);
        intent.putExtra("EXTRA_ANIME_TITLE", animeTitle);
        intent.putExtra("EXTRA_EPISODE_TITLE", episodeTitle);
        intent.putExtra("EXTRA_ANIME_ID", animeId);
        intent.putExtra("EXTRA_EPISODE_NUMBER", episodeNumber);
        context.startActivity(intent);
    }

    public static void startForOfflineEpisode(Context context, String localFilePath, String animeTitle, String episodeTitle) {
        startForOfflineEpisode(context, localFilePath, animeTitle, episodeTitle, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnimeLIB_VideoPlayer);
        setContentView(R.layout.activity_video_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.playerView);
        playerContainer = findViewById(R.id.playerContainer);
        if (playerContainer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playerContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int l = Math.round(currentOutlineLeft);
                    int t = Math.round(currentOutlineTop);
                    int r = Math.round(currentOutlineRight);
                    int b = Math.round(currentOutlineBottom);
                    if (r > l && b > t && (l > 0 || t > 0 || r < view.getWidth() || b < view.getHeight() || currentCornerRadiusPx > 0)) {
                        if (currentCornerRadiusPx > 0) {
                            outline.setRoundRect(l, t, r, b, currentCornerRadiusPx);
                        } else {
                            outline.setRect(l, t, r, b);
                        }
                    } else {
                        outline.setRect(0, 0, view.getWidth(), view.getHeight());
                    }
                }
            });
            playerContainer.setClipToOutline(true);
        }
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ambientLightView = findViewById(R.id.ambientLightView);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
        }

        updateControllerAutoHide();

        playerView.setControllerAnimationEnabled(false);

        executor = Executors.newSingleThreadExecutor();
        apiService = new ApiService(this);

        loadAndApplyTheme();

        commentsManager = new CommentsManager(this, apiService);

        episodesManager = new EpisodesManager(this, apiService);

        episodesManager.setPlayerControlsCallback(shouldAutoHide -> {
            shouldAutoHideControls = shouldAutoHide;
            updateControllerAutoHide();
        });

        playersManager = new PlayersManager(this, apiService);

        gesturesManager = new GesturesManager(this);
        verticalGesturesManager = new VerticalGesturesManager(this);

        gesturesManager.setVerticalGesturesManager(verticalGesturesManager);
        verticalGesturesManager.setGesturesManager(gesturesManager);

        timecodeManager = new TimecodeManager(this);
        surroundSoundManager = new com.example.animelib.managers.SurroundSoundManager(this);

        try {
            android.webkit.WebView webView = new android.webkit.WebView(this);
            webView.clearCache(true);
            webView.clearHistory();
            webView.destroy();
        } catch (Exception e) {
            Log.w("VideoPlayer", "Failed to clear WebView cache", e);
        }

        executor.execute(() -> {
            enable4K = apiService.load4KSetting();
            enableAmbientLight = apiService.loadAmbientLightSetting();
            enableSurroundSound = apiService.loadSurroundSoundSetting();
            surroundMode = apiService.loadSurround3DMode();
            surroundSpatialWidth = apiService.loadSurroundSpatialWidth();
            surroundDialogueBoost = apiService.loadSurroundDialogueBoost();
            surroundBassBoost = apiService.loadSurroundBassBoost();
            surroundTrebleBoost = apiService.loadSurroundTrebleBoost();
            autoPlay = apiService.loadAutoPlaySetting();
            longSkipDuration = apiService.loadLongSkipDurationSetting();
            currentTheme = apiService.loadThemeSetting();
            Log.d("VideoPlayer", "Loaded settings - 4K: " + enable4K + ", AmbientLight: " + enableAmbientLight + ", SurroundSound: " + enableSurroundSound + " (mode=" + surroundMode + "), AutoPlay: " + autoPlay + ", SkipDuration: " + longSkipDuration + ", Theme: " + currentTheme);

            runOnUiThread(() -> {
                if (playersManager != null) {
                    playersManager.setEnable4K(enable4K);
                }
                if (ambientLightManager != null) {
                    ambientLightManager.setEnabled(enableAmbientLight);
                }
                if (surroundSoundManager != null) {
                    surroundSoundManager.setEnabled(enableSurroundSound);
                    surroundSoundManager.setSpatialMode(surroundMode);
                    surroundSoundManager.setSpatialWidth(surroundSpatialWidth);
                    surroundSoundManager.setDialogueBoost(surroundDialogueBoost);
                    surroundSoundManager.setBassBoostLevel(surroundBassBoost);
                    surroundSoundManager.setTrebleBoostLevel(surroundTrebleBoost);
                }
            });
        });

        // Initialize HTTP data source with custom headers for video requests
        httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(Map.of(
                        "Referer", "https://v3.animelib.org/",
                        "Accept", "video/mp4,video/*,*/*",
                        "Accept-Encoding", "identity;q=1, *;q=0",
                        "Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7",
                        "Origin", "https://v3.animelib.org",
                        "Sec-Fetch-Dest", "video",
                        "Sec-Fetch-Mode", "cors",
                        "Sec-Fetch-Site", "cross-site",
                        "Priority", "i"
                ));

        Log.d("VideoPlayer", "Initialized HTTP data source with custom headers: User-Agent, Referer, Accept, Accept-Encoding, Accept-Language, Origin, Sec-Fetch-*");

        // Get data from intent
        String localFilePath = getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH");
        currentVideoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        animeUrl = getIntent().getStringExtra(EXTRA_ANIME_URL);

        if (localFilePath != null) {
            isOfflineMode = true;
        }

        // Setup fullscreen
        setupFullscreen();

        // Setup menu
        setupMenu();

        setupPipReceiver();

        if (localFilePath != null) {
            String animeTitle = getIntent().getStringExtra("EXTRA_ANIME_TITLE");
            String episodeTitle = getIntent().getStringExtra("EXTRA_EPISODE_TITLE");
            playOfflineFile(localFilePath, animeTitle, episodeTitle);
        } else if (currentVideoUrl != null) {
            // Direct video URL provided - start player immediately
            initializePlayer();
        } else if (animeUrl != null) {
            // Anime page URL provided - fetch video links first
            showLoading("Загрузка видео...");
            loadAnimeFromUrl(animeUrl);
        } else {
            CustomToast.showWarning(this, "Ошибка: Не указан URL видео или страницы аниме");
            finish();
        }
    }

    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            runOnUiThread(runnable);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e("VideoPlayer", "Error in fallback callback", ex);
            }
        }
    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            try {
                // Получаем тему из базы данных
                int themeMode = apiService.loadThemeSetting();
                Log.d("VideoPlayerTheme", "Loaded theme from database: " + themeMode);
                
                // Также проверяем SharedPreferences
                int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
                Log.d("VideoPlayerTheme", "Loaded theme from SharedPreferences: " + sharedPrefTheme);
                
                int finalTheme = (themeMode >= 0 && themeMode <= 2) ? themeMode : sharedPrefTheme;
                
                // Применяем тему в главном потоке
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, finalTheme);
                    setupFullscreen();
                    checkAndUpdateOrientation();
                    Log.d("VideoPlayerTheme", "Theme applied on startup: " + finalTheme);
                });
                
            } catch (Exception e) {
                Log.e("VideoPlayerTheme", "Failed to load and apply theme", e);
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, ThemeUtils.getSavedThemePreference(VideoPlayerActivity.this));
                    setupFullscreen();
                    checkAndUpdateOrientation();
                });
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Entering PiP mode
            if (player != null && !player.isPlaying()) {
                player.play();
                Log.d("VideoPlayer", "Resumed playback in PiP mode");
            }
            wasCommentsVisibleBeforePiP = commentsManager.isCommentsVisible();
            hideAllUI();
            updatePictureInPictureParams();
            Log.d("VideoPlayer", "Entered Picture-in-Picture mode");
        } else {
            // Exiting PiP mode
            showAllUI();
            Log.d("VideoPlayer", "Exited Picture-in-Picture mode");
        }
    }

    private void setupPipReceiver() {
        pipBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null || player == null) return;
                String action = intent.getAction();
                if (ACTION_PIP_PLAY_PAUSE.equals(action)) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                    updatePictureInPictureParams();
                } else if (ACTION_PIP_REWIND.equals(action)) {
                    long currentPos = player.getCurrentPosition();
                    long newPos = Math.max(0, currentPos - 10000);
                    player.seekTo(newPos);
                } else if (ACTION_PIP_FAST_FORWARD.equals(action)) {
                    long currentPos = player.getCurrentPosition();
                    long duration = player.getDuration();
                    long newPos = duration > 0 ? Math.min(duration, currentPos + 10000) : currentPos + 10000;
                    player.seekTo(newPos);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PIP_PLAY_PAUSE);
        filter.addAction(ACTION_PIP_REWIND);
        filter.addAction(ACTION_PIP_FAST_FORWARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pipBroadcastReceiver, filter);
        }
    }

    public List<RemoteAction> buildPipActions() {
        List<RemoteAction> actions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Rewind 10s
            Intent rewindIntent = new Intent(ACTION_PIP_REWIND).setPackage(getPackageName());
            PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_REWIND, rewindIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon rewindIcon = Icon.createWithResource(this, R.drawable.ic_pip_rewind);
            actions.add(new RemoteAction(rewindIcon, "10 сек назад", "Перемотать на 10 секунд назад", rewindPendingIntent));

            // Play / Pause
            boolean isPlaying = player != null && player.isPlaying();
            Intent playPauseIntent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_PLAY_PAUSE, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon playPauseIcon = Icon.createWithResource(
                    this,
                    isPlaying ? R.drawable.ic_pip_pause : R.drawable.ic_pip_play
            );
            String playPauseTitle = isPlaying ? "Пауза" : "Воспроизведение";
            actions.add(new RemoteAction(playPauseIcon, playPauseTitle, playPauseTitle, playPausePendingIntent));

            // Fast Forward 10s
            Intent ffIntent = new Intent(ACTION_PIP_FAST_FORWARD).setPackage(getPackageName());
            PendingIntent ffPendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_FAST_FORWARD, ffIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon ffIcon = Icon.createWithResource(this, R.drawable.ic_pip_fast_forward);
            actions.add(new RemoteAction(ffIcon, "10 сек вперед", "Перемотать на 10 секунд вперед", ffPendingIntent));
        }
        return actions;
    }

    public void updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .setActions(buildPipActions())
                        .build();
                setPictureInPictureParams(params);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to update Picture-in-Picture params", e);
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter PiP mode when user presses home button
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            // Автоматически сохраняем закладку перед переходом в PiP
            autoSaveBookmark();
            enterPictureInPictureMode();
        }
    }

    public void enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .setActions(buildPipActions())
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to enter Picture-in-Picture mode", e);
            }
        }
    }

    /**
     * Переключает ориентацию экрана (портретная / альбомная)
     */
    private void toggleOrientation() {
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void setVideoResizeMode(int resizeMode) {
        this.currentResizeMode = resizeMode;
        if (playerView != null) {
            playerView.setResizeMode(resizeMode);
        }
        if (player != null) {
            if (resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            } else {
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        }
    }

    /**
     * Обновление настроек автоматического скрытия контроллера
     */
    private void updateControllerAutoHide() {
        updateControllerAutoHide(shouldAutoHideControls);
    }
    
    /**
     * Обновление настроек автоматического скрытия контроллера с принудительным значением
     */
    private void updateControllerAutoHide(boolean enableAutoHide) {
        if (playerView != null) {
            if (enableAutoHide) {
                // Включаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs);
                playerView.setControllerAutoShow(true);
                playerView.setControllerHideOnTouch(true);
                Log.d("VideoPlayer", "Controller auto-hide enabled");
            } else {
                // Отключаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(0); // Никогда не скрывать
                playerView.setControllerAutoShow(false);
                playerView.setControllerHideOnTouch(false);
                Log.d("VideoPlayer", "Controller auto-hide disabled");
            }
        }
    }

    /**
     * Открыть панель меню плееров
     */
    public void openMenuPanel() {
        if (isOfflineMode) return;
        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }
        if (playerView != null) {
            playerView.hideController();
        }
        if (menuPanelContainer != null) {
            menuPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель меню плееров
     */
    public void closeMenuPanel() {
        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
    }
    
    /**
     * Открыть панель комментариев
     */
    public void openCommentsPanel() {
        if (isOfflineMode) return;
        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }
        if (playerView != null) {
            playerView.hideController();
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель комментариев
     */
    public void closeCommentsPanel() {
        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.closePanel();
        }
    }
    
    
    /**
     * Проверить открыта ли панель меню
     */
    public boolean isMenuPanelOpen() {
        return menuPanelContainer != null && menuPanelContainer.isOpen();
    }
    
    /**
     * Проверить открыта ли панель комментариев
     */
    public boolean isCommentsPanelOpen() {
        return commentsPanelContainer != null && commentsPanelContainer.isOpen();
    }
    
    
    private void hideAllUI() {
        // Hide all UI elements except the player
        if (playerView != null) {
            playerView.setUseController(false);
        }

        // Hide menu and other panels
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
        // Hide comments panel if it's currently visible
        if (commentsManager.isCommentsVisible()) {
            commentsManager.hideCommentsPanel();
        }
        // Episodes are now managed by EpisodesManager
        if (episodesManager != null) {
            // НЕ скрываем playersControlBar в PiP режиме, только остальные элементы
            episodesManager.hideEpisodesUIForPiP();
        }
        
        // Players are now managed by PlayersManager
        if (playersManager != null) {
            playersManager.hideAllPlayersUI();
        }
        
        // Gestures are now managed by GesturesManager
        if (gesturesManager != null) {
            gesturesManager.hideAllGesturesUI();
        }

        // Hide PiP button in PiP mode
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.GONE);
        }

        // Hide settings dialog if open
        if (currentSettingsBottomSheet != null && currentSettingsBottomSheet.isShowing()) {
            currentSettingsBottomSheet.dismiss();
        }
    }

    private void showAllUI() {
        // Show UI elements back
        if (playerView != null) {
            playerView.setUseController(true);
        }

        // Menu panel is shown only when user requests it
        // (don't auto-open after PiP)

        // Show PiP button back
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.VISIBLE);
        }

        // Restore episodes UI state
        if (episodesManager != null) {
            episodesManager.showAllEpisodesUI();
        }
        
        // Restore players UI state
        if (playersManager != null) {
            playersManager.showAllPlayersUI();
        }
        
        // Restore gestures UI state
        if (gesturesManager != null) {
            gesturesManager.showAllGesturesUI();
        }

        // Restore comments panel state if it was visible before PiP
        if (wasCommentsVisibleBeforePiP && !commentsManager.isCommentsVisible()) {
            commentsManager.showCommentsPanel();
        }
    }

    private void setupFullscreen() {
        Window window = getWindow();
        if (window != null) {
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);

            if (isPortrait) {
                // В вертикальном положении показывай системную шторку (status bar)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                controller.show(WindowInsetsCompat.Type.statusBars());
            } else {
                // В горизонтальном положении убирай системную шторку
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            View decorView = window.getDecorView();
            ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                View content = v.findViewById(android.R.id.content);
                if (content != null) {
                    content.setPadding(0, 0, 0, 0);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
            rootView.setPadding(0, 0, 0, 0);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "RtlHardcoded"})
    private void setupMenu() {
        // === 1. Initialize UI Components ===
        initializeUIComponents();
        
        // === 2. Initialize Managers ===
        initializeManagers();
        
        // === 3. Setup Event Listeners ===
        setupEventListeners();
        
        // === 4. Configure Initial State ===
        configureInitialState();
        checkAndUpdateOrientation();
        
        // === 5. Setup Manager Callbacks ===
        setupManagerCallbacks();
        
        // === 6. Start Initial Loading ===
        startInitialLoading();
    }
    
    /**
     * Инициализация всех UI компонентов
     */
    private void initializeUIComponents() {

        // Main menu components
        controllerView = playerView.findViewById(R.id.exo_controller);
        timeBar = controllerView.findViewById(R.id.exo_progress);
        
        // Set initial alpha for controller (will be animated by our custom logic)
        if (controllerView != null) {
            controllerView.setAlpha(1.0f);
        }

        // Initialize draggable panels
        menuPanelContainer = findViewById(R.id.menuPanelContainer);
        commentsPanelContainer = findViewById(R.id.commentsPanelContainer);
        
        slidingMenuPanel = findViewById(R.id.slidingMenuPanel);
        menuOverlay = findViewById(R.id.menuOverlay);
        menuLoadingIndicator = findViewById(R.id.menuLoadingIndicator);
        menuLoadingOverlay = findViewById(R.id.menuLoadingOverlay);
        
        // Setup menu overlay click to close panels
        if (menuOverlay != null) {
            menuOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            menuOverlay.setOnClickListener(v -> {
                // Закрываем открытые панели при клике на overlay
                if (menuPanelContainer != null && menuPanelContainer.isOpen()) {
                    closeMenuPanel();
                }
                if (commentsPanelContainer != null && commentsPanelContainer.isOpen()) {
                    closeCommentsPanel();
                }
            });
        }
        
        // Initialize anime info placeholder
        animeInfoPlaceholder = findViewById(R.id.animeInfoPlaceholder);
        animeInfoPoster = findViewById(R.id.animeInfoPoster);
        animeInfoTitle = findViewById(R.id.animeInfoTitle);
        animeInfoOriginalTitle = findViewById(R.id.animeInfoOriginalTitle);
        animeInfoYear = findViewById(R.id.animeInfoYear);
        animeInfoType = findViewById(R.id.animeInfoType);
        animeInfoStatus = findViewById(R.id.animeInfoStatus);
        animeInfoRating = findViewById(R.id.animeInfoRating);
        animeInfoEpisodes = findViewById(R.id.animeInfoEpisodes);
        animeInfoAge = findViewById(R.id.animeInfoAge);
        animeInfoReleaseDate = findViewById(R.id.animeInfoReleaseDate);
        animeInfoShikimori = findViewById(R.id.animeInfoShikimori);
        
        // Next episode overlay
        nextEpisodeOverlay = findViewById(R.id.nextEpisodeOverlay);
        nextEpisodeNumber = findViewById(R.id.nextEpisodeNumber);
        nextEpisodeCountdown = findViewById(R.id.nextEpisodeCountdown);
        cancelNextEpisodeButton = findViewById(R.id.cancelNextEpisodeButton);
        playNextEpisodeButton = findViewById(R.id.playNextEpisodeButton);
        nextEpisodeHandler = new Handler(Looper.getMainLooper());
        
        // Setup next episode overlay buttons
        if (cancelNextEpisodeButton != null) {
            cancelNextEpisodeButton.setOnClickListener(v -> cancelNextEpisode());
        }
        if (playNextEpisodeButton != null) {
            playNextEpisodeButton.setOnClickListener(v -> playNextEpisodeNow());
        }
        
        // Gesture components
        TextView seekPreviewText = findViewById(R.id.seekPreviewText);
        TextView holdSpeedToast = findViewById(R.id.holdSpeedToast);
        View skipIndicatorLeft = findViewById(R.id.skipIndicatorLeft);
        View skipIndicatorRight = findViewById(R.id.skipIndicatorRight);
        
        // Comments components
        View commentsPanel = findViewById(R.id.commentsPanel);
        ImageButton closeCommentsButton = findViewById(R.id.closeCommentsButton);
        RecyclerView commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        View commentsLoadingOverlay = findViewById(R.id.commentsLoadingOverlay);
        View commentsOptionsButton = findViewById(R.id.btnSortComments);
        TextView emptyCommentsText = findViewById(R.id.emptyCommentsText);
        
        // Related titles components
        relatedTitlesOverlay = findViewById(R.id.relatedTitlesOverlay);
        relatedTitlesDimOverlay = findViewById(R.id.relatedTitlesDimOverlay);
        relatedTitlesRecyclerView = findViewById(R.id.relatedTitlesRecyclerView);
        
        // Player control components
        if (controllerView != null) {
            initializeControllerComponents();
        }
        
        // Menu control components
        ImageButton closeMenuButton = findViewById(R.id.closeMenuButton);
        ImageButton pipButton = findViewById(R.id.pipButton);
        
        // Store components for manager initialization
        this.menuOverlay = menuOverlay;
        this.closeMenuButton = closeMenuButton;
        this.commentsPanel = commentsPanel;
        this.closeCommentsButton = closeCommentsButton;
        this.commentsRecyclerView = commentsRecyclerView;
        this.commentsLoadingOverlay = commentsLoadingOverlay;
        this.commentsOptionsButton = commentsOptionsButton;
        this.emptyCommentsText = emptyCommentsText;
        this.seekPreviewText = seekPreviewText;
        this.holdSpeedToast = holdSpeedToast;
        this.pipButton = pipButton;
        this.skipIndicatorLeft = skipIndicatorLeft;
        this.skipIndicatorRight = skipIndicatorRight;
    }
    
    /**
     * Инициализация компонентов контроллера плеера
     */
    private void initializeControllerComponents() {
        // Player info components
        ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
        settingsButton = controllerView.findViewById(R.id.settingsButton);
        settingsQualityTag = controllerView.findViewById(R.id.settingsQualityTag);
        updateSettingsQualityTag();
        menuToggleFullscreen = controllerView.findViewById(R.id.menuToggleFullscreen);
        if (pipButton == null && controllerView != null) {
            pipButton = controllerView.findViewById(R.id.pipButton);
        }
        animeTitleView = controllerView.findViewById(R.id.animeTitle);
        currentTeamName = controllerView.findViewById(R.id.currentTeamName);
        currentEpisodeName = controllerView.findViewById(R.id.currentEpisodeName);
        currentEpisodeNumberView = controllerView.findViewById(R.id.currentEpisodeNumber);
        
        // Navigation components
        episodesMenuButton = controllerView.findViewById(R.id.episodesMenuButton);
        prevEpisodeButton = controllerView.findViewById(R.id.prevEpisodeButton);
        nextEpisodeButton = controllerView.findViewById(R.id.nextEpisodeButton);
        
        // Control components
        menuToggleButton = controllerView.findViewById(R.id.menuToggleButton);
        playersControlBar = controllerView.findViewById(R.id.playersControlBar);
        
        // Episode list component
        RecyclerView episodesHorizontalRecyclerView = controllerView.findViewById(R.id.episodesHorizontalRecyclerView);
        ImageButton commentsButton = controllerView.findViewById(R.id.commentsButton);
        bookmarkButton = controllerView.findViewById(R.id.bookmarkButton);
        downloadButton = null;
        downloadButtonTop = null;
        btnDownloadFromMenu = findViewById(R.id.btnDownloadFromMenu);
        downloadProgressText = findViewById(R.id.downloadProgressText);
        setupDownloadListener();

        // Store for manager initialization
        this.episodesHorizontalRecyclerView = episodesHorizontalRecyclerView;
        this.commentsButton = commentsButton;
    }
    
    /**
     * Настройка драггабельных панелей
     */
    private void setupDraggablePanels() {
        if (menuPanelContainer != null) {
            menuPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelDragStart() {
                    if (ambientLightManager != null) {
                        ambientLightManager.suspend();
                    }
                }

                @Override
                public void onPanelOpened() {
                    applyPlayerSidePanelTransform(1f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                    }
                    if (ambientLightManager != null) {
                        ambientLightManager.resume();
                    }
                }

                @Override
                public void onPanelClosed() {
                    applyPlayerSidePanelTransform(0f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    if (playersManager != null) {
                        playersManager.onPanelClosedByDrag();
                    }
                    if (ambientLightManager != null) {
                        ambientLightManager.resume();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    applyPlayerSidePanelTransform(1f - slideOffset);
                    if (menuOverlay != null) {
                        if (slideOffset < 1f && menuOverlay.getVisibility() != View.VISIBLE) {
                            menuOverlay.setVisibility(View.VISIBLE);
                        } else if (slideOffset >= 1f && menuOverlay.getVisibility() != View.GONE) {
                            menuOverlay.setVisibility(View.GONE);
                        }
                    }
                }
                
                @Override
                public boolean canClosePanel() {
                    if (isOfflineMode) {
                        return true;
                    }
                    if (playersManager != null && playersManager.getCurrentPlayerData() == null) {
                        return false;
                    }
                    return true;
                }
            });
        }
        
        if (commentsPanelContainer != null) {
            commentsPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelDragStart() {
                    if (ambientLightManager != null) {
                        ambientLightManager.suspend();
                    }
                }

                @Override
                public void onPanelOpened() {
                    applyPlayerSidePanelTransform(1f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                    }
                    if (ambientLightManager != null) {
                        ambientLightManager.resume();
                    }
                }

                @Override
                public void onPanelClosed() {
                    applyPlayerSidePanelTransform(0f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    if (commentsManager != null) {
                        commentsManager.onPanelClosedByDrag();
                    }
                    if (ambientLightManager != null) {
                        ambientLightManager.resume();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    applyPlayerSidePanelTransform(1f - slideOffset);
                    if (menuOverlay != null) {
                        if (slideOffset < 1f && menuOverlay.getVisibility() != View.VISIBLE) {
                            menuOverlay.setVisibility(View.VISIBLE);
                        } else if (slideOffset >= 1f && menuOverlay.getVisibility() != View.GONE) {
                            menuOverlay.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    /**
     * Применяет трансформацию плеера (уменьшение, позиционирование, скругление углов)
     * и синхронизирует положение ambient-подсветки при открытии боковых панелей (озвучки/комментарии).
     * @param openProgress Прогресс открытия панели от 0.0 (закрыто, полный экран) до 1.0 (полностью открыто)
     */
    private void applyPlayerSidePanelTransform(float openProgress) {
        openProgress = Math.max(0f, Math.min(1f, openProgress));

        if (playerContainer == null) {
            return;
        }

        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        ViewCompat.setElevation(playerContainer, isPortrait ? 10f : 0f);

        // Получаем реальные размеры экрана
        int screenWidth = 0;
        int screenHeight = 0;
        View decorView = getWindow().getDecorView();
        if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
            screenWidth = decorView.getWidth();
            screenHeight = decorView.getHeight();
        } else {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }

        if (screenWidth <= 0 || screenHeight <= 0) return;

        // Важно: playerContainer всегда должен быть MATCH_PARENT, чтобы элементы управления
        // растягивались на весь экран, и жесты вытягивания с края экрана улавливались на playerView
        android.view.ViewGroup.LayoutParams rawLp = playerContainer.getLayoutParams();
        if (rawLp instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) rawLp;
            if (mlp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                mlp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                mlp.leftMargin != 0 || mlp.topMargin != 0) {
                mlp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                mlp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                mlp.leftMargin = 0;
                mlp.topMargin = 0;
                if (mlp instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) mlp).gravity = Gravity.TOP | Gravity.START;
                } else if (mlp instanceof LinearLayout.LayoutParams) {
                    ((LinearLayout.LayoutParams) mlp).gravity = Gravity.TOP | Gravity.START;
                }
                playerContainer.setLayoutParams(mlp);
            }
        } else if (rawLp != null) {
            if (rawLp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                rawLp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT) {
                rawLp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                rawLp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                playerContainer.setLayoutParams(rawLp);
            }
        }

        // Определяем соотношение сторон видео (по умолчанию 16:9)
        float videoAspect = 16f / 9f;
        if (player != null && player.getVideoSize() != null) {
            int vWidth = player.getVideoSize().width;
            int vHeight = player.getVideoSize().height;
            if (vWidth > 0 && vHeight > 0) {
                videoAspect = (float) vWidth / (float) vHeight;
            }
        }

        // Размеры видео в полноэкранном режиме (progress = 0)
        float screenAspect = (float) screenWidth / (float) screenHeight;
        float videoW0, videoH0;
        if (screenAspect > videoAspect) {
            videoH0 = screenHeight;
            videoW0 = screenHeight * videoAspect;
        } else {
            videoW0 = screenWidth;
            videoH0 = screenWidth / videoAspect;
        }
        float videoLeft0 = (screenWidth - videoW0) / 2f;
        float videoTop0 = (screenHeight - videoH0) / 2f;

        // Определяем ширину открытой боковой панели в пикселях (по умолчанию 360dp)
        float density = getResources().getDisplayMetrics().density;
        float panelWidthPx = 360f * density;

        if (menuPanelContainer != null && menuPanelContainer.getChildCount() > 0) {
            int w = menuPanelContainer.getChildAt(0).getWidth();
            if (w > 0) panelWidthPx = w;
        } else if (commentsPanelContainer != null && commentsPanelContainer.getChildCount() > 0) {
            int w = commentsPanelContainer.getChildAt(0).getWidth();
            if (w > 0) panelWidthPx = w;
        }

        // Отступ слева от экрана и от панели для видео в открытом состоянии (16dp)
        float sideMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, getResources().getDisplayMetrics());

        // Доступная область для видео при открытой панели (progress = 1)
        float availWidth1 = Math.max(0f, screenWidth - panelWidthPx - (2f * sideMarginPx));
        float availHeight1 = Math.max(0f, screenHeight - (2f * sideMarginPx));
        float availAspect1 = (availHeight1 > 0) ? (availWidth1 / availHeight1) : videoAspect;
        float videoW1, videoH1;
        if (availAspect1 > videoAspect) {
            videoH1 = availHeight1;
            videoW1 = availHeight1 * videoAspect;
        } else {
            videoW1 = availWidth1;
            videoH1 = availWidth1 / videoAspect;
        }
        float videoLeft1 = sideMarginPx + (availWidth1 - videoW1) / 2f;
        float videoTop1 = sideMarginPx + (availHeight1 - videoH1) / 2f;

        // Линейная интерполяция размеров и положения видео на экране
        float currentVideoW = videoW0 + (videoW1 - videoW0) * openProgress;
        float currentVideoH = videoH0 + (videoH1 - videoH0) * openProgress;
        float currentLeft = videoLeft0 + (videoLeft1 - videoLeft0) * openProgress;
        float currentTop = videoTop0 + (videoTop1 - videoTop0) * openProgress;

        // Масштабирование playerContainer от 1.0
        float scale = (videoW0 > 0) ? (currentVideoW / videoW0) : 1f;

        // Положение верхнего левого угла видео на экране
        float translationX = currentLeft - (videoLeft0 * scale);
        float translationY = currentTop - (videoTop0 * scale);

        playerContainer.setPivotX(0f);
        playerContainer.setPivotY(0f);
        playerContainer.setScaleX(scale);
        playerContainer.setScaleY(scale);
        playerContainer.setTranslationX(translationX);
        playerContainer.setTranslationY(translationY);

        // Границы отсечения внутри playerContainer в его локальных координатах
        currentOutlineLeft = videoLeft0 * openProgress;
        currentOutlineTop = videoTop0 * openProgress;
        currentOutlineRight = screenWidth + (videoLeft0 + videoW0 - screenWidth) * openProgress;
        currentOutlineBottom = screenHeight + (videoTop0 + videoH0 - screenHeight) * openProgress;

        // Скругление углов контейнера: визуально 20dp в открытом состоянии
        float maxRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, getResources().getDisplayMetrics());
        currentCornerRadiusPx = (scale > 0) ? (maxRadiusPx * openProgress / scale) : 0f;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playerContainer.invalidateOutline();
        }

        // Обновляем подсветку (ambient light), оставляя сам AmbientLightView полноэкранным без обрезки
        if (ambientLightView != null) {
            ambientLightView.setScaleX(1f);
            ambientLightView.setScaleY(1f);
            ambientLightView.setTranslationX(0f);
            ambientLightView.setTranslationY(0f);
            if (openProgress > 0f) {
                ambientLightView.setCustomVideoBounds(currentLeft, currentTop, currentLeft + currentVideoW, currentTop + currentVideoH);
            } else {
                ambientLightView.clearCustomVideoBounds();
            }
        }
    }
    
    /**
     * Инициализация всех менеджеров
     */
    private void initializeManagers() {
        // Initialize gestures manager
        gesturesManager.initializeViews(playerView, null, holdSpeedToast, seekPreviewText,
                skipIndicatorLeft, skipIndicatorRight);
        
        // Setup draggable panels
        setupDraggablePanels();
        
        // Initialize comments manager
        commentsManager.initializeViews(commentsPanel, closeCommentsButton, commentsRecyclerView,
                commentsLoadingOverlay, commentsButton, commentsOptionsButton, menuOverlay, emptyCommentsText);
        
        // Initialize players manager
        playersManager.initializeViews(slidingMenuPanel, closeMenuButton, null, null,
                menuOverlay, menuLoadingOverlay, menuLoadingIndicator);
        
        // Initialize episodes manager
        episodesManager.initializeViews(null, episodesMenuButton, episodesHorizontalRecyclerView,
                null, prevEpisodeButton, nextEpisodeButton, menuOverlay, playersControlBar);
        
        // Configure menu width
        float density = getResources().getDisplayMetrics().density;
        menuWidth = (int) (menuWidth * density);
        playersManager.setMenuWidth(menuWidth);
        
        // Initialize ambient light manager
        ambientLightManager = new AmbientLightManager(this, playerView, ambientLightView);
        
        // Initialize related titles
        initializeRelatedTitles();

        // Initialize portrait views
        initializePortraitViews();
    }

    private void initializePortraitViews() {
        portraitScrollView = findViewById(R.id.portraitScrollView);
        portraitVoiceoverPlayerButton = findViewById(R.id.portraitVoiceoverPlayerButton);
        tvPortraitVoiceover = findViewById(R.id.tvPortraitVoiceover);
        tvPortraitPlayer = findViewById(R.id.tvPortraitPlayer);
        portraitBookmarkButton = findViewById(R.id.portraitBookmarkButton);
        portraitEpisodesRecyclerView = findViewById(R.id.portraitEpisodesRecyclerView);
        tvPortraitAnimeTitle = findViewById(R.id.tvPortraitAnimeTitle);
        tvPortraitEpisodeTitle = findViewById(R.id.tvPortraitEpisodeTitle);
        btnPortraitSortComments = findViewById(R.id.btnPortraitSortComments);
        tvPortraitSortComments = findViewById(R.id.tvPortraitSortComments);
        btnPortraitCommentRules = findViewById(R.id.btnPortraitCommentRules);
        portraitCommentInputField = findViewById(R.id.portraitCommentInputField);
        portraitCommentsRecyclerView = findViewById(R.id.portraitCommentsRecyclerView);
        tvPortraitEmptyComments = findViewById(R.id.tvPortraitEmptyComments);
        portraitCommentsLoadingOverlay = findViewById(R.id.portraitCommentsLoadingOverlay);

        showPlayerTextSkeletons();

        if (portraitVoiceoverPlayerButton != null) {
            portraitVoiceoverPlayerButton.setOnClickListener(v -> {
                if (playersManager != null) {
                    playersManager.toggleMenu();
                }
            });
        }

        if (portraitBookmarkButton != null) {
            portraitBookmarkButton.setOnClickListener(v -> addBookmark());
        }

        if (episodesManager != null && portraitEpisodesRecyclerView != null) {
            episodesManager.setPortraitEpisodesRecyclerView(portraitEpisodesRecyclerView);
        }

        if (commentsManager != null && portraitCommentsRecyclerView != null) {
            commentsManager.setPortraitViews(portraitCommentsRecyclerView, portraitCommentsLoadingOverlay,
                    tvPortraitEmptyComments, btnPortraitSortComments, tvPortraitSortComments, btnPortraitCommentRules);
        }

        if (portraitScrollView != null) {
            portraitScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 200)) {
                    if (commentsManager != null && getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                        commentsManager.loadNextCommentsPageIfAvailable();
                    }
                }
            });
        }
    }

    /**
     * Показывает скелетоны загрузки для всех текстовых элементов плеера
     */
    private void showPlayerTextSkeletons() {
        safeRunOnUiThread(() -> {
            if (animeTitleView != null) SkeletonHelper.showSkeleton(animeTitleView, 160);
            if (currentTeamName != null) SkeletonHelper.showSkeleton(currentTeamName, 80);
            if (currentEpisodeNumberView != null) SkeletonHelper.showSkeleton(currentEpisodeNumberView, 65);
            if (currentEpisodeName != null) SkeletonHelper.showSkeleton(currentEpisodeName, 100);

            if (tvPortraitAnimeTitle != null) SkeletonHelper.showSkeleton(tvPortraitAnimeTitle, 180);
            if (tvPortraitEpisodeTitle != null) SkeletonHelper.showSkeleton(tvPortraitEpisodeTitle, 120);
            if (tvPortraitVoiceover != null) SkeletonHelper.showSkeleton(tvPortraitVoiceover, 90);
            if (tvPortraitPlayer != null) SkeletonHelper.showSkeleton(tvPortraitPlayer, 60);

            if (animeInfoTitle != null) SkeletonHelper.showSkeleton(animeInfoTitle, 200);
            if (animeInfoOriginalTitle != null) SkeletonHelper.showSkeleton(animeInfoOriginalTitle, 140);
            if (animeInfoRating != null) SkeletonHelper.showSkeleton(animeInfoRating, 40);
            if (animeInfoEpisodes != null) SkeletonHelper.showSkeleton(animeInfoEpisodes, 80);
            if (animeInfoYear != null) SkeletonHelper.showSkeleton(animeInfoYear, 50);
            if (animeInfoAge != null) SkeletonHelper.showSkeleton(animeInfoAge, 50);
            if (animeInfoType != null) SkeletonHelper.showSkeleton(animeInfoType, 60);
            if (animeInfoStatus != null) SkeletonHelper.showSkeleton(animeInfoStatus, 70);
            if (animeInfoReleaseDate != null) SkeletonHelper.showSkeleton(animeInfoReleaseDate, 100);
            if (animeInfoShikimori != null) SkeletonHelper.showSkeleton(animeInfoShikimori, 40);
        });
    }

    private void updatePortraitVoiceoverPlayerUI() {
        safeRunOnUiThread(() -> {
            if (tvPortraitVoiceover == null || tvPortraitPlayer == null || playersManager == null) return;
            EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
            if (cur != null) {
                String teamName = (cur.getTeam() != null && cur.getTeam().getName() != null && !cur.getTeam().getName().isEmpty())
                        ? cur.getTeam().getName()
                        : ((cur.getTranslationType() != null && cur.getTranslationType().getLabel() != null)
                                ? cur.getTranslationType().getLabel() : "Озвучка");
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, teamName);

                String playerName = cur.getPlayer();
                if (playerName != null && !playerName.isEmpty()) {
                    playerName = playerName.substring(0, 1).toUpperCase() + playerName.substring(1);
                } else {
                    playerName = "Плеер";
                }
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, playerName);
            } else {
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, "Выбрать озвучку");
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, "");
            }
        });
    }

    private void updatePortraitHeaderTitlesUI() {
        safeRunOnUiThread(() -> {
            if (tvPortraitAnimeTitle != null) {
                String animeTitleStr = animeTitleView != null ? animeTitleView.getText().toString() : "";
                if (animeTitleView != null && Boolean.TRUE.equals(animeTitleView.getTag(R.id.tag_skeleton_active))) {
                    SkeletonHelper.showSkeleton(tvPortraitAnimeTitle, 180);
                } else if (!animeTitleStr.isEmpty()) {
                    SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, animeTitleStr);
                }
            }
            if (tvPortraitEpisodeTitle != null) {
                EpisodesListResponse.EpisodeItem curEp = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
                if (curEp != null) {
                    String num = curEp.getNumber() != null ? curEp.getNumber() : "";
                    String name = curEp.getName() != null && !curEp.getName().isEmpty() ? curEp.getName() : "";
                    String epTitle = (!num.isEmpty() ? (num + " серия") : "") + (!name.isEmpty() ? (", " + name) : "");
                    SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, epTitle);
                } else {
                    SkeletonHelper.showSkeleton(tvPortraitEpisodeTitle, 120);
                }
            }
        });
    }
    
    /**
     * Инициализация связанных тайтлов
     */
    private void initializeRelatedTitles() {
        if (relatedTitlesRecyclerView == null) {
            Log.w("VideoPlayer", "Related titles RecyclerView is null");
            return;
        }
        
        // Setup RecyclerView
        relatedTitlesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, 
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        
        // Initialize adapter
        relatedTitlesAdapter = new HorizontalRelatedTitlesAdapter(
                new java.util.ArrayList<>(), 
                this::onRelatedTitleSelected
        );
        
        // Initialize RelatedTitlesManager
        relatedTitlesManager = new RelatedTitlesManager();
        TextView relatedTitlesHeader = findViewById(R.id.relatedTitlesHeader);
        LinearLayout relatedAnimeInfoContainer = findViewById(R.id.relatedAnimeInfoContainer);
        ImageView relatedAnimeCover = findViewById(R.id.relatedAnimeCover);
        TextView relatedAnimeTitle = findViewById(R.id.relatedAnimeTitle);
        TextView relatedAnimeEngTitle = findViewById(R.id.relatedAnimeEngTitle);
        com.google.android.material.chip.Chip relatedAnimeTypeChip = findViewById(R.id.relatedAnimeTypeChip);
        com.google.android.material.chip.Chip relatedAnimeStatusChip = findViewById(R.id.relatedAnimeStatusChip);
        com.google.android.material.chip.Chip relatedAnimeYearChip = findViewById(R.id.relatedAnimeYearChip);
        com.google.android.material.chip.Chip relatedAnimeAgeChip = findViewById(R.id.relatedAnimeAgeChip);
        TextView relatedAnimeRating = findViewById(R.id.relatedAnimeRating);
        TextView relatedAnimeVotes = findViewById(R.id.relatedAnimeVotes);
        TextView relatedAnimeEpisodes = findViewById(R.id.relatedAnimeEpisodes);
        relatedTitlesManager.initialize(relatedTitlesOverlay, relatedTitlesDimOverlay, relatedTitlesRecyclerView,
                                        relatedTitlesHeader, relatedAnimeInfoContainer, relatedAnimeCover,
                                        relatedAnimeTitle, relatedAnimeEngTitle, relatedAnimeTypeChip, 
                                        relatedAnimeStatusChip, relatedAnimeYearChip, relatedAnimeAgeChip,
                                        relatedAnimeRating, relatedAnimeVotes, relatedAnimeEpisodes);
        relatedTitlesManager.setAdapter(relatedTitlesAdapter);
        
        // Устанавливаем listener для управления интерфейсом плеера
        relatedTitlesManager.setPlayerInterfaceControlListener(new RelatedTitlesManager.OnPlayerInterfaceControlListener() {
            @Override
            public void onHidePlayerInterface() {
                // НЕ скрываем интерфейс полностью, только меняем alpha через onPlayerInterfaceAlpha
            }
            
            @Override
            public void onShowPlayerInterface() {
                // Показываем интерфейс плеера
                if (playerView != null) {
                    playerView.showController();
                }
                if (ambientLightManager != null) {
                    ambientLightManager.resume();
                }
            }
            
            @Override
            public void onPlayerInterfaceAlpha(float alpha) {
                // Плавно меняем прозрачность интерфейса плеера
                View controller = findViewById(R.id.exo_controller);
                if (controller != null) {
                    controller.setAlpha(alpha);
                }
            }
        });
        
        // Настраиваем drag для закрытия панели связанных тайтлов
        setupRelatedTitlesDragToClose();
        
        // Load related titles if we have anime info
        if (currentAnimeId != null) {
            loadRelatedTitles();
        }
    }
    
    /**
     * Настраивает drag-to-close для панели связанных тайтлов (BottomSheet style)
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupRelatedTitlesDragToClose() {
        if (relatedTitlesOverlay == null) return;
        
        final float[] initialY = {0f};
        final float[] lastY = {0f};
        final boolean[] isDragging = {false};
        final int touchSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        
        relatedTitlesOverlay.setOnTouchListener((v, event) -> {
            // Обрабатываем только если панель открыта
            if (relatedTitlesManager == null || !relatedTitlesManager.isRelatedTitlesVisible()) {
                return false;
            }
            
            float currentY = event.getRawY();
            
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    initialY[0] = currentY;
                    lastY[0] = currentY;
                    isDragging[0] = false;
                    // Останавливаем текущие анимации
                    relatedTitlesOverlay.animate().cancel();
                    return true;
                    
                case android.view.MotionEvent.ACTION_MOVE:
                    float deltaY = currentY - initialY[0];
                    float moveDelta = currentY - lastY[0];
                    
                    // Начинаем drag если прошли touchSlop
                    if (!isDragging[0] && Math.abs(deltaY) > touchSlop) {
                        isDragging[0] = true;
                        // Запрещаем родителю перехватывать события
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    
                    if (isDragging[0]) {
                        // ПАНЕЛЬ ОТКРЫВАЕТСЯ СВЕРХУ ВНИЗ (translationY: -height -> 0)
                        // Закрываем её тягой ВВЕРХ (deltaY < 0)
                        
                        if (deltaY < 0) {
                            // Тянем ВВЕРХ (закрытие) - translationY становится отрицательным
                            relatedTitlesOverlay.setTranslationY(deltaY);
                        } else {
                            // Тянем ВНИЗ - не даём тянуть дальше (панель уже открыта)
                            relatedTitlesOverlay.setTranslationY(0);
                        }
                        
                        // Вычисляем прогресс (1.0 = открыто на месте, 0.0 = закрыто наверху)
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                        // currentTranslation: 0 (открыто) -> -screenHeight (закрыто)
                        float progress = 1.0f + (currentTranslation / screenHeight);
                        progress = Math.max(0f, Math.min(1f, progress));
                        
                        // Обновляем затемнение
                        if (relatedTitlesDimOverlay != null) {
                            relatedTitlesDimOverlay.setAlpha(progress);
                        }
                        
                        // Обновляем прозрачность интерфейса плеера
                        View controller = findViewById(R.id.exo_controller);
                        if (controller != null) {
                            controller.setAlpha(1f - progress);
                        }
                    }
                    
                    lastY[0] = currentY;
                    return true;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (isDragging[0]) {
                        float finalDeltaY = currentY - initialY[0];
                        float velocity = lastY[0] - currentY; // Скорость вверх = положительная
                        
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float dismissThreshold = screenHeight * 0.15f; // Уменьшен порог с 0.3 до 0.15
                        
                        // ПАНЕЛЬ ЗАКРЫВАЕТСЯ ТЯГОЙ ВВЕРХ (finalDeltaY < 0)
                        // Решаем закрывать или нет на основе расстояния и скорости
                        boolean shouldDismiss = (finalDeltaY < -dismissThreshold) || (velocity > 50 && finalDeltaY < 0);
                        
                        if (shouldDismiss) {
                            // Закрываем панель
                            relatedTitlesManager.hideRelatedTitles();
                            if (ambientLightManager != null) {
                                ambientLightManager.resume();
                            }
                        } else {
                            // Возвращаем на место
                            relatedTitlesOverlay.animate()
                                    .translationY(0f)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .setUpdateListener(animation -> {
                                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                                        float progress = 1.0f + (currentTranslation / screenHeight);
                                        
                                        if (relatedTitlesDimOverlay != null) {
                                            relatedTitlesDimOverlay.setAlpha(progress);
                                        }
                                        
                                        View controller = findViewById(R.id.exo_controller);
                                        if (controller != null) {
                                            controller.setAlpha(1f - progress);
                                        }
                                    })
                                    .start();
                        }
                        
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    isDragging[0] = false;
                    return true;
            }
            
            return false;
        });
    }
    
    /**
     * Загрузка связанных тайтлов
     */
    private void loadRelatedTitles() {
        if (currentAnimeId == null) {
            Log.w("VideoPlayer", "No anime ID available for loading related titles");
            return;
        }
        
        // Extract anime slug from current anime ID or URL
        String animeSlug = currentAnimeId;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            animeSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (animeSlug == null || animeSlug.isEmpty()) {
            Log.w("VideoPlayer", "No anime slug available for loading related titles");
            return;
        }
        
        Log.d("VideoPlayer", "Loading related titles for anime: " + animeSlug);
        
        apiService.getRelatedTitles(animeSlug, new ApiService.RelatedTitlesCallback() {
            @Override
            public void onRelatedTitlesReceived(RelatedTitlesResponse response) {
                runOnUiThread(() -> {
                    if (response.getData() != null && !response.getData().isEmpty()) {
                        Log.d("VideoPlayer", "Related titles loaded: " + response.getData().size());
                        showRelatedTitles(response.getData());
                    } else {
                        Log.d("VideoPlayer", "No related titles found");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("VideoPlayer", "Error loading related titles: " + error);
            }
        });
    }
    
    /**
     * Показать связанные тайтлы
     */
    private void showRelatedTitles(List<RelatedTitlesResponse.RelatedTitle> relatedTitles) {
        if (relatedTitlesManager != null) {
            relatedTitlesManager.updateRelatedTitles(relatedTitles);
        }
    }
    
    /**
     * Обработчик выбора связанного тайтла
     */
    /**
     * Устанавливает информацию об аниме в панель связанных тайтлов
     */
    private void setAnimeInfoToRelatedPanel(AnimeInfoResponse.Data animeData) {
        if (relatedTitlesManager == null) {
            Log.w("VideoPlayer", "relatedTitlesManager is null");
            return;
        }

        // Получаем обложку
        String coverUrl = null;
        if (animeData.getCover() != null && animeData.getCover().getDefaultUrl() != null) {
            coverUrl = animeData.getCover().getDefaultUrl();
            currentPosterUrl = coverUrl;
        }

        // Получаем название (приоритет: русское -> английское -> оригинальное)
        String title = animeData.getRus_name();
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getEng_name();
        }
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getName();
        }
        if (title == null) {
            title = "Без названия";
        }

        // Английское название (если русское название используется)
        String engTitle = null;
        if (animeData.getRus_name() != null && !animeData.getRus_name().trim().isEmpty()) {
            engTitle = animeData.getEng_name();
        }

        // Тип
        String type = null;
        if (animeData.getType() != null && animeData.getType().getLabel() != null) {
            type = animeData.getType().getLabel();
        }

        // Статус
        String status = null;
        if (animeData.getStatus() != null && animeData.getStatus().getLabel() != null) {
            status = animeData.getStatus().getLabel();
        }

        // Год выхода
        String year = null;
        if (animeData.getReleaseDateString() != null && !animeData.getReleaseDateString().isEmpty()) {
            // Извлекаем год из даты (например "2024" из "15.10.2024" или просто "2024")
            try {
                String dateStr = animeData.getReleaseDateString();
                if (dateStr.contains(".")) {
                    // Формат "DD.MM.YYYY"
                    String[] parts = dateStr.split("\\.");
                    if (parts.length >= 3) {
                        year = parts[2];
                    }
                } else if (dateStr.matches("\\d{4}")) {
                    // Формат "YYYY"
                    year = dateStr;
                }
            } catch (Exception e) {
                Log.w("VideoPlayer", "Failed to parse year from: " + animeData.getReleaseDateString());
            }
        }

        // Возрастной рейтинг
        String ageRating = null;
        if (animeData.getAgeRestriction() != null && animeData.getAgeRestriction().getLabel() != null) {
            ageRating = animeData.getAgeRestriction().getLabel();
        }

        // Рейтинг
        String rating = "";
        String votes = "";
        if (animeData.getRating() != null) {
            if (animeData.getRating().getAverageFormated() != null) {
                rating = animeData.getRating().getAverageFormated();
            }
            if (animeData.getRating().getVotesFormated() != null) {
                votes = "(" + animeData.getRating().getVotesFormated() + ")";
            }
        }

        // Количество эпизодов
        String episodes = null;
        if (animeData.getItems_count() != null) {
            episodes = "Эпизоды: " + animeData.getItems_count().getUploaded() +
                      " / " + animeData.getItems_count().getTotal();
        }

        Log.d("VideoPlayer", "Setting anime info to related panel: title=" + title + 
              ", type=" + type + ", status=" + status + ", year=" + year);
        
        relatedTitlesManager.setAnimeInfo(coverUrl, title, engTitle, type, status, year, 
                                         ageRating, rating, votes, episodes);
    }
    
    /**
     * Обработчик выбора связанного тайтла
     * @param slugUrl URL выбранного аниме (только для model="anime")
     */
    private void onRelatedTitleSelected(String slugUrl) {
        if (slugUrl == null || slugUrl.trim().isEmpty()) {
            Log.w("VideoPlayer", "Related title slug URL is null or empty");
            return;
        }
        
        Log.d("VideoPlayer", "Related title selected with slug URL: " + slugUrl);
        
        // Hide related titles
        if (relatedTitlesManager != null) {
            relatedTitlesManager.hideRelatedTitles();
        }
        
        // Stop current player
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        
        // Reset state
        currentVideoUrl = null;
        currentAnimeId = null;
        
        // Show loading
        showLoading("Загрузка аниме...");
        
        // Load new anime
        animeUrl = slugUrl;
        loadAnimeFromUrl(slugUrl);
        
        Log.d("VideoPlayer", "Loading new anime from slug URL: " + slugUrl);
    }

    /**
     * Настройка всех event listeners
     */
    private void setupEventListeners() {
        // Setup controller visibility listener
        setupControllerVisibilityListener();
        
        // Setup button click listeners
        setupButtonClickListeners();
        
        // Setup overlay touch listener
        setupOverlayTouchListener();
        
        // Setup player listener
        setupPlayerListener();
        
        // Setup gestures callback
        setupGesturesCallback();
    }
    
    /**
     * Настройка listener'а видимости контроллера
     */
    private void setupControllerVisibilityListener() {
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            boolean shouldBeVisible = visibility == View.VISIBLE;
            
            // Animate controller visibility with alpha
            animateControllerVisibility(shouldBeVisible);
            
            isControllerVisible = shouldBeVisible;
            
            // Update navigation buttons visibility
            episodesManager.updateEpisodeNavigationButtonsVisibility();
            
            // Update button visibility
            updateControllerButtonsVisibility(isControllerVisible);
        });
    }
    
    /**
     * Анимация видимости контроллера через alpha
     */
    private void animateControllerVisibility(boolean visible) {
        if (controllerView == null) return;
        
        // Cancel any existing animation
        controllerView.animate().cancel();
        
        if (visible) {
            // Show controller with fade in
            controllerView.setVisibility(View.VISIBLE);
            controllerView.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал видимым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(true);
                        }
                    })
                    .start();
        } else {
            // Hide controller with fade out
            controllerView.animate()
                    .alpha(0.0f)
                    .setDuration(150)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал скрытым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(false);
                        }
                    })
                    .withEndAction(() -> {
                        // Set visibility to GONE after animation completes
                        if (controllerView.getAlpha() == 0.0f) {
                            controllerView.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }
    
    
    /**
     * Обновление видимости кнопок контроллера
     */
    private void updateControllerButtonsVisibility(boolean visible) {
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        int visibility = visible ? View.VISIBLE : View.GONE;
        
        if (settingsButton != null) settingsButton.setVisibility(visibility);
        updateSettingsQualityTag();
        
        if (menuToggleFullscreen != null) {
            menuToggleFullscreen.setVisibility(visibility);
            menuToggleFullscreen.setImageResource(isPortrait ? R.drawable.ic_fullscreen : R.drawable.ic_fullscreen_exit);
        }

        if (!isPortrait) {
            if (episodesMenuButton != null) episodesMenuButton.setVisibility(visibility);
            if (menuToggleButton != null) menuToggleButton.setVisibility(visibility);
            commentsManager.updateCommentsButtonVisibility(visible);
        } else {
            if (episodesMenuButton != null) episodesMenuButton.setVisibility(View.GONE);
            if (menuToggleButton != null) menuToggleButton.setVisibility(View.GONE);
            commentsManager.updateCommentsButtonVisibility(false);
        }
    }

    private void updateSettingsQualityTag() {
        if (settingsQualityTag != null) {
            if (settingsButton != null && settingsButton.getVisibility() != View.VISIBLE) {
                settingsQualityTag.setVisibility(View.GONE);
                return;
            }
            String tag = com.example.animelib.util.FloatingBottomSheetUtils.getQualityTag(preferredQuality);
            if (tag != null && !tag.isEmpty()) {
                settingsQualityTag.setText(tag);
                settingsQualityTag.setVisibility(View.VISIBLE);
            } else {
                settingsQualityTag.setVisibility(View.GONE);
            }
        }
    }

    private void showSurroundSoundToast(boolean enabled) {
        // Toast notification disabled by user request
    }
    
    /**
     * Настройка click listeners для кнопок
     */
    private void setupButtonClickListeners() {
        // Navigation buttons
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setOnClickListener(v -> {
                if (prevEpisodeButton.isEnabled()) {
                    episodesManager.navigateToPreviousEpisode();
                }
            });
        }
        
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setOnClickListener(v -> {
                if (nextEpisodeButton.isEnabled()) {
                    episodesManager.navigateToNextEpisode();
                }
            });
        }
        
        // Menu buttons
        if (menuToggleButton != null) {
            menuToggleButton.setOnClickListener(v -> toggleMenu());
        }
        
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (playersManager.getCurrentPlayerData() != null) {
                    playersManager.hideMenu();
                } else {
                    finish();
                }
            });
        }
        
        // Control buttons
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsDialog());
        }
        
        if (menuToggleFullscreen != null) {
            menuToggleFullscreen.setOnClickListener(v -> toggleOrientation());
        }
        
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед переходом в PiP
                autoSaveBookmark();
                enterPictureInPictureMode();
            });
        }
        
        if (ibClosePlayer != null) {
            ibClosePlayer.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед закрытием
                autoSaveBookmark();
                finish();
            });
        }

        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesInController());
        }

        View.OnClickListener downloadClickListener = v -> {
            if (DownloadService.isRunning()) {
                showDownloadProgressBottomSheet();
            } else {
                showDownloadBottomSheet();
            }
        };

        if (downloadButton != null) {
            downloadButton.setOnClickListener(downloadClickListener);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setOnClickListener(downloadClickListener);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setOnClickListener(downloadClickListener);
        }
        
        if (bookmarkButton != null) {
            Log.d("VideoPlayer", "Bookmark button found, visibility: " + bookmarkButton.getVisibility() + 
                               ", enabled: " + bookmarkButton.isEnabled() + 
                               ", clickable: " + bookmarkButton.isClickable());
            
            // Изначально отключаем кнопку до готовности плеера
            bookmarkButton.setEnabled(false);
            bookmarkButton.setClickable(false);
            
            bookmarkButton.setOnClickListener(v -> {
                Log.d("VideoPlayer", "Bookmark button clicked!");
                addBookmark();
            });
        } else {
            Log.e("VideoPlayer", "Bookmark button is null!");
        }
        
        // Setup player control buttons
        setupPlayerControlButtons();
    }
    
    /**
     * Настройка touch listener для overlay
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupOverlayTouchListener() {
        if (menuOverlay == null) return;
        
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setAlpha(0f);
        menuOverlay.setClickable(false);
        
        menuOverlay.setOnTouchListener((v, event) -> {
            if (!playersManager.isMenuVisible() && !commentsManager.isCommentsVisible()) return false;
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            boolean insidePanel = false;
            
            if (slidingMenuPanel != null && playersManager.isMenuVisible()) {
                Rect panelRect = new Rect();
                slidingMenuPanel.getGlobalVisibleRect(panelRect);
                insidePanel = panelRect.contains(x, y);
            }
            
            if (!insidePanel) {
                if (playersManager.isMenuVisible()) playersManager.hideMenu();
                if (commentsManager.isCommentsVisible()) commentsManager.hideCommentsPanel();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Настройка player listener
     */
    private void setupPlayerListener() {
        if (player != null) {
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    episodesManager.updateEpisodeNavigationButtonsVisibility();
                }
            });
        }
    }
    
    /**
     * Настройка gestures callback
     */
    private void setupGesturesCallback() {
        gesturesManager.setGestureCallback(new GesturesManager.GestureCallback() {
            @Override
            public void onSeekGesture(long seekPosition) {
                Log.d("VideoPlayer", "Seek gesture: " + seekPosition);
            }
            
            @Override
            public void onSpeedChange(float speed) {
                Log.d("VideoPlayer", "Speed change: " + speed);
            }
            
            @Override
            public void updatePlayLoadingIndicator(int playbackState) {
                VideoPlayerActivity.this.updatePlayLoadingIndicator(playbackState);
            }
            
            @Override
            public void onEpisodesSwipeUp() {
                Log.d("VideoPlayer", "Episodes swipe up detected");
                if (ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                // Открываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.showEpisodesMenu();
                }
            }
            
            @Override
            public void onEpisodesSwipeDown() {
                Log.d("VideoPlayer", "Episodes swipe down detected");
                if (ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                // Закрываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.hideEpisodesMenu();
                }
            }
            
            @Override
            public void onCommentsSwipeFromRight() {
                Log.d("VideoPlayer", "Comments swipe from right detected");
                // Открываем панель комментариев
                if (commentsManager != null) {
                    commentsManager.showCommentsPanel();
                }
            }
            
            @Override
            public void onPlayersSwipeFromRight() {
                Log.d("VideoPlayer", "Players swipe from right detected");
                // Открываем панель озвучек
                if (playersManager != null) {
                    playersManager.showMenu();
                }
            }
            
            @Override
            public void onEpisodesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Эпизоды теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onRelatedTitlesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Related titles теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onCommentsDragProgress(float progress) {
                if (isOfflineMode) return;
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Скрываем худ плеера при начале drag панели комментариев
                if (progress > 0 && playerView != null) {
                    playerView.hideController();
                }
                
                // Обновляем прогресс вытягивания панели комментариев
                if (commentsPanelContainer != null) {
                    commentsPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPlayersDragProgress(float progress) {
                if (isOfflineMode) return;
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Скрываем худ плеера при начале drag панели озвучек
                if (progress > 0 && playerView != null) {
                    playerView.hideController();
                }
                
                // Обновляем прогресс вытягивания панели озвучек
                if (menuPanelContainer != null) {
                    menuPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPanelDragComplete(GesturesManager.EdgeSwipeType type, boolean shouldOpen) {
                if (isOfflineMode) return;
                Log.d("VideoPlayer", "Panel drag complete: " + type + ", shouldOpen=" + shouldOpen);
                
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ (эпизоды и связанные тайтлы) ОБРАБАТЫВАЮТСЯ В VerticalGesturesManager
                // Здесь только горизонтальные (комментарии и озвучки)
                
                switch (type) {
                    case COMMENTS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (commentsPanelContainer != null) {
                            commentsPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (commentsManager != null) {
                            commentsManager.updateDragState(shouldOpen);
                        }
                        break;
                        
                    case PLAYERS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (menuPanelContainer != null) {
                            menuPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (playersManager != null) {
                            playersManager.updateDragState(shouldOpen);
                        }
                        break;
                }
            }
            
            @Override
            public boolean isEpisodesMenuVisible() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedTitlesMenuVisible() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
            }
            
            @Override
            public void onDoubleTapSkip(boolean isForward, int skipDurationSeconds) {
                if (player == null) {
                    Log.w("VideoPlayer", "Player is null, cannot perform skip");
                    return;
                }

                long currentPosition = player.getCurrentPosition();
                long skipDuration = skipDurationSeconds * 1000L;
                long newPosition = isForward ? currentPosition + skipDuration : currentPosition - skipDuration;
                long duration = player.getDuration();
                
                // Ограничиваем позицию границами видео
                newPosition = Math.max(0, newPosition);
                if (duration > 0) {
                    newPosition = Math.min(newPosition, duration);
                }
                
                player.seekTo(newPosition);
                Log.d("VideoPlayer", "Double tap skip: " + (isForward ? "forward" : "backward") + 
                      " to " + (newPosition / 1000) + "s (skip=" + (skipDuration / 1000) + "s)");
            }
        });
        
        // Setup vertical gestures callback
        verticalGesturesManager.setCallback(new VerticalGesturesManager.VerticalGestureCallback() {
            @Override
            public void onEpisodesDragProgress(float progress) {
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag (первый вызов с progress > 0)
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on episodes drag start");
                }
                
                if (episodesManager != null) {
                    episodesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onRelatedInfoDragProgress(float progress) {
                if (isOfflineMode) return;
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on related titles drag start");
                }
                
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onEpisodesDragComplete(boolean shouldOpen) {
                Log.d("VideoPlayer", "Episodes drag complete: shouldOpen=" + shouldOpen);
                if (episodesManager != null) {
                    // Используем completeDrag для корректного завершения анимации
                    episodesManager.completeDrag(shouldOpen);
                }
            }
            
            @Override
            public void onRelatedInfoDragComplete(boolean shouldOpen) {
                if (isOfflineMode) return;
                Log.d("VideoPlayer", "Related info drag complete: shouldOpen=" + shouldOpen);
                // Сначала сбрасываем эпизоды если они открыты
                if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                    episodesManager.resetControllerPosition();
                }
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.completeDrag(shouldOpen);
                }
            }
            
            @Override
            public boolean isEpisodesOpen() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedInfoOpen() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
            }
        });
    }
    
    /**
     * Настройка начального состояния UI
     */
    private void configureInitialState() {
        // Hide controller buttons initially
        updateControllerButtonsVisibility(false);
        
        // Set initial episode navigation buttons visibility
        if (prevEpisodeButton != null) prevEpisodeButton.setVisibility(View.VISIBLE);
        if (nextEpisodeButton != null) nextEpisodeButton.setVisibility(View.VISIBLE);
        
        // Players control bar position is now managed by EpisodesManager
    }
    
    /**
     * Настройка callbacks для менеджеров
     */
    private void setupManagerCallbacks() {
        // Comments manager callbacks
        commentsManager.setVisibilityCallback(isVisible -> {
            if (isVisible && playerView != null) {
                playerView.hideController();
            }
            if (episodesManager != null) {
                episodesManager.updateEpisodeNavigationButtonsVisibility();
            }
        });
        
        // Episodes manager callbacks
        episodesManager.setEpisodeSelectionCallback(this::onEpisodeSelected);
        episodesManager.setVisibilityCallback(isVisible -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        });
        
        // Related titles manager callbacks
        relatedTitlesManager.setVisibilityCallback(isVisible -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        });
        episodesManager.setDataCallback(new EpisodesManager.EpisodesDataCallback() {
            @Override
            public void onEpisodesLoaded(List<EpisodesListResponse.EpisodeItem> episodes) {
                Log.d("VideoPlayer", "Episodes loaded: " + episodes.size());
                
                // Сначала пытаемся загрузить эпизод из закладки
                if (currentAnimeId != null) {
                    Log.d("VideoPlayer", "Trying to load episode from bookmark for anime: " + currentAnimeId);
                    
                    // Получаем media_slug для закладки
                    String animeUrl = getIntent().getStringExtra("anime_url");
                    String mediaSlug = null;
                    if (animeUrl != null && !animeUrl.isEmpty()) {
                        mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
                    }
                    
                    if (mediaSlug != null) {
                        episodesManager.getBookmarkManager().getLastEpisodeFromBookmark(mediaSlug, episodes, 
                            new BookmarkManager.LastEpisodeCallback() {
                                @Override
                                public void onLastEpisodeFound(EpisodesListResponse.EpisodeItem episode, String progress) {
                                    Log.d("VideoPlayer", "Found bookmarked episode: " + episode.getNumber() + 
                                                         ", progress: " + progress);
                                    
                                    // Сохраняем таймкод из закладки
                                    bookmarkTimecode = episodesManager.getBookmarkManager().parseTimecodeToMilliseconds(progress);
                                    Log.d("VideoPlayer", "Parsed bookmark timecode: " + progress + " -> " + bookmarkTimecode + "ms");
                                    
                                    // Устанавливаем красный цвет кнопки для эпизода с закладкой
                                    updateBookmarkButtonColor(true);
                                    
                                    episodesManager.setCurrentEpisode(episode);
                                    commentsManager.setCurrentEpisode(episode);
                                    
                                    // СРАЗУ обновляем заголовок с номером эпизода
                                    updateEpisodeHeaderQuick();
                                    
                                    // ВАЖНО: Загружаем плееры для ПРАВИЛЬНОГО эпизода
                                    Log.d("VideoPlayer", "Loading players for bookmarked episode: " + episode.getId());
                                    playersManager.loadPlayersForEpisode(episode.getId());
                                }
                                
                                @Override
                                public void onNoBookmarkFound() {
                                    Log.d("VideoPlayer", "No bookmark found, loading first episode");
                                    loadFirstEpisode();
                                }
                            });
                    } else {
                        Log.d("VideoPlayer", "No media slug available, falling back to URL detection");
                        fallbackToUrlDetection();
                    }
                } else {
                    // Нет anime ID, используем URL detection
                    fallbackToUrlDetection();
                }
            }
            
            @Override
            public void onEpisodesError(String error) {
                Log.e("VideoPlayer", "Error loading episodes: " + error);
                showVideoErrorDialog("Ошибка загрузки озвучек", "Не удалось загрузить список эпизодов и озвучек:\n" + error, () -> {
                    if (currentAnimeId != null) {
                        loadEpisodes(currentAnimeId);
                    }
                }, true);
                initializeMenuWithoutAutoPlay();
            }
        });
        
        // Players manager callbacks
        playersManager.setPlayerSelectionCallback(this::onPlayerSelected);
        
        // Устанавливаем callback для загрузки плееров
        playersManager.setDataCallback(new PlayersManager.PlayersDataCallback() {
            @Override
            public void onPlayersLoaded(List<EpisodeResponse.PlayerData> players) {
                Log.d("VideoPlayer", "Players loaded: " + players.size() + " for episode: " + 
                      (episodesManager.getCurrentEpisode() != null ? episodesManager.getCurrentEpisode().getNumber() : "unknown"));
                
                // Проверяем нужно ли показывать меню
                // Показываем только если нет выбранной озвучки (первая загрузка)
                if (playersManager.getCurrentPlayerData() == null) {
                    Log.d("VideoPlayer", "No player selected yet, showing menu for first time");
                    showPlayerSelectionDialogWithAutoSelect(players);
                } else {
                    Log.d("VideoPlayer", "Player already selected, skipping menu (episode switch)");
                    // При переключении эпизода меню не показываем
                    // Автовыбор уже сработал в PlayersManager
                    if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onPlayersError(String error) {
                Log.e("VideoPlayer", "Error loading players: " + error);
                showVideoErrorDialog("Ошибка загрузки плееров", "Не удалось загрузить список плееров:\n" + error, () -> {
                    EpisodesListResponse.EpisodeItem episode = episodesManager.getCurrentEpisode();
                    if (episode != null) {
                        playersManager.loadPlayersForEpisode(episode.getId());
                    }
                }, true);
            }
        });
        
        // Показываем placeholder когда нет выбранной озвучки
        playersManager.setVisibilityCallback(isVisible -> {
            if (isVisible && playerView != null) {
                playerView.hideController();
            }
            if (!isVisible && playersManager.getCurrentPlayerData() == null) {
                // Меню закрыто и нет выбранной озвучки - показываем placeholder
                showAnimeInfoPlaceholder();
            }
        });
    }
    
    /**
     * Запуск начальной загрузки
     */
    private void startInitialLoading() {
        if (isOfflineMode) {
            return;
        }
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (!isPortrait) {
            playersManager.showMenu();
            showAnimeInfoPlaceholder();
        }
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.VISIBLE);
        }
        
        // Показываем anime info placeholder и загружаем информацию
        // Placeholder будет скрыт автоматически при выборе озвучки
        loadAnimeInfoForPlaceholder();
    }
    
    /**
     * Показывает placeholder с информацией об аниме (только 1 раз при входе)
     */
    private void showAnimeInfoPlaceholder() {
        if (isOfflineMode) return;
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) return;
        if (hasShownInitialAnimeInfo) {
            Log.d("VideoPlayer", "Anime info placeholder already shown once, skipping");
            return;
        }
        hasShownInitialAnimeInfo = true;
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.VISIBLE);
            animeInfoPlaceholder.setTranslationX(0); // Показываем справа
        }
    }
    
    /**
     * Скрывает placeholder с информацией об аниме
     */
    private void hideAnimeInfoPlaceholder() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    animeInfoPlaceholder.setVisibility(View.GONE);
                    animeInfoPlaceholder.setAlpha(1f);
                })
                .start();
        }
    }
    
    /**
     * Загружает информацию об аниме для placeholder
     */
    private void loadAnimeInfoForPlaceholder() {
        if (animeUrl == null) return;
        
        // Извлекаем slug из URL для API
        String animeSlug = apiService.extractAnimeSlug(animeUrl);
        if (animeSlug == null) {
            Log.e("VideoPlayer", "Could not extract anime slug from URL: " + animeUrl);
            return;
        }
        
        Log.d("VideoPlayer", "Loading anime info for slug: " + animeSlug);
        apiService.fetchAnimeInfo(animeSlug, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                runOnUiThread(() -> displayAnimeInfo(response));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("VideoPlayer", "Failed to load anime info: " + errorMessage);
            }
        });
    }
    
    /**
     * Отображает информацию об аниме в placeholder
     */
    @SuppressLint("SetTextI18n")
    private void displayAnimeInfo(AnimeInfoResponse animeInfo) {
        if (animeInfo == null || animeInfo.getData() == null) {
            Log.e("VideoPlayer", "displayAnimeInfo: animeInfo or data is null");
            return;
        }
        
        AnimeInfoResponse.Data data = animeInfo.getData();
        Log.d("VideoPlayer", "Displaying anime info: " + data.getRus_name());
        
        // Устанавливаем информацию об аниме в панель связанных тайтлов
        setAnimeInfoToRelatedPanel(data);
        
        // Название
        if (animeInfoTitle != null) {
            SkeletonHelper.hideSkeleton(animeInfoTitle, data.getRus_name() != null ? data.getRus_name() : "—");
        }
        
        // Оригинальное название
        if (animeInfoOriginalTitle != null) {
            if (data.getEng_name() != null && !data.getEng_name().isEmpty()) {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, data.getEng_name());
                animeInfoOriginalTitle.setVisibility(View.VISIBLE);
            } else {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, "");
                animeInfoOriginalTitle.setVisibility(View.GONE);
            }
        }
        
        // Год из releaseDate
        if (animeInfoYear != null) {
            String year = (data.getReleaseDate() != null && data.getReleaseDate().length() >= 4)
                    ? data.getReleaseDate().substring(0, 4) : "—";
            SkeletonHelper.hideSkeleton(animeInfoYear, year);
        }
        
        // Тип
        if (animeInfoType != null) {
            String type = (data.getType() != null && data.getType().getLabel() != null)
                    ? data.getType().getLabel().replace("TV ", "") : "—";
            SkeletonHelper.hideSkeleton(animeInfoType, type);
        }
        
        // Статус
        if (animeInfoStatus != null) {
            String statusLabel = (data.getStatus() != null && data.getStatus().getLabel() != null)
                    ? data.getStatus().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoStatus, statusLabel);
        }
        
        // Рейтинг
        if (animeInfoRating != null) {
            String ratingStr = (data.getRating() != null && data.getRating().getAverageFormated() != null)
                    ? data.getRating().getAverageFormated() : "—";
            SkeletonHelper.hideSkeleton(animeInfoRating, ratingStr);
        }
        
        // Количество эпизодов
        if (animeInfoEpisodes != null) {
            String epStr = (data.getItems_count() != null && data.getItems_count().getUploaded() > 0)
                    ? (data.getItems_count().getUploaded() + " эпизодов") : "—";
            SkeletonHelper.hideSkeleton(animeInfoEpisodes, epStr);
        }
        
        // Возрастное ограничение
        if (animeInfoAge != null) {
            String ageStr = (data.getAgeRestriction() != null && data.getAgeRestriction().getLabel() != null)
                    ? data.getAgeRestriction().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoAge, ageStr);
        }
        
        // Дата выхода
        if (animeInfoReleaseDate != null) {
            String relStr = data.getReleaseDateString() != null ? data.getReleaseDateString() : "—";
            SkeletonHelper.hideSkeleton(animeInfoReleaseDate, relStr);
        }
        
        // Shikimori рейтинг
        if (animeInfoShikimori != null) {
            String shikiStr = data.getShikimori_href() != null ? String.valueOf(data.getShiki_rate()) : "—";
            SkeletonHelper.hideSkeleton(animeInfoShikimori, shikiStr);
        }
        
        // Постер - загружаем через ImageLoader
        if (animeInfoPoster != null && data.getCover() != null) {
            String posterUrl = data.getCover().getDefaultUrl();
            if (posterUrl != null && !posterUrl.isEmpty()) {
                currentPosterUrl = posterUrl;
                com.example.animelib.util.ImageLoader.getInstance()
                    .loadInto(animeInfoPoster, posterUrl, R.drawable.ic_image_placeholder);
            } else {
                animeInfoPoster.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }

    private void toggleMenu() {
        if (isOfflineMode) return;
        playersManager.toggleMenu();
    }
    
    /**
     * Показывает оверлей следующего эпизода с обратным отсчетом
     */
    private void showNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null || episodesManager == null) return;
        
        // Проверяем есть ли следующий эпизод
        EpisodesListResponse.EpisodeItem nextEpisode = episodesManager.getNextEpisode();
        if (nextEpisode == null) {
            Log.d("VideoPlayer", "No next episode available");
            return;
        }
        
        Log.d("VideoPlayer", "Showing next episode overlay for episode: " + nextEpisode.getNumber());
        
        // Устанавливаем номер эпизода
        if (nextEpisodeNumber != null) {
            nextEpisodeNumber.setText("эпизод " + nextEpisode.getNumber());
        }
        
        // Сбрасываем счетчик
        countdownSeconds = 7;
        if (nextEpisodeCountdown != null) {
            nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
        }
        
        // Показываем оверлей
        nextEpisodeOverlay.setVisibility(View.VISIBLE);
        nextEpisodeOverlay.setAlpha(0f);
        nextEpisodeOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Запускаем обратный отсчет
        startCountdown();
    }
    
    /**
     * Запускает обратный отсчет до следующего эпизода
     */
    private void startCountdown() {
        if (nextEpisodeHandler == null) return;
        
        nextEpisodeRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds--;
                
                if (nextEpisodeCountdown != null) {
                    nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
                }
                
                if (countdownSeconds > 0) {
                    nextEpisodeHandler.postDelayed(this, 1000);
                } else {
                    // Время вышло - запускаем следующий эпизод
                    playNextEpisodeNow();
                }
            }
        };
        
        nextEpisodeHandler.postDelayed(nextEpisodeRunnable, 1000);
    }
    
    /**
     * Отменяет автовоспроизведение следующего эпизода
     */
    private void cancelNextEpisode() {
        Log.d("VideoPlayer", "Next episode cancelled by user");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
    }
    
    /**
     * Немедленно запускает следующий эпизод
     */
    private void playNextEpisodeNow() {
        Log.d("VideoPlayer", "Playing next episode now");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
        
        // Переключаемся на следующий эпизод
        if (episodesManager != null) {
            episodesManager.navigateToNextEpisode();
        }
    }
    
    /**
     * Скрывает оверлей следующего эпизода
     */
    private void hideNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null) return;
        
        nextEpisodeOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                if (nextEpisodeOverlay != null) {
                    nextEpisodeOverlay.setVisibility(View.GONE);
                }
            })
            .start();
    }

    private void toggleEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.toggleEpisodesMenu();
        }
    }

    private void hideEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.hideEpisodesMenu();
        }
    }

    private void updatePlayerControlsState() {
        if (player == null) return;

        runOnUiThread(() -> {
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView != null) {
                ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

                // Включаем/выключаем кнопки в зависимости от состояния плеера
                boolean canSeek = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM);

                if (skipForwardButton != null) {
                    skipForwardButton.setEnabled(canSeek);
                    skipForwardButton.setAlpha(canSeek ? 1.0f : 0.5f);
                }

                // Обновляем видимость play/pause кнопок
                updatePlayPauseButtonsVisibility();
            }
        });
    }

    private void updatePlayPauseButtonsVisibility() {
        updatePlayPauseAndLoadingState(true);
    }

    private void onPlayerSelected(EpisodeResponse.PlayerData playerData) {
        Log.d("VideoPlayer", "Player selected: " + playerData.getPlayer());

        autoPlayOnPrepare = player == null || player.getPlayWhenReady();

        hideAnimeInfoPlaceholder();

        updateEpisodeHeaderQuick();

        if (playerData.getPlayer() != null && playerData.getTeam() != null) {
            apiService.savePlayerPreferences(playerData.getPlayer(), playerData.getTeam().getId());
            Log.d("VideoPlayer", "Immediately saved player preferences: player=" + playerData.getPlayer() + 
                  ", teamId=" + playerData.getTeam().getId());
        }

        if (player != null) {
            savedPlayerPosition = player.getCurrentPosition();
            Log.d("VideoPlayer", "Saved player position: " + savedPlayerPosition + "ms before switching to: " + playerData.getPlayer());
        }

        stopCurrentPlayback();

        List<String> newQualities = playersManager.getAvailableQualities();

        enableBookmarkButton();

        playersManager.setCurrentPlayerData(playerData);

        Log.d("VideoPlayer", "Player selected, ready to start playback");
        
        if (!newQualities.isEmpty()) {
            executor.execute(() -> {
                com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
                String savedQuality = (prefs != null) ? prefs.getPreferredQuality() : null;
                
                safeRunOnUiThread(() -> {
                    String newPreferredQuality = null;
                    
                    if (savedQuality != null && newQualities.contains(savedQuality)) {
                        newPreferredQuality = savedQuality;
                        Log.d("VideoPlayer", "Using saved quality: " + savedQuality);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            newPreferredQuality = newQualities.getFirst();
                        }
                        Log.d("VideoPlayer", "Saved quality not found, using max quality: " + newPreferredQuality);
                    }
                    
                    preferredQuality = newPreferredQuality;
                    updateSettingsQualityTag();
                    Log.d("VideoPlayer", "Updated preferred quality to: " + newPreferredQuality + " for player: " + playerData.getPlayer());
                    
                    if (playerData.getPlayer() != null && playerData.getTeam() != null) {
                        apiService.savePlayerPreferences(playerData.getPlayer(), playerData.getTeam().getId(), preferredQuality);
                        Log.d("VideoPlayer", "Updated player preferences with quality: player=" + playerData.getPlayer() + 
                              ", teamId=" + playerData.getTeam().getId() + ", quality=" + preferredQuality);
                    }
                    
                    if (currentSettingsBottomSheet != null) {
                        currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                    }
                    
                    long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
                    Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (bookmark: " + bookmarkTimecode + "ms, saved: " + savedPlayerPosition + "ms)");
                    
                    if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                        handleAnimelibPlayer(playerData, startPosition);
                    } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                        handleKodikPlayer(playerData, startPosition);
                    }
                    
                    updateAnimeInfoHeaderFull();
                });
            });
        } else {
            long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
            Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (no qualities available)");
            
            if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                handleAnimelibPlayer(playerData, startPosition);
            } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                handleKodikPlayer(playerData, startPosition);
            }
            
            updateAnimeInfoHeaderFull();
        }
    }

    private void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode, boolean autoPlay) {
        Log.d("VideoPlayer", "Episode selected: " + episode.getNumber() + " (ID: " + episode.getId() + "), autoPlay: " + autoPlay);

        // Слайдер эпизодов открывает серию на паузе, кнопки вперёд/назад — сразу
        autoPlayOnPrepare = autoPlay;

        // Reset bookmark timecode for new episode selection
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for new episode");
        
        // Reset saved player position for new episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for new episode");
        
        // Reset auto-bookmark flag for new episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for new episode");
        
        // Reset bookmark button color for new episode
        updateBookmarkButtonColor(false);

        // Stop current playback if playing
        stopCurrentPlayback();

        // Hide episodes in controller
        hideEpisodesInController();

        // Update current episode in both managers
        episodesManager.setCurrentEpisode(episode);
        commentsManager.setCurrentEpisode(episode);
        commentsManager.resetCommentsOnEpisodeChange(true);

        // Эпизод теперь сохраняется автоматически через закладки при добавлении
        Log.d("EpisodeMemory", "Episode " + episode.getNumber() + " is now current episode");

        // Update navigation buttons visibility
        episodesManager.updateEpisodeNavigationButtonsVisibility();

        // Update episodes RecyclerView to highlight current episode
        episodesManager.updateEpisodesRecyclerView();
        
        // СРАЗУ обновляем заголовок с номером эпизода (синхронно)
        updateEpisodeHeaderQuick();

        if (isOfflineMode) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity offlineEp = offlineEpisodesMap.get(episode.getNumber());
            if (offlineEp == null) {
                offlineEp = offlineEpisodesMap.get(String.valueOf(episode.getId()));
            }
            if (offlineEp != null && offlineEp.getLocalFilePath() != null) {
                playOfflineFile(offlineEp.getLocalFilePath(), offlineEp.getAnimeTitle(), offlineEp.getEpisodeName());
            } else {
                CustomToast.showWarning(this, "Файл серии не найден");
            }
            return;
        }

        // Show loading and load players for this episode
        runOnUiThread(() -> {
            showLoading("Загрузка плееров для эпизода...");
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.VISIBLE);
        });

        // Load players for the selected episode
        playersManager.loadPlayersForEpisode(episode.getId());
    }

    private void stopCurrentPlayback() {
        if (player != null) {
            Log.d("VideoPlayer", "Stopping current playback");
            player.stop();
            player.clearMediaItems();
        }
    }

    private void showSettingsDialog() {
        if (!isOfflineMode && playersManager.getCurrentPlayerData() == null) {
            return;
        }

        List<String> availableQualities;
        if (isOfflineMode) {
            availableQualities = new ArrayList<>();
        } else {
            availableQualities = playersManager.getAvailableQualities();
            if (availableQualities.isEmpty()) {
                CustomToast.showWarning(this, "Качества недоступны");
                return;
            }
        }

        SettingsBottomSheet dialog = new SettingsBottomSheet(this, availableQualities, preferredQuality,
                quality -> {
                    String oldQuality = preferredQuality;
                    preferredQuality = quality;
                    updateSettingsQualityTag();
                    Log.d("VideoPlayer", "Selected quality: " + quality);

                    EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
                    if (currentPlayer != null && currentPlayer.getPlayer() != null && currentPlayer.getTeam() != null) {
                        apiService.savePlayerPreferences(currentPlayer.getPlayer(), 
                                                        currentPlayer.getTeam().getId(), 
                                                        quality);
                        Log.d("VideoPlayer", "Saved quality preference: " + quality);
                    }

                    if (!quality.equals(oldQuality) && player != null) {
                        Log.d("VideoPlayer", "Restarting player with new quality");
                        restartPlayerWithNewQuality();
                    }
                },
                player != null ? player.getPlaybackParameters().speed : 1.0f,
                speed -> {
                    if (player != null) {
                        player.setPlaybackSpeed(speed);
                        Log.d("VideoPlayer", "Speed changed via dialog: " + speed);
                    }
                },
                enable4K,
                enabled -> {
                    enable4K = enabled;
                    apiService.save4KSetting(enabled);
                    playersManager.setEnable4K(enabled);
                    Log.d("VideoPlayer", "4K setting changed to: " + enabled);
                    List<String> newQualities = playersManager.getAvailableQualities();
                    Log.d("VideoPlayer", "New qualities after 4K toggle: " + newQualities);
                    if (!newQualities.isEmpty()) {
                        if (!newQualities.contains(preferredQuality)) {
                            String oldQuality = preferredQuality;
                            preferredQuality = newQualities.get(0);
                            Log.d("VideoPlayer", "Preferred quality changed from " + oldQuality + " to " + preferredQuality);
                        }
                        if (currentSettingsBottomSheet != null) {
                            Log.d("VideoPlayer", "Updating SettingsBottomSheet with new qualities");
                            currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                        } else {
                            Log.w("VideoPlayer", "currentSettingsBottomSheet is null, cannot update");
                        }
                    } else {
                        Log.w("VideoPlayer", "New qualities list is empty!");
                    }
                },
                enableAmbientLight,
                enabled -> {
                    enableAmbientLight = enabled;
                    apiService.saveAmbientLightSetting(enabled);
                    if (ambientLightManager != null) {
                        ambientLightManager.setEnabled(enabled);
                    }
                    Log.d("VideoPlayer", "Ambient light setting changed to: " + enabled);
                },
                autoPlay,
                enabled -> {
                    autoPlay = enabled;
                    apiService.saveAutoPlaySetting(enabled);
                    Log.d("VideoPlayer", "AutoPlay enabled: " + enabled);
                },
                longSkipDuration,
                duration -> {
                    longSkipDuration = duration;
                    apiService.saveLongSkipDurationSetting(duration);
                    Log.d("VideoPlayer", "LongSkipDuration changed: " + duration);
                },
                currentTheme,
                themeMode -> {
                    currentTheme = themeMode;
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, themeMode);
                    setupFullscreen();
                    checkAndUpdateOrientation();
                    apiService.saveThemeSetting(themeMode);
                    Log.d("VideoPlayer", "Theme changed: " + themeMode);
                });

        dialog.setOfflineMode(isOfflineMode);
        dialog.setSurround3DSettings(
                enableSurroundSound,
                surroundMode,
                surroundSpatialWidth,
                surroundDialogueBoost,
                surroundBassBoost,
                surroundTrebleBoost,
                (enabled, mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost) -> {
                    enableSurroundSound = enabled;
                    surroundMode = mode;
                    surroundSpatialWidth = spatialWidth;
                    surroundDialogueBoost = dialogueBoost;
                    surroundBassBoost = bassBoost;
                    surroundTrebleBoost = trebleBoost;

                    apiService.saveSurroundSoundSetting(enabled);
                    apiService.saveSurround3DSettings(mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost);

                    if (surroundSoundManager != null) {
                        surroundSoundManager.setEnabled(enabled);
                        surroundSoundManager.setSpatialMode(mode);
                        surroundSoundManager.setSpatialWidth(spatialWidth);
                        surroundSoundManager.setDialogueBoost(dialogueBoost);
                        surroundSoundManager.setBassBoostLevel(bassBoost);
                        surroundSoundManager.setTrebleBoostLevel(trebleBoost);
                    }
                    showSurroundSoundToast(enabled);
                    Log.d("VideoPlayer", "3D Surround sound settings changed: enabled=" + enabled + ", mode=" + mode + ", width=" + spatialWidth + ", dialogue=" + dialogueBoost + ", bass=" + bassBoost + ", treble=" + trebleBoost);
                }
        );
        dialog.setResizeMode(currentResizeMode, newMode -> setVideoResizeMode(newMode));
        currentSettingsBottomSheet = dialog;

        dialog.setOnShowListener(dialogInterface -> {
            applySettingsFromDialog(dialog);
        });

        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }

        dialog.setOnDismissListener(dialogInterface -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        });

        dialog.show();
    }

    private Context getPlayerContext() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Context attributionContext = createAttributionContext("default");
                if (attributionContext != null) {
                    return attributionContext;
                }
            } catch (Exception e) {
                Log.e("VideoPlayer", "Error creating attribution context", e);
            }
        }
        return this;
    }

    private void playOfflineFile(String localPath, String animeTitle, String episodeTitle) {
        isOfflineMode = true;
        java.io.File file = new java.io.File(localPath);
        if (!file.exists()) {
            CustomToast.showWarning(this, "Офлайн файл не найден: " + localPath);
            finish();
            return;
        }

        if (animeTitleView != null) {
            String fullTitle = (animeTitle != null ? animeTitle : "") +
                    (episodeTitle != null ? " - " + episodeTitle : "");
            SkeletonHelper.hideSkeleton(animeTitleView, fullTitle);
        }
        if (tvPortraitAnimeTitle != null) {
            SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, animeTitle != null ? animeTitle : "");
        }
        if (tvPortraitEpisodeTitle != null) {
            SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, episodeTitle != null ? episodeTitle : "");
        }

        Uri videoUri = Uri.fromFile(file);
        if (player == null) {
            com.example.animelib.util.SurroundRenderersFactory rf = new com.example.animelib.util.SurroundRenderersFactory(
                    getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);
            player = new androidx.media3.exoplayer.ExoPlayer.Builder(getPlayerContext(), rf)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000)
                    .build();
            playerView.setPlayer(player);
            setVideoResizeMode(currentResizeMode);
        } else {
            player.stop();
            player.clearMediaItems();
        }

        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        playerView.setUseController(true);
        updateControllerAutoHide();

        if (gesturesManager != null) {
            gesturesManager.updatePlayer(player);
        }

        com.google.android.material.button.MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        if (timecodeManager != null) {
            timecodeManager.initializeViews(player, playerView, skipSegmentButton);
        }

        androidx.media3.common.MediaItem mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        setupPlayerControlButtons();

        hideLoading();
        setupOfflineEpisodes(localPath);
        applyOfflineUIState();
    }

    private void setupOfflineEpisodes(String localPath) {
        if (!isOfflineMode) return;

        executor.execute(() -> {
            com.example.animelib.data.DatabaseManager databaseManager = apiService.getDatabaseManager();
            if (databaseManager == null) return;

            String animeId = getIntent().getStringExtra("EXTRA_ANIME_ID");
            String epNumber = getIntent().getStringExtra("EXTRA_EPISODE_NUMBER");

            if (animeId == null && localPath != null) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity currentEp = databaseManager.findEpisodeByPath(localPath);
                if (currentEp != null) {
                    animeId = currentEp.getAnimeId();
                    if (epNumber == null) {
                        epNumber = currentEp.getEpisodeNumber();
                    }
                }
            }

            if (animeId != null) {
                currentAnimeId = animeId;
                List<com.example.animelib.data.entity.DownloadedEpisodeEntity> downloadedEps = databaseManager.getEpisodesForAnimeSync(animeId);
                if (downloadedEps != null && !downloadedEps.isEmpty()) {
                    List<EpisodesListResponse.EpisodeItem> episodeItems = new ArrayList<>();
                    EpisodesListResponse.EpisodeItem currentItem = null;

                    for (com.example.animelib.data.entity.DownloadedEpisodeEntity dep : downloadedEps) {
                        EpisodesListResponse.EpisodeItem item = new EpisodesListResponse.EpisodeItem();
                        int id = 0;
                        try {
                            id = Integer.parseInt(dep.getEpisodeNumber());
                        } catch (Exception ignored) {
                            id = Math.abs(dep.getId().hashCode());
                        }
                        item.setId(id);
                        item.setNumber(dep.getEpisodeNumber());
                        item.setName(dep.getEpisodeName() != null ? dep.getEpisodeName() : "Серия " + dep.getEpisodeNumber());

                        if (dep.getEpisodeNumber() != null) {
                            offlineEpisodesMap.put(dep.getEpisodeNumber(), dep);
                        }
                        offlineEpisodesMap.put(String.valueOf(id), dep);

                        episodeItems.add(item);

                        if ((localPath != null && localPath.equals(dep.getLocalFilePath())) ||
                            (epNumber != null && epNumber.equals(dep.getEpisodeNumber()))) {
                            currentItem = item;
                        }
                    }

                    if (currentItem == null && !episodeItems.isEmpty()) {
                        currentItem = episodeItems.get(0);
                    }

                    final List<EpisodesListResponse.EpisodeItem> finalItems = episodeItems;
                    final EpisodesListResponse.EpisodeItem finalCurrent = currentItem;

                    runOnUiThread(() -> {
                        if (episodesManager != null) {
                            episodesManager.setEpisodes(finalItems);
                            if (finalCurrent != null) {
                                episodesManager.setCurrentEpisode(finalCurrent);
                            }
                            episodesManager.updateEpisodeNavigationButtonsVisibility();
                        }
                        if (episodesMenuButton != null) {
                            episodesMenuButton.setEnabled(true);
                            episodesMenuButton.setAlpha(1.0f);
                            episodesMenuButton.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    private void applyOfflineUIState() {
        isOfflineMode = true;

        if (menuPanelContainer != null) {
            menuPanelContainer.forceClose();
            menuPanelContainer.setVisibility(View.GONE);
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.forceClose();
            commentsPanelContainer.setVisibility(View.GONE);
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.GONE);
        }
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.GONE);
        }

        // Сбрасываем трансформацию плеера к полноэкранному состоянию
        applyPlayerSidePanelTransform(0f);
        if (playerContainer != null) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
        }

        if (settingsButton != null) {
            settingsButton.setEnabled(true);
            settingsButton.setAlpha(1.0f);
            settingsButton.setVisibility(View.VISIBLE);
        }

        if (menuToggleButton != null) {
            menuToggleButton.setEnabled(false);
            menuToggleButton.setAlpha(0.3f);
        }
        boolean hasEpisodes = episodesManager != null && !episodesManager.getEpisodes().isEmpty();
        if (episodesMenuButton != null) {
            episodesMenuButton.setEnabled(hasEpisodes);
            episodesMenuButton.setAlpha(hasEpisodes ? 1.0f : 0.3f);
            episodesMenuButton.setVisibility(View.VISIBLE);
        }
        if (commentsButton != null) {
            commentsButton.setEnabled(false);
            commentsButton.setAlpha(0.3f);
        }
        if (bookmarkButton != null) {
            bookmarkButton.setEnabled(false);
            bookmarkButton.setAlpha(0.3f);
        }
        if (episodesManager != null) {
            episodesManager.updateEpisodeNavigationButtonsVisibility();
        }
        if (downloadButton != null) {
            downloadButton.setEnabled(false);
            downloadButton.setAlpha(0.3f);
            downloadButton.setVisibility(View.GONE);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setEnabled(false);
            downloadButtonTop.setAlpha(0.3f);
            downloadButtonTop.setVisibility(View.GONE);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setEnabled(false);
            btnDownloadFromMenu.setAlpha(0.3f);
            btnDownloadFromMenu.setVisibility(View.GONE);
        }

        hideLoading();

        View controllerView = playerView.findViewById(R.id.exo_controller);
        if (controllerView != null) {
            View spinner = controllerView.findViewById(R.id.playLoadingIndicator);
            if (spinner != null) {
                spinner.setVisibility(View.GONE);
            }
            View play = controllerView.findViewById(R.id.exo_play);
            View pause = controllerView.findViewById(R.id.exo_pause);
            if (player != null) {
                boolean isPlaying = player.isPlaying();
                if (play != null) play.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                if (pause != null) pause.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Показывает окно выбора серий для скачивания (BottomSheet)
     */
    public void showDownloadBottomSheet() {
        if (isOfflineMode) {
            CustomToast.showInfo(this, "Воспроизводится скачанный файл");
            return;
        }
        if (currentAnimeId == null) {
            CustomToast.showWarning(this, "Информация об аниме ещё не загружена");
            return;
        }

        requestNotificationPermission();

        List<EpisodesListResponse.EpisodeItem> epList = episodesManager != null ? episodesManager.getEpisodes() : new ArrayList<>();
        List<EpisodeResponse.PlayerData> players = playersManager != null ? playersManager.getAllPlayers() : new ArrayList<>();

        if (players == null || players.isEmpty()) {
            CustomToast.showInfo(this, "Озвучки ещё не загружены, подождите...");
            return;
        }

        String title = animeTitleView != null ? animeTitleView.getText().toString() : "Аниме";

        com.example.animelib.ui.DownloadBottomSheet bottomSheet = com.example.animelib.ui.DownloadBottomSheet.newInstance(
                currentAnimeId,
                title,
                currentPosterUrl,
                epList,
                players
        );
        bottomSheet.show(getSupportFragmentManager(), "DownloadBottomSheet");
    }

    /**
     * Показывает окно прогресса скачивания серий (BottomSheet)
     */
    private void showDownloadProgressBottomSheet() {
        com.example.animelib.ui.DownloadProgressBottomSheet progressSheet = com.example.animelib.ui.DownloadProgressBottomSheet.newInstance();
        progressSheet.show(getSupportFragmentManager(), "DownloadProgressBottomSheet");
    }

    /**
     * Подписывает плеер на события фонового скачивания
     */
    private void setupDownloadListener() {
        DownloadService.setListener(new DownloadService.ProgressListener() {
            @Override
            public void onProgress(int currentTaskIndex, int totalTasks, int taskPercent, String currentTaskTitle) {
                safeRunOnUiThread(() -> showDownloadProgress(taskPercent));
            }

            @Override
            public void onFinished(int totalDownloaded) {
                safeRunOnUiThread(() -> {
                    resetDownloadUi();
                    CustomToast.showSuccess(VideoPlayerActivity.this,
                            "Скачивание завершено (" + totalDownloaded + " серий)");
                });
            }

            @Override
            public void onError(String message) {
                safeRunOnUiThread(() -> {
                    resetDownloadUi();
                    CustomToast.showWarning(VideoPlayerActivity.this,
                            "Ошибка скачивания: " + message);
                });
            }
        });
    }

    /**
     * Показывает плашку прогресса скачивания
     */
    private void showDownloadProgress(int percent) {
        if (downloadProgressText != null) {
            downloadProgressText.setText("Скачивание " + percent + "%");
            downloadProgressText.setVisibility(View.VISIBLE);
        }
        if (downloadButton != null) {
            downloadButton.setAlpha(0.85f);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setAlpha(0.85f);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setAlpha(1.0f);
            com.example.animelib.util.DownloadAnimationUtils.startDownloadAnimation(btnDownloadFromMenu);
        }
    }

    /**
     * Запрашивает разрешение на уведомления о прогрессе
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4201);
        }
    }

    /**
     * Возвращает ссылку на файл выбранного качества
     */
    private String resolveDownloadUrl(String quality) {
        EpisodeResponse.PlayerData playerData = playersManager.getCurrentPlayerData();
        if (playerData == null || playerData.getVideo() == null || playerData.getVideo().getQuality() == null) {
            return null;
        }

        try {
            int target = Integer.parseInt(quality.replace("p", ""));
            for (EpisodeResponse.QualityData data : playerData.getVideo().getQuality()) {
                if (data.getQuality() == target) {
                    return VideoUrlHelper.toAbsoluteVideoUrl(data.getHref());
                }
            }
        } catch (NumberFormatException e) {
            Log.w("VideoPlayer", "Invalid quality format: " + quality);
        }
        return null;
    }

    /**
     * Формирует имя файла для скачиваемой серии
     */
    private String buildDownloadFileName(String quality) {
        String title = animeTitleView != null ? animeTitleView.getText().toString() : "anime";
        EpisodesListResponse.EpisodeItem episode = episodesManager.getCurrentEpisode();
        String number = episode != null ? episode.getNumber() : "0";
        String name = title + " - " + number + " серия (" + quality + ")";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim() + ".mp4";
    }

    /**
     * Возвращает элементы скачивания в исходное состояние
     */
    private void resetDownloadUi() {
        if (downloadProgressText != null) {
            downloadProgressText.setVisibility(View.GONE);
        }
        if (downloadButton != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(downloadButton);
        }
        if (downloadButtonTop != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(downloadButtonTop);
        }
        if (btnDownloadFromMenu != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(btnDownloadFromMenu);
        }
    }

    private void restartPlayerWithNewQuality() {
        EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
        if (currentPlayerData == null) {
            return;
        }

        long currentPosition = player != null ? player.getCurrentPosition() : 0;

        autoPlayOnPrepare = player == null || player.getPlayWhenReady();

        stopCurrentPlayback();

        if ("animelib".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            handleAnimelibPlayer(currentPlayerData, currentPosition);
        } else if ("kodik".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            if (currentKodikResponse != null && currentKodikResponse.getData() != null) {
                String qualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : "1080";
                if (currentKodikResponse.getData().containsKey(qualityKey) &&
                        Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey)).length > 0) {
                    String newHlsUrl = Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey))[0].getSrc();
                    if (newHlsUrl != null && !newHlsUrl.isEmpty()) {
                        currentVideoUrl = newHlsUrl;
                        Log.d("VideoPlayer", "Updated HLS URL for quality " + preferredQuality + ": " + currentVideoUrl);
                    }
                }
            }
            handleKodikPlayer(currentPlayerData, currentPosition);
        }
        
        // Обновляем currentPlayerData в PlayersManager для правильной подсветки после смены качества
        playersManager.setCurrentPlayerData(currentPlayerData);
    }

    private void applySettingsFromDialog(SettingsBottomSheet dialog) {
        if (player != null) {
            // Apply playback speed
            float speed = dialog.getCurrentPlaybackSpeed();
            player.setPlaybackSpeed(speed);
            Log.d("VideoPlayer", "Applied playback speed: " + speed);
        }
    }

    private void showLoading(String message) {
        safeRunOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
                TextView textView = loadingOverlay.findViewById(R.id.loadingText);
                if (textView != null) {
                    textView.setText(message);
                }
            }
        });
    }

    private void hideLoading() {
        safeRunOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void showVideoErrorDialog(String title, String message, Runnable retryAction) {
        showVideoErrorDialog(title, message, retryAction, false);
    }

    private void showVideoErrorDialog(String title, String message, Runnable retryAction, boolean isVoiceoverError) {
        safeRunOnUiThread(() -> {
            if (currentErrorDialog != null && currentErrorDialog.isShowing()) {
                try {
                    currentErrorDialog.dismiss();
                } catch (Exception ignored) {}
            }

            hideLoading();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_video_error, null);
            TextView titleTv = dialogView.findViewById(R.id.errorTitleText);
            TextView messageTv = dialogView.findViewById(R.id.errorMessageText);
            TextView detailsTv = dialogView.findViewById(R.id.errorPlayerDetailsText);
            MaterialButton retryBtn = dialogView.findViewById(R.id.retryButton);
            MaterialButton switchPlayerBtn = dialogView.findViewById(R.id.switchPlayerButton);
            MaterialButton goToDownloadsBtn = dialogView.findViewById(R.id.goToDownloadsButton);
            ImageButton closeCrossBtn = dialogView.findViewById(R.id.closeErrorCrossButton);

            if (goToDownloadsBtn != null) {
                goToDownloadsBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                    com.example.animelib.ui.DownloadsActivity.start(VideoPlayerActivity.this);
                });
            }

            if (title != null && !title.isEmpty()) {
                titleTv.setText(title);
            } else {
                titleTv.setText("Ошибка загрузки видео");
            }

            messageTv.setText(message != null ? message : "Произошла ошибка при загрузке видео.");

            EpisodeResponse.PlayerData playerData = playersManager != null ? playersManager.getCurrentPlayerData() : null;
            if (playerData != null && !isVoiceoverError) {
                String pName = playerData.getPlayer() != null ? playerData.getPlayer() : "Неизвестный";
                String tName = playerData.getTeam() != null ? playerData.getTeam().getName() : "";
                String qName = preferredQuality != null ? preferredQuality : "";
                StringBuilder details = new StringBuilder("Плеер: ").append(pName);
                if (tName != null && !tName.isEmpty()) {
                    details.append(" (").append(tName).append(")");
                }
                if (qName != null && !qName.isEmpty()) {
                    details.append(" • ").append(qName);
                }
                detailsTv.setText(details.toString());
                detailsTv.setVisibility(View.VISIBLE);
            } else {
                detailsTv.setVisibility(View.GONE);
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setView(dialogView);
            currentErrorDialog = builder.create();

            if (currentErrorDialog.getWindow() != null) {
                currentErrorDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            retryBtn.setOnClickListener(v -> {
                if (currentErrorDialog != null) {
                    currentErrorDialog.dismiss();
                }
                if (retryAction != null) {
                    showLoading("Повторная попытка...");
                    retryAction.run();
                }
            });

            if (isVoiceoverError) {
                switchPlayerBtn.setText("Выйти");
                switchPlayerBtn.setIcon(null);
                switchPlayerBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                    finish();
                });
            } else {
                switchPlayerBtn.setText("Выбрать другую озвучку");
                switchPlayerBtn.setIconResource(R.drawable.ic_voice);
                switchPlayerBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                    if (playersManager != null) {
                        playersManager.showMenu();
                    }
                });
            }

            if (closeCrossBtn != null) {
                closeCrossBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                });
            }

            currentErrorDialog.show();
        });
    }

    private void initializePlayer() {
        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with custom data source, 5.1 Surround Sound processor, and 4K support
        com.example.animelib.util.SurroundRenderersFactory rf1 = new com.example.animelib.util.SurroundRenderersFactory(
                getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);

        player = new ExoPlayer.Builder(getPlayerContext(), rf1)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpDataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);
        setVideoResizeMode(currentResizeMode);
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("PlayerInit", "ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

        // Setup all player control buttons
        setupPlayerControlButtons();

        // Create media item
        MediaItem mediaItem = MediaItem.fromUri(currentVideoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();

        // Start playback
        if (autoPlayOnPrepare) {
            player.play();
        }
        autoPlayOnPrepare = true;

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("VideoPlayer", "Playback error: " + error.getMessage(), error);
                Log.e("VideoPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка воспроизведения";

                // Check if this is a 4K playback error (Source error, decoder error, etc.)
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("VideoPlayer", "4K playback failed, attempting fallback to 1080p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 1080p...";
                    CustomToast.showWarning(VideoPlayerActivity.this, errorMsg);
                    
                    // Try to fallback to 1080p
                    List<String> availableQualities = playersManager.getAvailableQualities();
                    if (availableQualities.contains("1080p")) {
                        preferredQuality = "1080p";
                        // Restart player with lower quality
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            long savedPosition = player.getCurrentPosition();
                            restartPlayerWithNewQuality();
                        }, 500);
                        return; // Don't show error toast
                    } else if (!availableQualities.isEmpty()) {
                        // Use any available quality
                        preferredQuality = availableQualities.get(0);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            restartPlayerWithNewQuality();
                        }, 500);
                        return;
                    }
                } else if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403). Пробуем без токена...";
                    Log.d("VideoPlayer", "403 Forbidden - trying URL without auth token");

                    // Try loading video without auth token
                    if (currentVideoUrl.contains("video1.cdnlibs.org/.%D0%B0s")) {
                        String urlWithoutToken = currentVideoUrl.replace("https://video1.cdnlibs.org/.%D0%B0s", "https://video1.cdnlibs.org");
                        Log.d("VideoPlayer", "Retrying with URL: " + urlWithoutToken);
                        retryWithUrl(urlWithoutToken);
                        return; // Don't show error toast yet
                    }
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": видео не найдено (404).";
                } else {
                    errorMsg += ": " + error.getMessage();
                }

                showVideoErrorDialog("Ошибка воспроизведения", errorMsg, () -> {
                    EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
                    if (cur != null) {
                        onPlayerSelected(cur);
                    } else if (currentVideoUrl != null) {
                        retryWithUrl(currentVideoUrl);
                    }
                });
            }
        });
    }

    private void retryWithUrl(String newUrl) {
        safeRunOnUiThread(() -> {
            Log.d("VideoPlayer", "Retrying playback with new URL: " + newUrl);
            currentVideoUrl = newUrl;

            // Check if this is an HLS URL (contains .m3u8 or hls in path)
            boolean isHls = newUrl.contains(".m3u8") || newUrl.contains(":hls:");

            if (isHls) {
                // For HLS, reinitialize the player with HLS support
                Log.d("VideoPlayer", "Retrying with HLS player");
                initializeHlsPlayer(newUrl);
            } else {
                // For regular video, just change the media item
                Log.d("VideoPlayer", "Retrying with regular player");
                MediaItem mediaItem = MediaItem.fromUri(newUrl);
                if (player != null) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                } else {
                    initializePlayer();
                }
            }

            CustomToast.showInfo(this, "Повторная попытка загрузки видео...");
        });
    }

    private void loadAnimeFromUrl(String url) {
        // Извлекаем anime ID из URL
        currentAnimeId = apiService.extractAnimeId(url);
        Log.d("VideoPlayer", "Extracted anime ID from URL: " + currentAnimeId);
        
        if (currentAnimeId == null) {
            CustomToast.showWarning(this, "Не удалось извлечь ID аниме из URL");
            finish();
            return;
        }
        
        // Load related titles
        loadRelatedTitles();
        
        // ВАЖНО: Сначала загружаем эпизоды, потом плееры!
        // Это позволит правильно определить эпизод из закладки
        Log.d("VideoPlayer", "Loading episodes first, then players will be loaded for correct episode");
        loadEpisodes(currentAnimeId);
    }

    private void showPlayerSelectionDialogWithAutoSelect(List<EpisodeResponse.PlayerData> players) {
        hideLoading();

        Log.d("VideoPlayer", "showPlayerSelectionDialogWithAutoSelect called with " + players.size() + " players");
        Log.d("VideoPlayer", "NOTE: setPlayersData already called by PlayersManager, only checking for auto-select");

        // Попытка автоматического выбора плеера на основе сохраненных предпочтений
        // ВАЖНО: setPlayersData уже вызван в PlayersManager.loadPlayersForEpisode()!
        executor.execute(() -> {
            com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
            
            Log.d("VideoPlayer", "Loaded preferences from DB: " + (prefs != null ? 
                  ("player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId() + ", quality=" + prefs.getPreferredQuality()) : 
                  "null"));
            
            EpisodeResponse.PlayerData matchingPlayer = null;
            
            if (prefs != null && prefs.getPlayer() != null && prefs.getTeamId() != null) {
                Log.d("VideoPlayer", "Found saved preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
                
                // Сначала ищем точное совпадение (сохраненный плеер + озвучка)
                for (EpisodeResponse.PlayerData player : players) {
                    if (player.getPlayer() != null && 
                        player.getPlayer().equals(prefs.getPlayer()) && 
                        player.getTeam() != null && 
                        player.getTeam().getId() == prefs.getTeamId()) {
                        matchingPlayer = player;
                        Log.d("VideoPlayer", "Found exact match in saved player: " + player.getPlayer() + 
                              ", team: " + player.getTeam().getName());
                        break;
                    }
                }
                
                // Если не найдено в сохраненном плеере, ищем озвучку в других плеерах
                if (matchingPlayer == null) {
                    Log.d("VideoPlayer", "Team not found in saved player, searching in other players");
                    for (EpisodeResponse.PlayerData player : players) {
                        if (player.getTeam() != null && 
                            player.getTeam().getId() == prefs.getTeamId()) {
                            matchingPlayer = player;
                            Log.d("VideoPlayer", "Found team in different player: " + player.getPlayer() + 
                                  ", team: " + player.getTeam().getName());
                            break;
                        }
                    }
                }
            } else {
                Log.d("VideoPlayer", "No saved player preferences found");
            }
            
            // Финальный результат в UI потоке
            EpisodeResponse.PlayerData finalMatchingPlayer = matchingPlayer;
            safeRunOnUiThread(() -> {
                if (finalMatchingPlayer != null) {
                    // Автоматически выбираем найденный плеер
                    Log.d("VideoPlayer", "Auto-selecting player based on preferences");
                    onPlayerSelected(finalMatchingPlayer);
                } else if (players != null && !players.isEmpty()) {
                    // Ставим первую доступную озвучку, если не сохранена последняя
                    Log.d("VideoPlayer", "No matching saved preference, auto-selecting first available player");
                    onPlayerSelected(players.get(0));
                } else {
                    Log.d("VideoPlayer", "No players available");
                }
                
                // Скрываем loading overlay в любом случае
                if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
            });
        });
    }

    private void handleAnimelibPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("AnimelibPlayer", "Handling Animelib player");
        if (playerData.getVideo() != null && playerData.getVideo().getQuality() != null && !playerData.getVideo().getQuality().isEmpty()) {
            // Select quality based on preference or best available
            EpisodeResponse.QualityData selectedQuality = null;
            String preferredQualityValue = preferredQuality != null ? preferredQuality.replace("p", "") : null;

            if (preferredQualityValue != null) {
                try {
                    int preferredQualityInt = Integer.parseInt(preferredQualityValue);
                    // Find exact quality match (include 4K if enabled)
                    for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                        if (quality.getQuality() == preferredQualityInt) {
                            // Include 4K only if enabled
                            if (preferredQualityInt == 2160 && !enable4K) {
                                continue;
                            }
                            selectedQuality = quality;
                            Log.d("AnimelibPlayer", "Using preferred quality: " + preferredQualityInt + "p");
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w("AnimelibPlayer", "Invalid preferred quality format: " + preferredQuality);
                }
            }

            // If no preferred quality found or no preference set, select best quality
            if (selectedQuality == null) {
                selectedQuality = null;
                Log.d("AnimelibPlayer", "Available qualities:");
                for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                    String q = String.valueOf(quality.getQuality());
                    Log.d("AnimelibPlayer", "  - " + quality.getQuality() + "p: " + quality.getHref());
                    // Include 4K only if enabled
                    if (enable4K || !"2160".equals(q)) {
                        if (selectedQuality == null || quality.getQuality() > selectedQuality.getQuality()) {
                            selectedQuality = quality;
                        }
                    }
                }
                // Set preferred quality to the best available
                if (preferredQuality == null && selectedQuality != null) {
                    String quality = String.valueOf(selectedQuality.getQuality());
                    preferredQuality = quality + "p";
                }
            }

            if (selectedQuality == null) {
                Log.e("AnimelibPlayer", "No suitable quality found");
                showVideoErrorDialog("Ошибка плеера AnimeLib", "Нет подходящего качества видео для этой озвучки.", () -> {
                    onPlayerSelected(playerData);
                });
                return;
            }

            String videoUrl = selectedQuality.getHref();
            Log.d("AnimelibPlayer", "Selected quality: " + selectedQuality.getQuality() + "p, URL: " + videoUrl);

            videoUrl = VideoUrlHelper.toAbsoluteVideoUrl(videoUrl);

            Log.d("AnimelibPlayer", "Final video URL: " + videoUrl);
            currentVideoUrl = videoUrl;
            
            // Set timecodes from player data
            timecodeManager.setTimecodes(playerData);
            
            initializePlayer();
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            showVideoErrorDialog("Видео недоступно", "У выбранной озвучки AnimeLib отсутствуют ссылки на видео.", () -> {
                onPlayerSelected(playerData);
            });
        }
    }

    private void handleKodikPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("KodikPlayer", "Handling Kodik player");
        
        // Set timecodes from player data
        timecodeManager.setTimecodes(playerData);
        
        if (playerData.getSrc() != null && !playerData.getSrc().isEmpty()) {
            String kodikSrc = playerData.getSrc();
            if (!kodikSrc.startsWith("http")) {
                kodikSrc = "https:" + kodikSrc;
            }
            Log.d("KodikPlayer", "Kodik src: " + kodikSrc);
            fetchKodikVideoLinks(kodikSrc, seekToPosition);
        } else {
            Log.w("KodikPlayer", "No src found in Kodik player data");
            showVideoErrorDialog("Ошибка Kodik", "Ссылка на плеер Kodik отсутствует. Попробуйте выбрать другую озвучку.", () -> {
                handleKodikPlayer(playerData, seekToPosition);
            });
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        }
    }

    private void fetchKodikVideoLinks(String kodikSrc, long seekToPosition) {
        Log.d("KodikAPI", "Fetching HLS links for Kodik src: " + kodikSrc);
        safeRunOnUiThread(() -> showLoading("Получение HLS ссылок..."));

        apiService.fetchKodikVideoLinks(kodikSrc, new ApiService.KodikVideoCallback() {
            @Override
            public void onKodikVideoReceived(KodikResponse response) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    startHlsPlayer(response, seekToPosition);
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    showVideoErrorDialog("Ошибка загрузки Kodik", "Не удалось загрузить HLS видеоссылки Kodik:\n" + error, () -> {
                        fetchKodikVideoLinks(kodikSrc, seekToPosition);
                    });
                    // Clean player state
                    if (player != null) {
                        player.stop();
                        player.clearMediaItems();
                    }
                    // Очистить возможные UI меню/загрузка
                    if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadEpisodes(String animeId) {
        Log.d("EpisodesAPI", "Loading episodes for anime_id: " + animeId);
        
        // Получаем media_slug из URL аниме для загрузки закладки
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        // Use EpisodesManager to load episodes with bookmark
        if (mediaSlug != null) {
            episodesManager.loadEpisodesWithBookmark(animeId, mediaSlug);
        } else {
            episodesManager.loadEpisodes(animeId);
        }
    }

    /**
     * Загружает первый эпизод когда нет закладки
     */
    private void loadFirstEpisode() {
        // Reset bookmark timecode when loading first episode
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for first episode");
        
        // Reset saved player position when loading first episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for first episode");
        
        // Reset auto-bookmark flag when loading first episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for first episode");
        
        // Reset bookmark button color for first episode
        updateBookmarkButtonColor(false);
        
        // Получаем первый эпизод из списка
        List<EpisodesListResponse.EpisodeItem> episodes = episodesManager.getEpisodes();
        if (episodes != null && !episodes.isEmpty()) {
            EpisodesListResponse.EpisodeItem firstEpisode = episodes.get(0);
            Log.d("VideoPlayer", "Loading first episode: " + firstEpisode.getNumber());
            
            episodesManager.setCurrentEpisode(firstEpisode);
            commentsManager.setCurrentEpisode(firstEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(firstEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No episodes available, initializing menu without auto play");
            initializeMenuWithoutAutoPlay();
        }
    }
    
    private void fallbackToUrlDetection() {
        // Reset bookmark timecode when falling back to URL detection
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for URL detection fallback");
        
        // Reset saved player position when falling back to URL detection
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for URL detection fallback");
        
        // Reset auto-bookmark flag when falling back to URL detection
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for URL detection fallback");
        
        // Reset bookmark button color for URL detection fallback
        updateBookmarkButtonColor(false);
        
        String animeUrl = getIntent().getStringExtra("anime_url");
        episodesManager.findAndSetCurrentEpisodeFromUrl(animeUrl);
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            commentsManager.setCurrentEpisode(currentEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            // Если не найден эпизод по URL, загружаем первый
            loadFirstEpisode();
        }
    }

    /**
     * Включает кнопку закладки когда плеер готов
     */
    private void enableBookmarkButton() {
        if (bookmarkButton != null) {
            bookmarkButton.setEnabled(true);
            bookmarkButton.setClickable(true);
            Log.d("VideoPlayer", "Bookmark button enabled");
        }
    }
    
    /**
     * Автоматически сохраняет закладку с текущим таймкодом
     */
    private void autoSaveBookmark() {
        // Проверяем что закладка еще не сохранена
        if (autoBookmarkSaved) {
            Log.d("VideoPlayer", "Auto-bookmark already saved, skipping");
            return;
        }
        
        Log.d("VideoPlayer", "Auto-saving bookmark on exit");
        
        // Получаем текущие данные эпизода
        EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        
        // Проверяем что данные готовы
        if (currentPlayer == null || currentEpisode == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - player or episode not ready");
            return;
        }
        
        // Получаем media_slug из URL аниме
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - media slug not available");
            return;
        }
        
        // Получаем текущее время воспроизведения и длительность
        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        long duration = player != null ? player.getDuration() : 0;
        
        // Проверяем что позиция больше 10 секунд (чтобы не сохранять случайные клики)
        if (currentPosition < 10000) {
            Log.d("VideoPlayer", "Position too small for auto-save: " + currentPosition + "ms");
            return;
        }
        
        // Проверяем что просмотрено минимум 20% эпизода
        if (duration > 0) {
            double watchedPercentage = (double) currentPosition / duration * 100;
            if (watchedPercentage < 20.0) {
                Log.d("VideoPlayer", "Watched percentage too low for auto-save: " + 
                    String.format("%.1f", watchedPercentage) + "% (minimum 20%)");
                return;
            }
            Log.d("VideoPlayer", "Watched percentage: " + String.format("%.1f", watchedPercentage) + "%");
        } else {
            Log.d("VideoPlayer", "Duration not available, using position-based check only");
        }
        
        Log.d("VideoPlayer", "Auto-saving bookmark with data:");
        Log.d("VideoPlayer", "  - mediaSlug: " + mediaSlug);
        Log.d("VideoPlayer", "  - episodeId: " + currentEpisode.getId());
        Log.d("VideoPlayer", "  - episodeNumber: " + currentEpisode.getNumber());
        Log.d("VideoPlayer", "  - teamId: " + (currentPlayer.getTeam() != null ? currentPlayer.getTeam().getId() : "null"));
        Log.d("VideoPlayer", "  - currentPosition: " + currentPosition + "ms");
        Log.d("VideoPlayer", "  - duration: " + duration + "ms");
        
        // Используем BookmarkManager для добавления закладки (без UI обновлений)
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    Log.d("VideoPlayer", "Auto-bookmark saved successfully for episode: " + episodeId);
                    autoBookmarkSaved = true; // Устанавливаем флаг что закладка сохранена
                }
                
                @Override
                public void onBookmarkError(String error) {
                    Log.e("VideoPlayer", "Failed to auto-save bookmark: " + error);
                    // Не устанавливаем флаг при ошибке, чтобы можно было повторить попытку
                }
            },
            false // Не показываем Toast при успехе для автосохранения
        );
    }
    
    /**
     * Обновляет цвет кнопки закладки
     * @param isBookmarked true если закладка добавлена, false если нет
     */
    private void updateBookmarkButtonColor(boolean isBookmarked) {
        runOnUiThread(() -> {
            if (bookmarkButton != null) {
                if (isBookmarked) {
                    bookmarkButton.setColorFilter(getResources().getColor(R.color.bookmark_color));
                } else {
                    bookmarkButton.setColorFilter(getResources().getColor(R.color.white_color));
                }
            }
            if (portraitBookmarkButton != null) {
                if (isBookmarked) {
                    portraitBookmarkButton.setColorFilter(getResources().getColor(R.color.bookmark_color));
                } else {
                    portraitBookmarkButton.setColorFilter(getResources().getColor(R.color.primary_text_color));
                }
            }
        });
    }
    
    /**
     * Обновляет список эпизодов после добавления закладки
     */
    private void updateEpisodesListAfterBookmark() {
        safeRunOnUiThread(() -> {
            // Получаем media_slug для обновления закладки
            String animeUrl = getIntent().getStringExtra("anime_url");
            String mediaSlug = null;
            if (animeUrl != null && !animeUrl.isEmpty()) {
                mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
            }
            
            if (mediaSlug != null) {
                // Обновляем закладку в EpisodesManager
                episodesManager.getBookmarkManager().fetchAnimeBookmark(mediaSlug, 
                    new BookmarkManager.AnimeBookmarkCallback() {
                        @Override
                        public void onBookmarkReceived(com.example.animelib.models.AnimeBookmarkResponse response) {
                            safeRunOnUiThread(() -> {
                                if (response != null && response.getData() != null) {
                                    // Обновляем закладку в адаптере
                                    episodesManager.updateBookmarkInAdapter(response.getData());
                                    Log.d("VideoPlayer", "Episodes list updated with new bookmark");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("VideoPlayer", "Failed to update bookmark in episodes list: " + error);
                        }
                    });
            }
        });
    }

    private void initializeMenuWithoutAutoPlay() {
        // Players are now managed by PlayersManager
        episodesManager.updateEpisodeNavigationButtonsVisibility();
        episodesManager.updateEpisodesRecyclerView(); // Call this last to ensure currentEpisode is set

        // Загружаем плееры для текущего эпизода
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            Log.d("VideoPlayer", "Loading players for episode in initializeMenuWithoutAutoPlay: " + currentEpisode.getNumber());
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No current episode available for loading players");
        }

        // PlayersManager handles auto-selection and menu display
        // Ensure loading overlay is hidden once players are available
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        if (menuLoadingIndicator != null) menuLoadingIndicator.setVisibility(View.GONE);
    }

    /**
     * Быстрое обновление заголовка с номером эпизода (синхронно, без API)
     */
    private void updateEpisodeHeaderQuick() {
        safeRunOnUiThread(() -> {
            EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
            EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
            
            String tm = (currentPlayerData != null && currentPlayerData.getTeam() != null)
                    ? currentPlayerData.getTeam().getName() : null;
            String ep = (currentEpisode != null) ? currentEpisode.getNumber() : null;
            String em = (currentEpisode != null && currentEpisode.getName() != null && !Objects.equals(currentEpisode.getName(), ""))
                    ? currentEpisode.getName() : null;

            if (currentTeamName != null) {
                if (tm != null && !tm.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentTeamName, tm);
                } else if (currentPlayerData == null) {
                    SkeletonHelper.showSkeleton(currentTeamName, 80);
                } else {
                    SkeletonHelper.hideSkeleton(currentTeamName, "");
                }
            }

            if (currentEpisodeNumberView != null) {
                if (ep != null && !ep.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, ep + " серия");
                    Log.d("VideoPlayer", "Quick header update: episode " + ep);
                } else if (currentEpisode == null) {
                    SkeletonHelper.showSkeleton(currentEpisodeNumberView, 65);
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, "");
                }
            }

            if (currentEpisodeName != null) {
                if (em != null && !em.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, ", " + em);
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, "");
                }
            }

            updatePortraitVoiceoverPlayerUI();
            updatePortraitHeaderTitlesUI();
        });
    }
    
    /**
     * Полное обновление заголовка с названием аниме (асинхронно с API)
     */
    private void updateAnimeInfoHeaderFull() {
        // Сначала быстро обновляем эпизод
        updateEpisodeHeaderQuick();
        
        // Затем асинхронно загружаем название аниме
        if (animeTitleView == null) return;
        String slugOrId = apiService.extractAnimeSlug(animeUrl);
        if (slugOrId == null) return;

        apiService.fetchAnimeInfo(slugOrId, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                safeRunOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        String rus = response.getData().getRus_name();
                        SkeletonHelper.hideSkeleton(animeTitleView, rus != null ? rus : "");
                        updatePortraitHeaderTitlesUI();
                        Log.d("VideoPlayer", "Full header update: anime title set");
                    }
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    SkeletonHelper.hideSkeleton(animeTitleView, "Аниме");
                });
                Log.w("VideoPlayer", "Failed to load anime title: " + error);
            }
        });
    }
    
    /**
     * Старый метод для обратной совместимости
     */
    private void updateAnimeInfoHeader() {
        updateAnimeInfoHeaderFull();
    }

    private void startHlsPlayer(KodikResponse kodikResponse, long seekToPosition) {
        // Save Kodik response for quality selection
        currentKodikResponse = kodikResponse;

        // Select quality based on preference or best available
        String hlsUrl = null;
        String preferredQualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : null;

        if (preferredQualityKey != null && kodikResponse.getData().containsKey(preferredQualityKey) &&
                kodikResponse.getData().get(preferredQualityKey).length > 0) {
            hlsUrl = kodikResponse.getData().get(preferredQualityKey)[0].getSrc();
            Log.d("KodikPlayer", "Using preferred quality: " + preferredQualityKey + "p");
        } else {
            // Fallback to best quality available (prefer 720p, then 480p, then 360p)
            if (kodikResponse.getData().containsKey("720") && kodikResponse.getData().get("720").length > 0) {
                hlsUrl = kodikResponse.getData().get("720")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "720p";
            } else if (kodikResponse.getData().containsKey("480") && kodikResponse.getData().get("480").length > 0) {
                hlsUrl = kodikResponse.getData().get("480")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "480p";
            } else if (kodikResponse.getData().containsKey("360") && kodikResponse.getData().get("360").length > 0) {
                hlsUrl = kodikResponse.getData().get("360")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "360p";
            }
        }

        if (hlsUrl != null) {
            // Ensure URL is absolute
            if (!hlsUrl.startsWith("http")) {
                hlsUrl = "https:" + hlsUrl;
            }

            Log.d("HlsPlayer", "Starting HLS playback with URL: " + hlsUrl);
            currentVideoUrl = hlsUrl;
            initializeHlsPlayer(hlsUrl);
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            showVideoErrorDialog("HLS видео недоступно", "HLS ссылка для выбранного качества Kodik не найдена.", () -> {
                handleKodikPlayer(playersManager.getCurrentPlayerData(), seekToPosition);
            });
        }
    }

    private void initializeHlsPlayer(String hlsUrl) {
        // Create HLS media source with OkHttp data source
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    boolean kodikHost = original.url().host().contains("kodik");
                    String referer = kodikHost ? "https://kodik.info/" : "https://v3.animelib.org/";
                    String origin = kodikHost ? "https://kodik.info" : "https://v3.animelib.org";

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                            .header("Referer", referer)
                            .header("Accept", "video/mp4,video/*,*/*")
                            .header("Accept-Encoding", "identity;q=1, *;q=0")
                            .header("Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7")
                            .header("Origin", origin)
                            .header("Sec-Fetch-Dest", "video")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "cross-site")
                            .header("Priority", "i");

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(okHttpClient);

        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(okHttpDataSourceFactory)
                .setAllowChunklessPreparation(true) // Для лучшей совместимости с 4K HLS
                .createMediaSource(MediaItem.fromUri(hlsUrl));

        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with 4K support and 5.1 Surround Sound
        com.example.animelib.util.SurroundRenderersFactory rf2 = new com.example.animelib.util.SurroundRenderersFactory(
                getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);

        player = new ExoPlayer.Builder(getPlayerContext(), rf2)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(okHttpDataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);
        setVideoResizeMode(currentResizeMode);
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("HlsPlayerInit", "HLS ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        if (autoPlayOnPrepare) {
            player.play();
        }
        autoPlayOnPrepare = true;

        // Re-setup all player control buttons for the new player
        setupPlayerControlButtons();

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("HlsPlayer", "HLS playback error: " + error.getMessage(), error);
                Log.e("HlsPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка HLS воспроизведения";

                // Check if this is a 4K playback error
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("HlsPlayer", "4K HLS playback failed, attempting fallback to 720p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 720p...";
                    CustomToast.showWarning(VideoPlayerActivity.this, errorMsg);
                    
                    // For Kodik HLS, fallback to 720p (standard Kodik quality)
                    preferredQuality = "720p";
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        restartPlayerWithNewQuality();
                    }, 500);
                    return; // Don't show error toast
                } else if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403)";
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": HLS плейлист не найден (404)";
                } else {
                    errorMsg += ": " + error.getMessage();
                }

                showVideoErrorDialog("Ошибка HLS воспроизведения", errorMsg, () -> {
                    EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
                    if (cur != null) {
                        onPlayerSelected(cur);
                    } else if (currentVideoUrl != null) {
                        initializeHlsPlayer(currentVideoUrl);
                    }
                });
            }
        });

        Log.d("HlsPlayer", "HLS player initialized and started");
    }

    private void setupPlayerControlButtons() {
        View controllerView = playerView.findViewById(R.id.exo_controller);
        if (controllerView == null) {
            Log.w("PlayerControls", "Controller view not found");
            return;
        }

        // НЕ трогаем стандартные ExoPlayer кнопки (exo_play, exo_pause)
        // ExoPlayer сам их обрабатывает автоматически

        // Настраиваем только наши кастомные кнопки
        ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

        // Setup skip forward button (1 минута 25 секунд)
        if (skipForwardButton != null) {
            skipForwardButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Skip forward clicked (1:25)");
                if (player != null && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                    long currentPosition = player.getCurrentPosition();
                    long skipDuration = longSkipDuration * 1000L; // Convert seconds to milliseconds
                    long newPosition = currentPosition + skipDuration;
                    long duration = player.getDuration();

                    // Не даем перемотать дальше конца видео
                    if (newPosition > duration && duration > 0) {
                        newPosition = duration;
                    }

                    player.seekTo(newPosition);
                    Log.d("PlayerControls", "Skipped forward to: " + (newPosition / 1000) + "s (+" + (skipDuration / 1000) + "s)");
                } else {
                    Log.w("PlayerControls", "Skip forward not available");
                }
            });
        }

        // Добавляем listener для синхронизации состояния всех кнопок
        if (player != null) {
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    Log.d("PlayerControls", "Playback state changed: " + playbackState);
                    updatePlayerControlsState();
                    episodesManager.updateEpisodeNavigationButtonsVisibility();
                    updatePlayLoadingIndicator(playbackState);

                    // Handle auto-play next episode
                    if (playbackState == Player.STATE_ENDED && autoPlay) {
                        Log.d("PlayerControls", "Video ended, checking for next episode");
                        
                        // Проверяем есть ли следующий эпизод
                        if (episodesManager != null && episodesManager.getNextEpisode() != null) {
                            // Показываем оверлей с обратным отсчетом
                            showNextEpisodeOverlay();
                        } else {
                            Log.d("PlayerControls", "No next episode available");
                        }
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d("PlayerControls", "Is playing changed: " + isPlaying);
                    updatePlayerControlsState();
                    if (isInPictureInPictureMode) {
                        updatePictureInPictureParams();
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e("PlayerControls", "Player error: " + error.getMessage());
                    updatePlayerControlsState();
                }
            });
        }

        // Инициализируем состояние кнопок
        updatePlayerControlsState();

        // Устанавливаем начальную видимость play/pause кнопок и индикатора
        updatePlayPauseAndLoadingState(false);

        // Проверяем, что ExoPlayer нашел стандартные кнопки
        ImageButton exoPlayButton = controllerView.findViewById(R.id.exo_play);
        ImageButton exoPauseButton = controllerView.findViewById(R.id.exo_pause);

        Log.d("PlayerControls", "ExoPlayer buttons found - Play: " + (exoPlayButton != null) +
                ", Pause: " + (exoPauseButton != null));

        if (exoPlayButton != null) {
            Log.d("PlayerControls", "Play button visibility: " + exoPlayButton.getVisibility() +
                    ", enabled: " + exoPlayButton.isEnabled() +
                    ", clickable: " + exoPlayButton.isClickable());

            // Добавляем обработчик с управлением видимостью
            exoPlayButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Play button clicked!");
                if (player != null) {
                    player.play();
                    updatePlayPauseAndLoadingState(true);
                }
            });
        }

        if (exoPauseButton != null) {
            Log.d("PlayerControls", "Pause button visibility: " + exoPauseButton.getVisibility() +
                    ", enabled: " + exoPauseButton.isEnabled() +
                    ", clickable: " + exoPauseButton.isClickable());

            // Добавляем обработчик с управлением видимостью
            exoPauseButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Pause button clicked!");
                if (player != null) {
                    player.pause();
                    updatePlayPauseAndLoadingState(true);
                }
            });
        }

        Log.d("PlayerControls", "Player control buttons setup completed");
    }

    private void updatePlayLoadingIndicator(int playbackState) {
        updatePlayPauseAndLoadingState(true);
    }

    private void updatePlayPauseAndLoadingState(boolean animate) {
        runOnUiThread(() -> {
            if (playerView == null) return;
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView == null) return;

            View play = controllerView.findViewById(R.id.exo_play);
            View pause = controllerView.findViewById(R.id.exo_pause);
            View spinner = controllerView.findViewById(R.id.playLoadingIndicator);

            if (play == null || pause == null || spinner == null) return;

            boolean isBuffering = (player != null) && (player.getPlaybackState() == Player.STATE_BUFFERING);
            boolean isPlaying = (player != null) && (player.isPlaying() || player.getPlayWhenReady());

            View activeView;
            if (isBuffering) {
                activeView = spinner;
            } else if (isPlaying) {
                activeView = pause;
            } else {
                activeView = play;
            }

            View[] allViews = new View[]{play, pause, spinner};

            for (View v : allViews) {
                if (v == activeView) {
                    v.animate().cancel();
                    v.animate().setListener(null);
                    if (v.getVisibility() != View.VISIBLE || v.getAlpha() < 0.95f) {
                        v.setVisibility(View.VISIBLE);
                        if (animate) {
                            v.setAlpha(0f);
                            v.setScaleX(0.75f);
                            v.setScaleY(0.75f);
                            v.animate()
                                    .alpha(1.0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(220)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .start();
                        } else {
                            v.setAlpha(1.0f);
                            v.setScaleX(1.0f);
                            v.setScaleY(1.0f);
                        }
                    }
                } else {
                    if (v.getVisibility() == View.VISIBLE && v.getAlpha() > 0.05f) {
                        v.animate().cancel();
                        if (animate) {
                            v.animate()
                                    .alpha(0.0f)
                                    .scaleX(0.75f)
                                    .scaleY(0.75f)
                                    .setDuration(160)
                                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                    .setListener(new android.animation.AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(android.animation.Animator animation) {
                                            v.setVisibility(View.GONE);
                                            v.setScaleX(1.0f);
                                            v.setScaleY(1.0f);
                                            v.setAlpha(1.0f);
                                        }
                                    })
                                    .start();
                        } else {
                            v.setVisibility(View.GONE);
                            v.setAlpha(1.0f);
                            v.setScaleX(1.0f);
                            v.setScaleY(1.0f);
                        }
                    } else if (v.getVisibility() == View.VISIBLE) {
                        v.setVisibility(View.GONE);
                        v.setAlpha(1.0f);
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                    }
                }
            }
        });
    }
    
    /**
     * Добавляет текущую серию в закладки
     */
    private void addBookmark() {
        Log.d("VideoPlayer", "Add bookmark method called");

        // Получаем текущие данные эпизода
        EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        
        // Проверяем что данные готовы
        if (currentPlayer == null) {
            Log.e("VideoPlayer", "Current player is null, cannot add bookmark");
            return;
        }
        
        if (currentEpisode == null) {
            Log.e("VideoPlayer", "Current episode is null, cannot add bookmark");
            CustomToast.showWarning(this, "Эпизод не выбран");
            return;
        }
        
        // Получаем media_slug из URL аниме
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.e("VideoPlayer", "Media slug is null, cannot add bookmark");
            CustomToast.showWarning(this, "Не удалось определить аниме");
            return;
        }

        // Получаем текущее время воспроизведения
        long currentPosition = player.getCurrentPosition();
        
        Log.d("VideoPlayer", "Adding bookmark with data:");
        Log.d("VideoPlayer", "  - mediaSlug: " + mediaSlug);
        Log.d("VideoPlayer", "  - episodeId: " + currentEpisode.getId());
        Log.d("VideoPlayer", "  - episodeNumber: " + currentEpisode.getNumber());
        Log.d("VideoPlayer", "  - teamId: " + (currentPlayer.getTeam() != null ? currentPlayer.getTeam().getId() : "null"));
        Log.d("VideoPlayer", "  - currentPosition: " + currentPosition + "ms");

        // Используем BookmarkManager для добавления закладки
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    // Меняем цвет кнопки на красный
                    updateBookmarkButtonColor(true);
                    
                    // Обновляем список эпизодов
                    updateEpisodesListAfterBookmark();
                }
                
                @Override
                public void onBookmarkError(String error) {
                    // Кнопка остается белой при ошибке
                    updateBookmarkButtonColor(false);
                }
            },
            true // Показываем Toast при успехе для ручного добавления
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null && wasPlayingBeforeBackground
                && player.getPlaybackState() != Player.STATE_ENDED) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        autoSaveBookmark();

        if (player != null) {
            wasPlayingBeforeBackground = player.getPlayWhenReady();
            player.pause();
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupFullscreen();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        setupFullscreen();
        checkAndUpdateOrientation();
    }

    private int getStatusBarHeight() {
        int sbHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                sbHeight = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
            }
        }
        if (sbHeight <= 0) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                sbHeight = getResources().getDimensionPixelSize(resourceId);
            }
        }
        return sbHeight;
    }

    private void applyRoundedCornersToTimeBar(View timeBarView) {
        if (timeBarView == null) return;
        try {
            float density = getResources().getDisplayMetrics().density;
            float radiusPx = 4f * density;
            android.graphics.CornerPathEffect pathEffect = new android.graphics.CornerPathEffect(radiusPx);

            Class<?> clazz = timeBarView.getClass();
            while (clazz != null && clazz != View.class) {
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if (android.graphics.Paint.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        android.graphics.Paint paint = (android.graphics.Paint) field.get(timeBarView);
                        if (paint != null) {
                            paint.setPathEffect(pathEffect);
                            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                timeBarView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                    }
                });
                timeBarView.setClipToOutline(true);
            }
            timeBarView.invalidate();
        } catch (Exception e) {
            Log.e("VideoPlayer", "Failed to round timebar corners", e);
        }
    }

    private void checkAndUpdateOrientation() {
        try {
            setupFullscreen();
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            int sbHeight = isPortrait ? getStatusBarHeight() : 0;

            View mainVideoLayout = findViewById(R.id.mainVideoLayout);
            if (mainVideoLayout != null) {
                mainVideoLayout.setPadding(0, sbHeight, 0, 0);
                if (mainVideoLayout instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) mainVideoLayout).setClipChildren(false);
                    ((android.view.ViewGroup) mainVideoLayout).setClipToPadding(false);
                }
            }

            // 1. Adjust player container height and show/hide bottom empty area
            View portraitBottomContainer = findViewById(R.id.portraitBottomContainer);
            if (playerContainer != null) {
                playerContainer.setPadding(0, 0, 0, 0);
                playerContainer.setClipChildren(false);
                playerContainer.setClipToPadding(false);
                ViewCompat.setElevation(playerContainer, isPortrait ? 10f : 0f);

                android.view.ViewGroup.LayoutParams rawParams = playerContainer.getLayoutParams();
                if (rawParams instanceof android.widget.LinearLayout.LayoutParams) {
                    android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) rawParams;
                    params.topMargin = 0;
                    if (isPortrait) {
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;
                        params.height = screenWidth * 9 / 16;
                        params.weight = 0;
                        if (portraitBottomContainer != null) {
                            portraitBottomContainer.setVisibility(View.VISIBLE);
                            ViewCompat.setElevation(portraitBottomContainer, 0f);
                            updatePortraitVoiceoverPlayerUI();
                            updatePortraitHeaderTitlesUI();
                            if (commentsManager != null) {
                                commentsManager.loadCommentsForPortraitIfNeeded();
                            }
                        }
                    } else {
                        params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                        params.weight = 0;
                        if (portraitBottomContainer != null) {
                            portraitBottomContainer.setVisibility(View.GONE);
                        }
                    }
                    playerContainer.setLayoutParams(params);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        playerContainer.invalidateOutline();
                    }
                }
            }

            if (playerView != null) {
                playerView.setClipChildren(false);
                playerView.setClipToPadding(false);
            }

            if (controllerView == null) {
                return;
            }

            if (controllerView instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) controllerView).setClipChildren(false);
                ((android.view.ViewGroup) controllerView).setClipToPadding(false);
            }

            // 2. Hide top anime info in portrait, keep close, settings, pip buttons visible
            View ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
            if (ibClosePlayer != null) ibClosePlayer.setVisibility(View.VISIBLE);

            View settingsButtonContainer = controllerView.findViewById(R.id.settingsButtonContainer);
            if (settingsButtonContainer != null) settingsButtonContainer.setVisibility(View.VISIBLE);
            if (settingsButton != null) settingsButton.setVisibility(View.VISIBLE);

            if (pipButton == null) pipButton = controllerView.findViewById(R.id.pipButton);
            if (pipButton != null) pipButton.setVisibility(View.VISIBLE);

            View animeInfoContainer = controllerView.findViewById(R.id.animeInfoContainer);
            if (animeInfoContainer != null) {
                animeInfoContainer.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            }

            View topBarContainer = controllerView.findViewById(R.id.topBarContainer);
            if (topBarContainer != null) {
                android.view.ViewGroup.LayoutParams lp = topBarContainer.getLayoutParams();
                if (lp instanceof android.widget.FrameLayout.LayoutParams) {
                    android.widget.FrameLayout.LayoutParams topParams = (android.widget.FrameLayout.LayoutParams) lp;
                    float density = getResources().getDisplayMetrics().density;
                    if (isPortrait) {
                        topParams.topMargin = (int) (10 * density);
                        topParams.leftMargin = (int) (10 * density);
                        topParams.rightMargin = (int) (10 * density);
                    } else {
                        topParams.topMargin = (int) (16 * density);
                        topParams.leftMargin = (int) (24 * density);
                        topParams.rightMargin = (int) (24 * density);
                    }
                    topBarContainer.setLayoutParams(topParams);
                }
            }

            // 3. Show center episode & play controls in both portrait and landscape
            View centerControlsContainer = controllerView.findViewById(R.id.centerControlsContainer);
            if (centerControlsContainer != null) {
                centerControlsContainer.setVisibility(View.VISIBLE);
            }

            // 4. Hide / show bottom buttons in portrait
            View skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);
            View bookmarkBtn = controllerView.findViewById(R.id.bookmarkButton);
            View commentsBtn = controllerView.findViewById(R.id.commentsButton);
            View episodesMenuBtn = controllerView.findViewById(R.id.episodesMenuButton);
            View menuToggleBtn = controllerView.findViewById(R.id.menuToggleButton);
            View menuToggleFullscreenBtn = controllerView.findViewById(R.id.menuToggleFullscreen);

            if (skipForwardButton != null) skipForwardButton.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (bookmarkBtn != null) bookmarkBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (commentsBtn != null) commentsBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (episodesMenuBtn != null) episodesMenuBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (menuToggleBtn != null) menuToggleBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (menuToggleFullscreenBtn != null) {
                menuToggleFullscreenBtn.setVisibility(View.VISIBLE);
                if (menuToggleFullscreenBtn instanceof ImageButton) {
                    ((ImageButton) menuToggleFullscreenBtn).setImageResource(isPortrait ? R.drawable.ic_fullscreen : R.drawable.ic_fullscreen_exit);
                }
            }
            if (episodesHorizontalRecyclerView != null) {
                if (isPortrait) {
                    episodesHorizontalRecyclerView.setVisibility(View.GONE);
                } else {
                    if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                        episodesHorizontalRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        episodesHorizontalRecyclerView.setVisibility(View.INVISIBLE);
                    }
                }
            }

            // 5. Adjust timebar position & margin
            android.widget.LinearLayout playersControlBarView = controllerView.findViewById(R.id.playersControlBar);
            View exoProgress = controllerView.findViewById(R.id.exo_progress);
            View timeAndControlsContainer = controllerView.findViewById(R.id.timeAndControlsContainer);

            if (playersControlBarView != null && exoProgress != null && timeAndControlsContainer != null) {
                playersControlBarView.setClipChildren(false);
                playersControlBarView.setClipToPadding(false);

                if (exoProgress.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) exoProgress.getParent()).removeView(exoProgress);
                }
                if (timeAndControlsContainer.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) timeAndControlsContainer.getParent()).removeView(timeAndControlsContainer);
                }
                if (episodesHorizontalRecyclerView != null && episodesHorizontalRecyclerView.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) episodesHorizontalRecyclerView.getParent()).removeView(episodesHorizontalRecyclerView);
                }

                android.view.ViewGroup.LayoutParams tcLp = timeAndControlsContainer.getLayoutParams();
                android.widget.LinearLayout.LayoutParams timeAndControlsParams = (tcLp instanceof android.widget.LinearLayout.LayoutParams) ?
                        (android.widget.LinearLayout.LayoutParams) tcLp :
                        new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

                android.view.ViewGroup.LayoutParams exLp = exoProgress.getLayoutParams();
                android.widget.LinearLayout.LayoutParams progressParams = (exLp instanceof android.widget.LinearLayout.LayoutParams) ?
                        (android.widget.LinearLayout.LayoutParams) exLp :
                        new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

                float density = getResources().getDisplayMetrics().density;

                if (isPortrait) {
                    // Reset translationY to 0 so timebar and timecode are fully visible
                    playersControlBarView.setTranslationY(0f);

                    // Align timeAndControls (16dp) with exoProgress (8dp + 8dp internal padding = 16dp)
                    timeAndControlsParams.topMargin = (int) (2 * density);
                    timeAndControlsParams.bottomMargin = (int) (2 * density);
                    timeAndControlsParams.leftMargin = (int) (16 * density);
                    timeAndControlsParams.rightMargin = (int) (16 * density);

                    progressParams.topMargin = (int) (2 * density);
                    progressParams.bottomMargin = (int) (12 * density);
                    progressParams.leftMargin = (int) (8 * density);
                    progressParams.rightMargin = (int) (8 * density);

                    timeAndControlsContainer.setLayoutParams(timeAndControlsParams);
                    exoProgress.setLayoutParams(progressParams);

                    exoProgress.setTranslationY(0f);
                    applyRoundedCornersToTimeBar(exoProgress);

                    playersControlBarView.addView(timeAndControlsContainer);
                    playersControlBarView.addView(exoProgress);
                    if (episodesHorizontalRecyclerView != null) {
                        playersControlBarView.addView(episodesHorizontalRecyclerView);
                    }
                } else {
                    // Landscape: timeAndControlsContainer on top, exoProgress at bottom (same order as portrait)
                    if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                        playersControlBarView.setTranslationY(0f);
                    } else {
                        playersControlBarView.setTranslationY(60f * density);
                    }

                    // Align timeAndControls (24dp) with exoProgress (16dp + 8dp internal padding = 24dp)
                    timeAndControlsParams.topMargin = (int) (2 * density);
                    timeAndControlsParams.bottomMargin = (int) (2 * density);
                    timeAndControlsParams.leftMargin = (int) (24 * density);
                    timeAndControlsParams.rightMargin = (int) (24 * density);

                    progressParams.topMargin = (int) (2 * density);
                    progressParams.bottomMargin = (int) (14 * density);
                    progressParams.leftMargin = (int) (16 * density);
                    progressParams.rightMargin = (int) (16 * density);

                    timeAndControlsContainer.setLayoutParams(timeAndControlsParams);
                    exoProgress.setLayoutParams(progressParams);

                    exoProgress.setTranslationY(0f);
                    applyRoundedCornersToTimeBar(exoProgress);

                    playersControlBarView.addView(timeAndControlsContainer);
                    playersControlBarView.addView(exoProgress);
                    if (episodesHorizontalRecyclerView != null) {
                        playersControlBarView.addView(episodesHorizontalRecyclerView);
                    }
                }

                playersControlBarView.setVisibility(View.VISIBLE);
                exoProgress.setVisibility(View.VISIBLE);
                timeAndControlsContainer.setVisibility(View.VISIBLE);

                View timeCodeContainer = controllerView.findViewById(R.id.timeCodeContainer);
                if (timeCodeContainer != null) timeCodeContainer.setVisibility(View.VISIBLE);
            }

            // 6. Block drag events on side panels, top curtain, episode carousel (Task 3)
            if (menuPanelContainer != null) menuPanelContainer.setDragEnabled(!isPortrait);
            if (commentsPanelContainer != null) commentsPanelContainer.setDragEnabled(!isPortrait);
            if (gesturesManager != null) gesturesManager.setPortraitMode(isPortrait);
            if (verticalGesturesManager != null) verticalGesturesManager.setPortraitMode(isPortrait);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error in checkAndUpdateOrientation", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupFullscreen();

        // Восстанавливаем плашку прогресса при возврате в плеер
        if (DownloadService.isRunning()) {
            showDownloadProgress(DownloadService.getCurrentTaskPercent());
        } else {
            resetDownloadUi();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Автоматически сохраняем закладку при закрытии плеера
        autoSaveBookmark();
        
        // Clear screen keep flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (currentErrorDialog != null && currentErrorDialog.isShowing()) {
            try {
                currentErrorDialog.dismiss();
            } catch (Exception ignored) {}
            currentErrorDialog = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (apiService != null) {
            apiService.shutdown();
        }
        if (commentsManager != null) {
            commentsManager.cleanup();
        }
        if (playersManager != null) {
            playersManager.cleanup();
        }
        if (gesturesManager != null) {
            gesturesManager.cleanup();
        }
        if (timecodeManager != null) {
            timecodeManager.cleanup();
        }
        if (ambientLightManager != null) {
            ambientLightManager.cleanup();
        }
        if (surroundSoundManager != null) {
            surroundSoundManager.release();
            surroundSoundManager = null;
        }
        if (pipBroadcastReceiver != null) {
            try {
                unregisterReceiver(pipBroadcastReceiver);
            } catch (Exception ignored) {}
            pipBroadcastReceiver = null;
        }

        DownloadService.setListener(null);
        if (btnDownloadFromMenu != null) com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(btnDownloadFromMenu);

        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            autoSaveBookmark();
            enterPictureInPictureMode();
            return;
        }
        autoSaveBookmark();

        super.onBackPressed();
    }
}


