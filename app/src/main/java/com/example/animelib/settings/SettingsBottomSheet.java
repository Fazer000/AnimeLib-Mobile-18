package com.example.animelib.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.animelib.R;
import com.example.animelib.UpdateActivity;
import com.example.animelib.adapters.QualityAdapter;
import com.example.animelib.models.UpdateInfo;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.util.UpdateManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
//import com.google.android.material.bottomsheet.BottomSheetDialogThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.materialswitch.MaterialSwitch;

import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.view.ViewGroup;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class SettingsBottomSheet extends BottomSheetDialog {
    private final List<String> qualities;
    private String currentQuality;
    private final QualityAdapter.OnQualitySelectedListener listener;
    private QualityAdapter qualityAdapter;
    private QualityBottomSheet currentQualityBottomSheet;
    private SpeedBottomSheet currentSpeedBottomSheet;
    private float currentPlaybackSpeed = 1.0f;
    private final SpeedBottomSheet.OnSpeedChangedListener speedListener;
    private boolean enable4K = false;
    private final On4KToggledListener on4KToggledListener;
    private boolean enableAmbientLight = false;
    private final OnAmbientLightToggledListener onAmbientLightToggledListener;
    private boolean enableSurroundSound = true;
    private int surroundMode = 0;
    private float surroundSpatialWidth = 1.0f;
    private float surroundDialogueBoost = 1.0f;
    private float surroundBassBoost = 1.0f;
    private float surroundTrebleBoost = 1.0f;
    private OnSurroundSoundToggledListener onSurroundSoundToggledListener;
    private OnSurround3DChangedListener onSurround3DChangedListener;
    private boolean autoPlay = true;
    private final OnAutoPlayToggledListener onAutoPlayToggledListener;
    private int longSkipDuration = 85; // seconds
    private final OnSkipDurationChangedListener onSkipDurationChangedListener;
    private int currentTheme = ThemeUtils.THEME_SYSTEM;
    private final OnThemeChangedListener onThemeChangedListener;
    private int currentResizeMode = 0; // RESIZE_MODE_FIT
    private OnResizeModeChangedListener onResizeModeChangedListener;
    private boolean isOfflineMode = false;

    public interface OnResizeModeChangedListener {
        void onResizeModeChanged(int resizeMode);
    }

    public void setResizeMode(int resizeMode, OnResizeModeChangedListener listener) {
        this.currentResizeMode = resizeMode;
        this.onResizeModeChangedListener = listener;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.isOfflineMode = offlineMode;
    }

    public SettingsBottomSheet(Context context,
                               List<String> qualities,
                               String currentQuality,
                               QualityAdapter.OnQualitySelectedListener listener,
                               float initialSpeed,
                               SpeedBottomSheet.OnSpeedChangedListener speedListener,
                               boolean enable4K,
                               On4KToggledListener on4KToggledListener,
                               boolean enableAmbientLight,
                               OnAmbientLightToggledListener onAmbientLightToggledListener,
                               boolean autoPlay,
                               OnAutoPlayToggledListener onAutoPlayToggledListener,
                               int longSkipDuration,
                               OnSkipDurationChangedListener onSkipDurationChangedListener,
                               int currentTheme,
                               OnThemeChangedListener onThemeChangedListener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.qualities = qualities;
        this.currentQuality = currentQuality;
        this.listener = listener;
        this.currentPlaybackSpeed = initialSpeed;
        this.speedListener = speedListener;
        this.enable4K = enable4K;
        this.on4KToggledListener = on4KToggledListener;
        this.enableAmbientLight = enableAmbientLight;
        this.onAmbientLightToggledListener = onAmbientLightToggledListener;
        this.autoPlay = autoPlay;
        this.onAutoPlayToggledListener = onAutoPlayToggledListener;
        this.longSkipDuration = longSkipDuration;
        this.onSkipDurationChangedListener = onSkipDurationChangedListener;
        this.currentTheme = currentTheme;
        this.onThemeChangedListener = onThemeChangedListener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_settings, null);
        setContentView(view);
        com.example.animelib.util.FloatingBottomSheetUtils.setupFloatingStyle(this);

        // Setup option click listeners
        LinearLayout qualityOption = view.findViewById(R.id.qualityOption);
        LinearLayout speedOption = view.findViewById(R.id.speedOption);
        LinearLayout fourKOption = view.findViewById(R.id.fourKOption);
        LinearLayout ambientLightOption = view.findViewById(R.id.ambientLightOption);
        LinearLayout surroundSoundOption = view.findViewById(R.id.surroundSoundOption);
        LinearLayout autoPlayOption = view.findViewById(R.id.autoPlayOption);
        LinearLayout skipDurationOption = view.findViewById(R.id.skipDurationOption);
        LinearLayout themeOption = view.findViewById(R.id.themeOption);
        LinearLayout resizeModeOption = view.findViewById(R.id.resizeModeOption);

        TextView currentQualityText = view.findViewById(R.id.currentQualityText);
        TextView currentSpeedText = view.findViewById(R.id.currentSpeedText);
        TextView currentSkipDurationText = view.findViewById(R.id.currentSkipDurationText);
        TextView currentThemeText = view.findViewById(R.id.currentThemeText);
        TextView currentResizeModeText = view.findViewById(R.id.currentResizeModeText);
        MaterialSwitch fourKSwitch = view.findViewById(R.id.fourKSwitch);
        MaterialSwitch ambientLightSwitch = view.findViewById(R.id.ambientLightSwitch);
        MaterialSwitch surroundSoundSwitch = view.findViewById(R.id.surroundSoundSwitch);
        MaterialSwitch autoPlaySwitch = view.findViewById(R.id.autoPlaySwitch);

        ImageView ivTheme = findViewById(R.id.ivTheme);
        ImageView exitBtn = view.findViewById(R.id.bs_exit);

        // Set current values
        updateQualityViews(currentQuality);
        currentSpeedText.setText(String.format("%.1fx", currentPlaybackSpeed));
        fourKSwitch.setChecked(enable4K);
        ambientLightSwitch.setChecked(enableAmbientLight);
        if (surroundSoundSwitch != null) {
            surroundSoundSwitch.setChecked(enableSurroundSound);
        }
        autoPlaySwitch.setChecked(autoPlay);
        currentSkipDurationText.setText(formatDuration(longSkipDuration));
        currentThemeText.setText(getThemeText(currentTheme));
        if (currentResizeModeText != null) {
            currentResizeModeText.setText(ResizeModeBottomSheet.getResizeModeText(currentResizeMode));
        }

        if (ivTheme != null) {
            int iconResId;
            float scale;
            switch (currentTheme) {
                case ThemeUtils.THEME_SYSTEM:
                    iconResId = R.drawable.ic_auto; // Иконка для автотемы
                    scale = 0.9F;
                    break;
                case ThemeUtils.THEME_LIGHT:
                    iconResId = R.drawable.ic_light; // Иконка для светлой темы
                    scale = 1F;
                    break;
                case ThemeUtils.THEME_DARK:
                    iconResId = R.drawable.ic_night; // Иконка для темной темы (исправлено написание)
                    scale = 0.8F;
                    break;
                default:
                    iconResId = R.drawable.ic_auto; // Иконка по умолчанию
                    scale = 0.9F;
                    break;
            }
            ivTheme.setImageResource(iconResId);
            ivTheme.setScaleX(scale);
            ivTheme.setScaleY(scale);
        }

        exitBtn.setOnClickListener(v -> dismiss());

        // Quality option click
        if (isOfflineMode) {
            qualityOption.setAlpha(0.5f);
            qualityOption.setOnClickListener(v -> {
                android.widget.Toast.makeText(getContext(), "Смена качества недоступна для скачанного видео", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            qualityOption.setOnClickListener(v -> {
                dismiss();
                showQualityDialog();
            });
        }

        // 4K option click
        fourKOption.setOnClickListener(v -> fourKSwitch.setChecked(!fourKSwitch.isChecked()));

        // 4K switch listener
        fourKSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enable4K = isChecked;
            if (on4KToggledListener != null) {
                on4KToggledListener.on4KToggled(isChecked);
            }
        });

        // Ambient light option click
        ambientLightOption.setOnClickListener(v -> ambientLightSwitch.setChecked(!ambientLightSwitch.isChecked()));

        // Ambient light switch listener
        ambientLightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableAmbientLight = isChecked;
            if (onAmbientLightToggledListener != null) {
                onAmbientLightToggledListener.onAmbientLightToggled(isChecked);
            }
        });

        // Surround sound option click (opens 3D Surround Sound settings sheet)
        if (surroundSoundOption != null && surroundSoundSwitch != null) {
            surroundSoundOption.setOnClickListener(v -> {
                dismiss();
                showSurround3DDialog();
            });
            surroundSoundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                enableSurroundSound = isChecked;
                updateSurroundSubtitle();
                if (onSurroundSoundToggledListener != null) {
                    onSurroundSoundToggledListener.onSurroundSoundToggled(isChecked);
                }
                if (onSurround3DChangedListener != null) {
                    onSurround3DChangedListener.onSurround3DChanged(isChecked, surroundMode, surroundSpatialWidth, surroundDialogueBoost, surroundBassBoost, surroundTrebleBoost);
                }
            });
        }
        updateSurroundSubtitle();

        // AutoPlay option click
        autoPlayOption.setOnClickListener(v -> autoPlaySwitch.setChecked(!autoPlaySwitch.isChecked()));

        // AutoPlay switch listener
        autoPlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoPlay = isChecked;
            if (onAutoPlayToggledListener != null) {
                onAutoPlayToggledListener.onAutoPlayToggled(isChecked);
            }
        });

        // Skip duration option click
        skipDurationOption.setOnClickListener(v -> {
            dismiss();
            showSkipDurationDialog();
        });

        // Theme option click
        themeOption.setOnClickListener(v -> {
            dismiss();
            showThemeDialog();
        });

        // Resize mode option click
        if (resizeModeOption != null) {
            resizeModeOption.setOnClickListener(v -> {
                dismiss();
                showResizeModeDialog();
            });
        }

        // Speed option click
        speedOption.setOnClickListener(v -> {
            dismiss();
            showSpeedDialog();
        });

        // Check for updates option
        LinearLayout checkForUpdatesOption = view.findViewById(R.id.checkForUpdatesOption);
        ProgressBar updateProgressBar = view.findViewById(R.id.updateProgressBar);
        ImageView ivUpdateArrow = view.findViewById(R.id.ivUpdateArrow);
        TextView appVersionText = view.findViewById(R.id.appVersionText);

        String currentVersion = UpdateManager.getCurrentVersion(getContext());
        if (appVersionText != null) {
            appVersionText.setText("Версия v" + currentVersion);
        }

        if (checkForUpdatesOption != null) {
            checkForUpdatesOption.setOnClickListener(v -> {
                if (updateProgressBar != null) updateProgressBar.setVisibility(View.VISIBLE);
                if (ivUpdateArrow != null) ivUpdateArrow.setVisibility(View.GONE);
                checkForUpdatesOption.setEnabled(false);

                UpdateManager.checkForUpdates(getContext(), null, new UpdateManager.CheckUpdateCallback() {
                    @Override
                    public void onUpdateCheckResult(boolean hasUpdate, UpdateInfo updateInfo, String currentVer) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                if (updateProgressBar != null) updateProgressBar.setVisibility(View.GONE);
                                if (ivUpdateArrow != null) ivUpdateArrow.setVisibility(View.VISIBLE);
                                checkForUpdatesOption.setEnabled(true);

                                if (hasUpdate && updateInfo != null) {
                                    dismiss();
                                    UpdateActivity.start(getContext(), updateInfo);
                                } else {
                                    CustomToast.showInfo(getContext(), "У вас установлена последняя версия (v" + currentVer + ")");
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                if (updateProgressBar != null) updateProgressBar.setVisibility(View.GONE);
                                if (ivUpdateArrow != null) ivUpdateArrow.setVisibility(View.VISIBLE);
                                checkForUpdatesOption.setEnabled(true);
                                CustomToast.showWarning(getContext(), errorMessage);
                            });
                        }
                    }
                });
            });
        }

        setCancelable(true);
    }

    @Nullable
    private Activity getActivity() {
        Activity activity = null;
        Context context = getContext();
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextWrapper) {
            Context baseContext = ((ContextWrapper) context).getBaseContext();
            if (baseContext instanceof Activity) {
                activity = (Activity) baseContext;
            }
        }
        return activity;
    }

    private void showQualityDialog() {
        // Create dialog only if it doesn't exist
        if (currentQualityBottomSheet == null) {
            currentQualityBottomSheet = new QualityBottomSheet(getContext(), new ArrayList<>(qualities), currentQuality, quality -> {
                if (listener != null) {
                    listener.onQualitySelected(quality);
                }
                // Update current quality and refresh UI
                currentQuality = quality;
                updateQualityViews(quality);
                // Update the quality dialog itself
                if (currentQualityBottomSheet != null) {
                    currentQualityBottomSheet.updateCurrentQuality(quality);
                }
            });

            // Set up back button listener
            // Show main settings when back button is pressed
            currentQualityBottomSheet.setOnBackPressedListener(this::show);
        } else {
            // Update existing dialog with current data
            currentQualityBottomSheet.updateCurrentQuality(currentQuality);
        }

        currentQualityBottomSheet.show();
    }

    private void showSpeedDialog() {
        if (currentSpeedBottomSheet == null) {
            currentSpeedBottomSheet = new SpeedBottomSheet(getContext(), currentPlaybackSpeed, speed -> {
                currentPlaybackSpeed = speed;
                TextView currentSpeedText = findViewById(R.id.currentSpeedText);
                if (currentSpeedText != null) {
                    currentSpeedText.setText(String.format(java.util.Locale.US, "%.1fx", speed));
                }
                if (speedListener != null) {
                    speedListener.onSpeedChanged(speed);
                }
            });
            currentSpeedBottomSheet.setOnBackPressedListener(this::show);
        } else {
            currentSpeedBottomSheet.updateCurrentSpeed(currentPlaybackSpeed);
        }

        currentSpeedBottomSheet.show();
    }

    private void showSkipDurationDialog() {
        SkipDurationBottomSheet dialog = new SkipDurationBottomSheet(getContext(), longSkipDuration, duration -> {
            longSkipDuration = duration;
            // Update UI in settings
            TextView currentSkipDurationText = findViewById(R.id.currentSkipDurationText);
            if (currentSkipDurationText != null) {
                currentSkipDurationText.setText(formatDuration(duration));
            }
            // Propagate to owner immediately
            if (onSkipDurationChangedListener != null) {
                onSkipDurationChangedListener.onSkipDurationChanged(duration);
            }
        });

        // Set up back button listener
        // Show main settings when back button is pressed
        dialog.setOnBackPressedListener(this::show);

        dialog.show();
    }

    @SuppressLint("DefaultLocale")
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    public float getCurrentPlaybackSpeed() {
        return currentPlaybackSpeed;
    }

    private void updateQualityViews(String quality) {
        TextView currentQualityText = findViewById(R.id.currentQualityText);
        TextView currentQualityTagText = findViewById(R.id.currentQualityTagText);
        if (currentQualityText == null) return;

        if (isOfflineMode) {
            currentQualityText.setText("Офлайн");
            if (currentQualityTagText != null) {
                currentQualityTagText.setVisibility(View.GONE);
            }
            return;
        }

        if (quality == null) {
            quality = "1080p";
        }

        String tag = com.example.animelib.util.FloatingBottomSheetUtils.getQualityTag(quality);
        currentQualityText.setText(quality);

        if (currentQualityTagText != null) {
            if (tag != null && !tag.isEmpty() && !quality.equalsIgnoreCase(tag)) {
                currentQualityTagText.setText(tag);
                currentQualityTagText.setVisibility(View.VISIBLE);
            } else {
                currentQualityTagText.setVisibility(View.GONE);
            }
        }
    }

    private String formatQualityWithTag(String quality) {
        if (isOfflineMode) {
            return "Офлайн";
        }
        if (quality == null) {
            quality = "1080p";
        }
        String tag = com.example.animelib.util.FloatingBottomSheetUtils.getQualityTag(quality);
        if (tag != null && !tag.isEmpty() && !quality.equalsIgnoreCase(tag)) {
            return quality + " (" + tag + ")";
        }
        return quality;
    }

    public void updateQualities(List<String> newQualities, String newCurrentQuality) {
        android.util.Log.d("SettingsDialog", "updateQualities called - newQualities: " + newQualities + ", newCurrentQuality: " + newCurrentQuality);
        this.qualities.clear();
        this.qualities.addAll(newQualities);
        this.currentQuality = newCurrentQuality;

        // Update UI
        updateQualityViews(newCurrentQuality);

        // Update quality dialog if it exists - обновляем весь список, а не только текущее качество
        if (currentQualityBottomSheet != null) {
            currentQualityBottomSheet.updateQualities(newQualities, newCurrentQuality);
        }
    }

    public interface On4KToggledListener {
        void on4KToggled(boolean enabled);
    }

    public interface OnAmbientLightToggledListener {
        void onAmbientLightToggled(boolean enabled);
    }

    public interface OnSurroundSoundToggledListener {
        void onSurroundSoundToggled(boolean enabled);
    }

    public interface OnSurround3DChangedListener {
        void onSurround3DChanged(boolean enabled, int mode, float spatialWidth, float dialogueBoost, float bassBoost, float trebleBoost);
    }

    public void setSurroundSound(boolean enableSurroundSound, OnSurroundSoundToggledListener listener) {
        this.enableSurroundSound = enableSurroundSound;
        this.onSurroundSoundToggledListener = listener;
        MaterialSwitch surroundSoundSwitch = findViewById(R.id.surroundSoundSwitch);
        if (surroundSoundSwitch != null) {
            surroundSoundSwitch.setChecked(enableSurroundSound);
        }
        updateSurroundSubtitle();
    }

    public void setSurround3DSettings(boolean enableSurroundSound, int mode, float spatialWidth, float dialogueBoost, float bassBoost, float trebleBoost, OnSurround3DChangedListener listener) {
        this.enableSurroundSound = enableSurroundSound;
        this.surroundMode = mode;
        this.surroundSpatialWidth = spatialWidth;
        this.surroundDialogueBoost = dialogueBoost;
        this.surroundBassBoost = bassBoost;
        this.surroundTrebleBoost = trebleBoost;
        this.onSurround3DChangedListener = listener;

        MaterialSwitch surroundSoundSwitch = findViewById(R.id.surroundSoundSwitch);
        if (surroundSoundSwitch != null) {
            surroundSoundSwitch.setChecked(enableSurroundSound);
        }
        updateSurroundSubtitle();
    }

    private void showSurround3DDialog() {
        SurroundSoundBottomSheet dialog = new SurroundSoundBottomSheet(
                getContext(),
                enableSurroundSound,
                surroundMode,
                surroundSpatialWidth,
                surroundDialogueBoost,
                surroundBassBoost,
                surroundTrebleBoost,
                (enabled, mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost) -> {
                    this.enableSurroundSound = enabled;
                    this.surroundMode = mode;
                    this.surroundSpatialWidth = spatialWidth;
                    this.surroundDialogueBoost = dialogueBoost;
                    this.surroundBassBoost = bassBoost;
                    this.surroundTrebleBoost = trebleBoost;

                    MaterialSwitch surroundSoundSwitch = findViewById(R.id.surroundSoundSwitch);
                    if (surroundSoundSwitch != null) {
                        surroundSoundSwitch.setChecked(enabled);
                    }
                    updateSurroundSubtitle();

                    if (onSurroundSoundToggledListener != null) {
                        onSurroundSoundToggledListener.onSurroundSoundToggled(enabled);
                    }
                    if (onSurround3DChangedListener != null) {
                        onSurround3DChangedListener.onSurround3DChanged(enabled, mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost);
                    }
                }
        );

        dialog.setOnBackPressedListener(this::show);
        dialog.show();
    }

    private void updateSurroundSubtitle() {
        TextView currentSurroundSoundText = findViewById(R.id.currentSurroundSoundText);
        if (currentSurroundSoundText != null) {
            if (!enableSurroundSound) {
                currentSurroundSoundText.setText("Выключено");
            } else {
                switch (surroundMode) {
                    case 0:
                        currentSurroundSoundText.setText("🎬 Кинотеатр 3D");
                        break;
                    case 1:
                        currentSurroundSoundText.setText("🎧 Концерт & OST 3D");
                        break;
                    case 2:
                        currentSurroundSoundText.setText("🎙️ Четкая Озвучка 3D");
                        break;
                    case 3:
                        currentSurroundSoundText.setText("🔊 Классический 5.1");
                        break;
                    case 4:
                        currentSurroundSoundText.setText("🌌 Экстрим 3D HRTF");
                        break;
                    default:
                        currentSurroundSoundText.setText("Объемный звук");
                        break;
                }
            }
        }
    }

    public interface OnAutoPlayToggledListener {
        void onAutoPlayToggled(boolean enabled);
    }

    public interface OnSkipDurationChangedListener {
        void onSkipDurationChanged(int durationInSeconds);
    }

    public interface OnThemeChangedListener {
        void onThemeChanged(int themeMode);
    }

    private void showThemeDialog() {
        ThemeSelectionBottomSheet bottomSheet = new ThemeSelectionBottomSheet(getContext(), currentTheme, themeMode -> {
            currentTheme = themeMode;
            // Update UI in settings
            TextView currentThemeText = findViewById(R.id.currentThemeText);

            if (currentThemeText != null) {
                currentThemeText.setText(getThemeText(themeMode));
            }

            // Propagate to owner
            if (onThemeChangedListener != null) {
                onThemeChangedListener.onThemeChanged(themeMode);
            }
        });

        // Set up back button listener
        // Show main settings when back button is pressed
        bottomSheet.setOnBackPressedListener(this::show);

        bottomSheet.show();
    }

    private void showResizeModeDialog() {
        ResizeModeBottomSheet bottomSheet = new ResizeModeBottomSheet(getContext(), currentResizeMode, resizeMode -> {
            currentResizeMode = resizeMode;
            TextView currentResizeModeText = findViewById(R.id.currentResizeModeText);
            if (currentResizeModeText != null) {
                currentResizeModeText.setText(ResizeModeBottomSheet.getResizeModeText(resizeMode));
            }
            if (onResizeModeChangedListener != null) {
                onResizeModeChangedListener.onResizeModeChanged(resizeMode);
            }
        });
        bottomSheet.setOnBackPressedListener(this::show);
        bottomSheet.show();
    }

    private String getThemeText(int themeMode) {
        switch (themeMode) {
            case ThemeUtils.THEME_SYSTEM:
                return "Авто";
            case ThemeUtils.THEME_LIGHT:
                return "Светлая";
            case ThemeUtils.THEME_DARK:
                return "Темная";
            default:
                return "Авто";
        }
    }
}
