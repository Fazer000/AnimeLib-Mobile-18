package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.adapters.PlayerOptionsAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.models.EpisodeResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Менеджер для управления плеерами и озвучками
 */
public class PlayersManager {
    private static final String TAG = "PlayersManager";
    
    // Контекст и зависимости
    private final Context context;
    private final ApiService apiService;
    
    // UI компоненты
    private LinearLayout slidingMenuPanel;
    private ImageButton closeMenuButton;
    private View menuOverlay;
    private View menuLoadingOverlay;
    private View menuLoadingIndicator;
    private com.example.animelib.ui.VoiceoverBottomSheet voiceoverBottomSheet;
    private boolean isLoading = false;

    // Side panel UI
    private TabLayout tabLayout;
    private RecyclerView rvVoiceovers;
    private PlayerOptionsAdapter sidePanelAdapter;
    private String sidePanelActiveTab = "animelib";
    private boolean isProgrammaticSideTabSelect = false;
    
    // Состояние меню
    private boolean isMenuVisible = false;
    private float menuWidth;
    
    // Данные плееров
    private List<EpisodeResponse.PlayerData> allPlayers = new ArrayList<>();
    private List<EpisodeResponse.PlayerData> animelibPlayers = new ArrayList<>();
    private List<EpisodeResponse.PlayerData> kodikPlayers = new ArrayList<>();
    private EpisodeResponse.PlayerData currentPlayerData;
    
    // Предпочтения пользователя
    private String preferredPlayerType; // "animelib" or "kodik"
    private EpisodeResponse.PlayerData preferredAnimelibPlayer;
    private EpisodeResponse.PlayerData preferredKodikPlayer;
    private boolean enable4K = false; // Настройка 4K
    
    // Callback интерфейсы
    public interface PlayerSelectionCallback {
        void onPlayerSelected(EpisodeResponse.PlayerData playerData);
    }
    
    public interface PlayersVisibilityCallback {
        void onPlayersVisibilityChanged(boolean isVisible);
    }
    
    public interface PlayersDataCallback {
        void onPlayersLoaded(List<EpisodeResponse.PlayerData> players);
        void onPlayersError(String error);
    }
    
    private PlayerSelectionCallback playerSelectionCallback;
    private PlayersVisibilityCallback visibilityCallback;
    private PlayersDataCallback dataCallback;
    
    public PlayersManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Инициализация UI компонентов
     */
    public void initializeViews(LinearLayout slidingMenuPanel, ImageButton closeMenuButton,
                                View dummy1, View dummy2,
                                View menuOverlay, View menuLoadingOverlay, View menuLoadingIndicator) {
        this.slidingMenuPanel = slidingMenuPanel;
        this.closeMenuButton = closeMenuButton;
        this.menuOverlay = menuOverlay;
        this.menuLoadingOverlay = menuLoadingOverlay;
        this.menuLoadingIndicator = menuLoadingIndicator;
        
        setupPlayersViews();
    }
    
    private long lastSwipeTime = 0;

    private void switchToNextSidePanelTab() {
        long now = System.currentTimeMillis();
        if (now - lastSwipeTime < 300) return;
        lastSwipeTime = now;

        if (tabLayout == null) return;
        int currentPos = tabLayout.getSelectedTabPosition();
        if (currentPos < tabLayout.getTabCount() - 1) {
            TabLayout.Tab nextTab = tabLayout.getTabAt(currentPos + 1);
            if (nextTab != null) {
                nextTab.select();
            }
        }
    }

    private void switchToPreviousSidePanelTab() {
        long now = System.currentTimeMillis();
        if (now - lastSwipeTime < 300) return;
        lastSwipeTime = now;

        if (tabLayout == null) return;
        int currentPos = tabLayout.getSelectedTabPosition();
        if (currentPos > 0) {
            TabLayout.Tab prevTab = tabLayout.getTabAt(currentPos - 1);
            if (prevTab != null) {
                prevTab.select();
            }
        }
    }

    private void setupSwipeGestures(RecyclerView recyclerView) {
        if (recyclerView == null || context == null) return;

        final GestureDetector gestureDetector = new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 80;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 80;

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();
                        if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX < 0) {
                                switchToNextSidePanelTab();
                            } else {
                                switchToPreviousSidePanelTab();
                            }
                            return true;
                        }
                        return false;
                    }
                }
        );

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            private float startX = 0f;
            private float startY = 0f;
            private boolean isHorizontalSwipe = false;

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = e.getX();
                        startY = e.getY();
                        isHorizontalSwipe = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getX() - startX;
                        float dy = e.getY() - startY;
                        if (!isHorizontalSwipe && Math.abs(dx) > 30f && Math.abs(dx) > Math.abs(dy)) {
                            isHorizontalSwipe = true;
                            if (rv.getParent() != null) {
                                rv.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isHorizontalSwipe) {
                            float diffX = e.getX() - startX;
                            if (Math.abs(diffX) > 80f) {
                                if (diffX < 0) {
                                    switchToNextSidePanelTab();
                                } else {
                                    switchToPreviousSidePanelTab();
                                }
                            }
                        }
                        break;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    /**
     * Настройка UI компонентов плееров
     */
    private void setupPlayersViews() {
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (currentPlayerData != null) {
                    hideMenu();
                }
            });
        }
        
        if (slidingMenuPanel != null) {
            tabLayout = slidingMenuPanel.findViewById(R.id.tabLayout);
            rvVoiceovers = slidingMenuPanel.findViewById(R.id.rvVoiceovers);

            if (rvVoiceovers != null) {
                rvVoiceovers.setLayoutManager(new LinearLayoutManager(context));
                rvVoiceovers.setHasFixedSize(true);
                sidePanelAdapter = new PlayerOptionsAdapter(
                        getCurrentListForSidePanelTab(),
                        currentPlayerData,
                        this::onPlayerSelected
                );
                rvVoiceovers.setAdapter(sidePanelAdapter);
                setupSwipeGestures(rvVoiceovers);
            }

            if (tabLayout != null) {
                tabLayout.removeAllTabs();
                tabLayout.addTab(tabLayout.newTab().setText("AnimeLib"));
                tabLayout.addTab(tabLayout.newTab().setText("Kodik"));

                int selectedIndex = "kodik".equals(sidePanelActiveTab) ? 1 : 0;
                isProgrammaticSideTabSelect = true;
                TabLayout.Tab initialTab = tabLayout.getTabAt(selectedIndex);
                if (initialTab != null) {
                    initialTab.select();
                }
                isProgrammaticSideTabSelect = false;

                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        if (tab == null) return;
                        int pos = tab.getPosition();
                        String targetTab = pos == 0 ? "animelib" : "kodik";

                        boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
                        boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

                        if (!isProgrammaticSideTabSelect) {
                            if ("animelib".equals(targetTab) && !hasAnimelib) {
                                com.example.animelib.util.CustomToast.showWarning(context, "Плеер AnimeLib недоступен или пользователь не авторизован");
                                return;
                            } else if ("kodik".equals(targetTab) && !hasKodik) {
                                com.example.animelib.util.CustomToast.showWarning(context, "Плеер Kodik недоступен");
                                return;
                            }
                        }

                        if (!targetTab.equals(sidePanelActiveTab)) {
                            switchSidePanelTabWithAnimation(targetTab);
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {}

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {}
                });
            }
        }
    }
    
    /**
     * Обработка выбора плеера
     */
    private void onPlayerSelected(EpisodeResponse.PlayerData playerData) {
        Log.d(TAG, "Player selected: " + playerData.getPlayer() + 
              ", Team: " + (playerData.getTeam() != null ? playerData.getTeam().getName() : "null"));
        
        // Update current player data
        currentPlayerData = playerData;
        
        // Обновляем меню с новым currentPlayerData для правильной подсветки
        updateMenuWithData();
        
        // Save user preference
        if (playerData.getPlayer() != null) {
            preferredPlayerType = playerData.getPlayer().toLowerCase();
            if ("animelib".equalsIgnoreCase(playerData.getPlayer())) {
                preferredAnimelibPlayer = playerData;
            } else if ("kodik".equalsIgnoreCase(playerData.getPlayer())) {
                preferredKodikPlayer = playerData;
            }
        }
        
        // Hide menu
        hideMenu();
        
        // Notify callback
        if (playerSelectionCallback != null) {
            playerSelectionCallback.onPlayerSelected(playerData);
        }
    }
    
    /**
     * Показать меню плееров
     */
    public void showMenu() {
        boolean isPortrait = context != null && context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) {
            showVoiceoverBottomSheet();
            return;
        }

        if (!isMenuVisible) {
            Log.d(TAG, "Showing players menu");
            isMenuVisible = true;
            
            // Обновляем данные в меню перед показом
            updateMenuWithData();
            
            // Use VideoPlayerActivity's method to open draggable panel
            if (context instanceof com.example.animelib.VideoPlayerActivity) {
                ((com.example.animelib.VideoPlayerActivity) context).openMenuPanel();
            }
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        }
    }

    /**
     * Показать BottomSheet с озвучками для портретного режима
     */
    public void showVoiceoverBottomSheet() {
        if (voiceoverBottomSheet != null && voiceoverBottomSheet.isShowing()) {
            voiceoverBottomSheet.dismiss();
        }

        voiceoverBottomSheet = new com.example.animelib.ui.VoiceoverBottomSheet(
                context,
                animelibPlayers,
                kodikPlayers,
                currentPlayerData,
                isLoading,
                this::onPlayerSelected,
                () -> {
                    if (context instanceof com.example.animelib.VideoPlayerActivity) {
                        ((com.example.animelib.VideoPlayerActivity) context).showDownloadBottomSheet();
                    }
                }
        );

        voiceoverBottomSheet.setOnDismissListener(dialog -> {
            isMenuVisible = false;
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(false);
            }
        });

        isMenuVisible = true;
        voiceoverBottomSheet.show();

        if (visibilityCallback != null) {
            visibilityCallback.onPlayersVisibilityChanged(true);
        }
    }
    
    /**
     * Скрыть меню плееров
     */
    @OptIn(markerClass = UnstableApi.class)
    public void hideMenu() {
        if (voiceoverBottomSheet != null && voiceoverBottomSheet.isShowing()) {
            voiceoverBottomSheet.dismiss();
        }

        // Block closing if no episode selected
        if (currentPlayerData == null) {
            return;
        }
        
        if (isMenuVisible) {
            Log.d(TAG, "Hiding players menu");
            isMenuVisible = false;
            
            // Use VideoPlayerActivity's method to close draggable panel
            if (context instanceof com.example.animelib.VideoPlayerActivity) {
                ((com.example.animelib.VideoPlayerActivity) context).closeMenuPanel();
            }
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(false);
            }
        }
    }
    
    /**
     * Переключение видимости меню
     */
    public void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }
    
    /**
     * Загрузка плееров для эпизода
     */
    public void loadPlayersForEpisode(int episodeId) {
        Log.d(TAG, "Loading players for episode ID: " + episodeId);
        
        // Show loading
        showLoading();
        
        apiService.fetchEpisodeData(episodeId, new ApiService.EpisodeDataCallback() {
            @Override
            public void onEpisodeDataReceived(EpisodeResponse response) {
                // ВАЖНО: onEpisodeDataReceived вызывается в фоновом потоке (от OkHttp callback)
                // Поэтому loadPlayerPreferences вызывается БЕЗ проблем здесь
                if (response.getData() != null && response.getData().getPlayers() != null) {
                    List<EpisodeResponse.PlayerData> players = response.getData().getPlayers();
                    
                    // Попытка автоматического выбора плеера на основе сохраненных предпочтений
                    com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
                    
                    Log.d(TAG, "Loaded preferences from DB: " + (prefs != null ? 
                          ("player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId() + ", quality=" + prefs.getPreferredQuality()) : 
                          "null"));
                    
                    EpisodeResponse.PlayerData matchingPlayer = null;
                    
                    if (prefs != null && prefs.getPlayer() != null && prefs.getTeamId() != null) {
                        Log.d(TAG, "Found saved preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
                        
                        // Сначала ищем точное совпадение (сохраненный плеер + озвучка)
                        for (EpisodeResponse.PlayerData player : players) {
                            if (player.getPlayer() != null && 
                                player.getPlayer().equals(prefs.getPlayer()) && 
                                player.getTeam() != null && 
                                player.getTeam().getId() == prefs.getTeamId()) {
                                matchingPlayer = player;
                                Log.d(TAG, "Found exact match in saved player: " + player.getPlayer() + 
                                      ", team: " + player.getTeam().getName());
                                break;
                            }
                        }
                        
                        // Если не найдено в сохраненном плеере, ищем озвучку в других плеерах
                        if (matchingPlayer == null) {
                            Log.d(TAG, "Team not found in saved player, searching in other players");
                            for (EpisodeResponse.PlayerData player : players) {
                                if (player.getTeam() != null && 
                                    player.getTeam().getId() == prefs.getTeamId()) {
                                    matchingPlayer = player;
                                    Log.d(TAG, "Found team in different player: " + player.getPlayer() + 
                                          ", team: " + player.getTeam().getName());
                                    break;
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No saved player preferences found");
                    }
                    
                    // Сохраняем финальный результат для передачи в UI поток
                    EpisodeResponse.PlayerData finalMatchingPlayer = matchingPlayer;
                    
                    // Переключаемся в UI поток для обновления интерфейса
                    safeRunOnUiThread(() -> {
                        hideLoading();
                        
                        if (finalMatchingPlayer != null) {
                            // Автоматически выбираем найденный плеер БЕЗ показа меню
                            Log.d(TAG, "Auto-selecting player for episode change: " + 
                                  finalMatchingPlayer.getPlayer() + ", Team: " + 
                                  (finalMatchingPlayer.getTeam() != null ? finalMatchingPlayer.getTeam().getName() : "null"));
                            
                            // Сохраняем данные плееров БЕЗ показа меню
                            setPlayersDataSilent(players);
                            
                            Log.d(TAG, "Players data saved, now calling playerSelectionCallback");
                            
                            // Вызываем callback для выбора плеера
                            if (playerSelectionCallback != null) {
                                playerSelectionCallback.onPlayerSelected(finalMatchingPlayer);
                            } else {
                                Log.w(TAG, "playerSelectionCallback is null!");
                            }
                        } else {
                            // НЕ показываем меню при переключении эпизода
                            // Автоматически выбираем первую доступную озвучку
                            Log.d(TAG, "No matching player found for episode, auto-selecting first available");
                            setPlayersDataSilent(players);
                            
                            // Выбираем первую доступную озвучку
                            if (!players.isEmpty()) {
                                EpisodeResponse.PlayerData firstPlayer = players.get(0);
                                Log.d(TAG, "Auto-selecting first player: " + firstPlayer.getPlayer() + 
                                      ", Team: " + (firstPlayer.getTeam() != null ? firstPlayer.getTeam().getName() : "null"));
                                
                                if (playerSelectionCallback != null) {
                                    playerSelectionCallback.onPlayerSelected(firstPlayer);
                                } else {
                                    Log.w(TAG, "playerSelectionCallback is null!");
                                }
                            } else {
                                Log.e(TAG, "No players available to auto-select!");
                            }
                        }
                        
                        if (dataCallback != null) {
                            dataCallback.onPlayersLoaded(allPlayers);
                        }
                    });
                } else {
                    safeRunOnUiThread(() -> {
                        hideLoading();
                        Log.e(TAG, "No players found in response");
                        if (dataCallback != null) {
                            dataCallback.onPlayersError("Плееры не найдены");
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    Log.e(TAG, "Error loading players: " + error);
                    
                    if (dataCallback != null) {
                        dataCallback.onPlayersError(error);
                    }
                });
            }
        });
    }
    
    /**
     * Установка данных плееров
     */
    public void setPlayersData(List<EpisodeResponse.PlayerData> players) {
        Log.d(TAG, "=== setPlayersData START ===");
        Log.d(TAG, "Input players count: " + (players != null ? players.size() : "null"));
        
        if (players == null || players.isEmpty()) {
            Log.e(TAG, "ERROR: players list is null or empty!");
            return;
        }
        
        // Store all players data
        allPlayers.clear();
        allPlayers.addAll(players);
        Log.d(TAG, "allPlayers size after adding: " + allPlayers.size());
        
        // Debug: print all players
        for (int i = 0; i < players.size(); i++) {
            EpisodeResponse.PlayerData p = players.get(i);
            Log.d(TAG, "  [" + i + "] Player: " + (p.getPlayer() != null ? p.getPlayer() : "null") + 
                  ", Team: " + (p.getTeam() != null ? p.getTeam().getName() : "null"));
        }
        
        // Separate players by type (case-insensitive)
        animelibPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "animelib".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());

        kodikPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "kodik".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());
        
        Log.d(TAG, "After filtering - AnimeLib: " + animelibPlayers.size() + ", Kodik: " + kodikPlayers.size());
        
        // Update menu with players
        updateMenuWithData();
        
        // Try to auto-select preferred player if available
        EpisodeResponse.PlayerData preferredPlayer = findPreferredPlayer();
        if (preferredPlayer != null) {
            Log.d(TAG, "Auto-selecting preferred player: " + preferredPlayer.getPlayer());
            // Добавляем небольшую задержку чтобы пользователь мог увидеть меню
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                onPlayerSelected(preferredPlayer);
            }, 500);
        } else if (!allPlayers.isEmpty()) {
            Log.d(TAG, "No preferred player found, auto-selecting first available player");
            EpisodeResponse.PlayerData firstPlayer = allPlayers.get(0);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                onPlayerSelected(firstPlayer);
            }, 500);
        } else {
            // Show menu for user to select player manually
            Log.d(TAG, "No players found, showing menu");
            showMenu();
        }
        
        Log.d(TAG, "=== setPlayersData END ===");
    }
    
    /**
     * Устанавливает данные плееров БЕЗ показа меню
     * Используется при автовыборе через сохраненные предпочтения
     */
    public void setPlayersDataSilent(List<EpisodeResponse.PlayerData> players) {
        Log.d(TAG, "Setting players data silently (no menu): " + players.size() + " players");
        
        // Store all players data
        allPlayers.clear();
        allPlayers.addAll(players);
        
        // Debug: print all players
        for (EpisodeResponse.PlayerData p : players) {
            Log.d(TAG, "  Player: " + p.getPlayer() + ", Team: " + 
                  (p.getTeam() != null ? p.getTeam().getName() : "null"));
        }
        
        // Separate players by type (case-insensitive)
        animelibPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "animelib".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());

        kodikPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "kodik".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());
        
        Log.d(TAG, "AnimeLib players: " + animelibPlayers.size() + ", Kodik players: " + kodikPlayers.size());
        
        // Update menu with players (but don't show it)
        updateMenuWithData();
    }
    
    /**
     * Обновление меню с данными плееров
     */
    private void updateMenuWithData() {
        if (voiceoverBottomSheet != null && voiceoverBottomSheet.isShowing()) {
            voiceoverBottomSheet.updateData(animelibPlayers, kodikPlayers, currentPlayerData);
        }

        boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
        boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

        if (currentPlayerData != null && currentPlayerData.getPlayer() != null) {
            String p = currentPlayerData.getPlayer().toLowerCase();
            if ("kodik".equals(p) && hasKodik) {
                sidePanelActiveTab = "kodik";
            } else if ("animelib".equals(p) && hasAnimelib) {
                sidePanelActiveTab = "animelib";
            } else if (hasAnimelib) {
                sidePanelActiveTab = "animelib";
            } else if (hasKodik) {
                sidePanelActiveTab = "kodik";
            } else {
                sidePanelActiveTab = "animelib";
            }
        } else if (!hasAnimelib && hasKodik) {
            sidePanelActiveTab = "kodik";
        } else {
            sidePanelActiveTab = "animelib";
        }

        updateSidePanelTabContent();
    }

    private void switchSidePanelTabWithAnimation(String targetTab) {
        sidePanelActiveTab = targetTab;

        boolean isMovingRight = "kodik".equals(targetTab);
        float offset = isMovingRight ? 60f : -60f;

        if (rvVoiceovers != null) {
            rvVoiceovers.animate().cancel();
            rvVoiceovers.animate()
                    .alpha(0f)
                    .translationX(-offset)
                    .setDuration(120)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (sidePanelAdapter != null) {
                            sidePanelAdapter.updatePlayers(getCurrentListForSidePanelTab(), currentPlayerData);
                        }
                        rvVoiceovers.setTranslationX(offset);
                        rvVoiceovers.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(200)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                                .start();
                    })
                    .start();
        } else if (sidePanelAdapter != null) {
            sidePanelAdapter.updatePlayers(getCurrentListForSidePanelTab(), currentPlayerData);
        }
    }

    private void updateSidePanelTabContent() {
        if (tabLayout != null) {
            boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
            boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

            TabLayout.Tab tabAnimelib = tabLayout.getTabAt(0);
            if (tabAnimelib != null && tabAnimelib.view != null) {
                tabAnimelib.view.setEnabled(hasAnimelib);
                tabAnimelib.view.setAlpha(hasAnimelib ? 1.0f : 0.4f);
            }

            TabLayout.Tab tabKodik = tabLayout.getTabAt(1);
            if (tabKodik != null && tabKodik.view != null) {
                tabKodik.view.setEnabled(hasKodik);
                tabKodik.view.setAlpha(hasKodik ? 1.0f : 0.4f);
            }

            int selectedIndex = "kodik".equals(sidePanelActiveTab) ? 1 : 0;
            if (tabLayout.getSelectedTabPosition() != selectedIndex) {
                isProgrammaticSideTabSelect = true;
                TabLayout.Tab tab = tabLayout.getTabAt(selectedIndex);
                if (tab != null) {
                    tab.select();
                }
                isProgrammaticSideTabSelect = false;
            }
        }

        if (sidePanelAdapter != null) {
            sidePanelAdapter.updatePlayers(getCurrentListForSidePanelTab(), currentPlayerData);
        }
    }

    private List<EpisodeResponse.PlayerData> getCurrentListForSidePanelTab() {
        if ("kodik".equals(sidePanelActiveTab)) {
            return kodikPlayers != null ? kodikPlayers : new ArrayList<>();
        }
        return animelibPlayers != null ? animelibPlayers : new ArrayList<>();
    }
    
    /**
     * Поиск предпочитаемого плеера
     */
    private EpisodeResponse.PlayerData findPreferredPlayer() {
        if (preferredPlayerType == null) {
            return null;
        }

        // Try to find exact match for preferred player
        if ("animelib".equals(preferredPlayerType) && preferredAnimelibPlayer != null) {
            for (EpisodeResponse.PlayerData player : animelibPlayers) {
                if (isSamePlayer(preferredAnimelibPlayer, player)) {
                    return player;
                }
            }
            // If exact match not found, return first available AnimeLib player
            return animelibPlayers.isEmpty() ? null : animelibPlayers.get(0);
        } else if ("kodik".equals(preferredPlayerType) && preferredKodikPlayer != null) {
            for (EpisodeResponse.PlayerData player : kodikPlayers) {
                if (isSamePlayer(preferredKodikPlayer, player)) {
                    return player;
                }
            }
            // If exact match not found, return first available Kodik player
            return kodikPlayers.isEmpty() ? null : kodikPlayers.get(0);
        }

        return null;
    }
    
    /**
     * Сравнение плееров
     */
    private boolean isSamePlayer(EpisodeResponse.PlayerData player1, EpisodeResponse.PlayerData player2) {
        if (player1 == null || player2 == null) return false;
        
        // Compare by team name and player type
        boolean sameTeam = (player1.getTeam() != null && player2.getTeam() != null) ?
                player1.getTeam().getName().equals(player2.getTeam().getName()) :
                (player1.getTeam() == null && player2.getTeam() == null);
        
        boolean samePlayer = (player1.getPlayer() != null && player2.getPlayer() != null) ?
                player1.getPlayer().equalsIgnoreCase(player2.getPlayer()) :
                (player1.getPlayer() == null && player2.getPlayer() == null);
        
        return sameTeam && samePlayer;
    }
    
    /**
     * Получение доступных качеств для текущего плеера
     */
    public List<String> getAvailableQualities() {
        List<String> qualities = new ArrayList<>();

        if (currentPlayerData == null) {
            return qualities;
        }

        if ("animelib".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            // AnimeLib qualities are in video.quality array
            if (currentPlayerData.getVideo() != null && currentPlayerData.getVideo().getQuality() != null) {
                for (EpisodeResponse.QualityData qualityData : currentPlayerData.getVideo().getQuality()) {
                    String quality = String.valueOf(qualityData.getQuality());
                    // Skip 4K if not enabled
                    if (("2160".equals(quality) || "4K".equals(quality)) && !enable4K) {
                        Log.d(TAG, "Skipping 4K quality (not enabled)");
                        continue;
                    }
                    qualities.add(quality + "p");
                }
            }
        } else if ("kodik".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            // Kodik qualities are usually standard
            qualities.add("720p");
            qualities.add("480p");
            qualities.add("360p");
        }

        return qualities;
    }
    
    /**
     * Установка настройки 4K
     */
    public void setEnable4K(boolean enable4K) {
        this.enable4K = enable4K;
        Log.d(TAG, "4K setting updated: " + enable4K);
    }
    
    /**
     * Показать индикатор загрузки
     */
    private void showLoading() {
        isLoading = true;
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.VISIBLE);
        }
        if (menuLoadingIndicator != null) {
            menuLoadingIndicator.setVisibility(View.VISIBLE);
        }
        if (voiceoverBottomSheet != null && voiceoverBottomSheet.isShowing()) {
            voiceoverBottomSheet.setLoading(true);
        }
    }
    
    /**
     * Скрыть индикатор загрузки
     */
    private void hideLoading() {
        isLoading = false;
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.GONE);
        }
        if (menuLoadingIndicator != null) {
            menuLoadingIndicator.setVisibility(View.GONE);
        }
        if (voiceoverBottomSheet != null && voiceoverBottomSheet.isShowing()) {
            voiceoverBottomSheet.setLoading(false);
        }
    }
    
    /**
     * Скрытие всех UI элементов плееров (для PiP режима)
     */
    public void hideAllPlayersUI() {
        if (context instanceof com.example.animelib.VideoPlayerActivity) {
            ((com.example.animelib.VideoPlayerActivity) context).closeMenuPanel();
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
        isMenuVisible = false;
    }
    
    /**
     * Показ всех UI элементов плееров (выход из PiP режима)
     */
    public void showAllPlayersUI() {
        // Menu is shown only when user requests it
    }
    
    // Getters
    public List<EpisodeResponse.PlayerData> getAllPlayers() {
        return allPlayers;
    }
    
    public List<EpisodeResponse.PlayerData> getAnimelibPlayers() {
        return animelibPlayers;
    }
    
    public List<EpisodeResponse.PlayerData> getKodikPlayers() {
        return kodikPlayers;
    }
    
    public EpisodeResponse.PlayerData getCurrentPlayerData() {
        return currentPlayerData;
    }
    
    public boolean isMenuVisible() {
        return isMenuVisible;
    }
    
    /**
     * Вызывается когда панель закрывается через драг
     */
    public void onPanelClosedByDrag() {
        Log.d(TAG, "Panel closed by drag, updating isMenuVisible flag");
        isMenuVisible = false;
    }
    
    public String getPreferredPlayerType() {
        return preferredPlayerType;
    }
    
    public EpisodeResponse.PlayerData getPreferredAnimelibPlayer() {
        return preferredAnimelibPlayer;
    }
    
    public EpisodeResponse.PlayerData getPreferredKodikPlayer() {
        return preferredKodikPlayer;
    }
    
    // Setters
    public void setMenuWidth(float menuWidth) {
        this.menuWidth = menuWidth;
    }
    
    public void setCurrentPlayerData(EpisodeResponse.PlayerData currentPlayerData) {
        this.currentPlayerData = currentPlayerData;
        updateMenuWithData();
    }
    
    public void setPreferredPlayerType(String preferredPlayerType) {
        this.preferredPlayerType = preferredPlayerType;
    }
    
    public void setPreferredAnimelibPlayer(EpisodeResponse.PlayerData preferredAnimelibPlayer) {
        this.preferredAnimelibPlayer = preferredAnimelibPlayer;
    }
    
    public void setPreferredKodikPlayer(EpisodeResponse.PlayerData preferredKodikPlayer) {
        this.preferredKodikPlayer = preferredKodikPlayer;
    }
    
    // Callback setters
    public void setPlayerSelectionCallback(PlayerSelectionCallback callback) {
        this.playerSelectionCallback = callback;
    }
    
    public void setVisibilityCallback(PlayersVisibilityCallback callback) {
        this.visibilityCallback = callback;
    }
    
    public void setDataCallback(PlayersDataCallback callback) {
        this.dataCallback = callback;
    }
    
    /**
     * Очистка ресурсов
     */
    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e(TAG, "Error in fallback callback", ex);
            }
        }
    }

    public void cleanup() {
        allPlayers.clear();
        animelibPlayers.clear();
        kodikPlayers.clear();
        currentPlayerData = null;
        playerSelectionCallback = null;
        visibilityCallback = null;
        dataCallback = null;
    }
    
    /**
     * Завершает drag жест с решением открыть или закрыть панель плееров
     */
    public void completeDrag(boolean shouldOpen) {
        Log.d(TAG, "Complete players drag: shouldOpen=" + shouldOpen);
        
        if (shouldOpen) {
            // При drag открытии НЕ вызываем openMenuPanel() - панель уже открывается через DraggableSidePanel
            // Только обновляем флаг
            if (isMenuVisible) {
                Log.w(TAG, "Players menu already visible, skipping");
                return;
            }
            
            isMenuVisible = true;
            
            // Уведомить о изменении видимости
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        } else {
            hideMenu();
        }
    }
    
    /**
     * Обновляет состояние после drag (вызывается после завершения анимации DraggableSidePanel)
     */
    public void updateDragState(boolean isOpen) {
        Log.d(TAG, "Update drag state: isOpen=" + isOpen);
        
        if (isOpen) {
            isMenuVisible = true;
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        } else {
            isMenuVisible = false;
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(false);
            }
        }
    }
}
