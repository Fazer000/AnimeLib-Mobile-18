package com.example.animelib.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.adapters.PlayerOptionsAdapter;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.util.FloatingBottomSheetUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class VoiceoverBottomSheet extends BottomSheetDialog {

    public interface OnPlayerSelectedListener {
        void onPlayerSelected(EpisodeResponse.PlayerData playerData);
    }

    public interface OnDownloadRequestedListener {
        void onDownloadRequested();
    }

    private List<EpisodeResponse.PlayerData> animelibPlayers;
    private List<EpisodeResponse.PlayerData> kodikPlayers;
    private List<EpisodeResponse.PlayerData> testPlayers = new ArrayList<>();
    private EpisodeResponse.PlayerData currentPlayerData;
    private boolean isLoading = false;

    private final OnPlayerSelectedListener selectionListener;
    private final OnDownloadRequestedListener downloadListener;

    private TabLayout tabLayout;
    private RecyclerView rvVoiceovers;
    private View loadingOverlay;
    private PlayerOptionsAdapter adapter;

    private String activeTab = "animelib";
    private int savedHeight = 0;
    private boolean isProgrammaticTabSelect = false;

    public VoiceoverBottomSheet(@NonNull Context context,
                                List<EpisodeResponse.PlayerData> animelibPlayers,
                                List<EpisodeResponse.PlayerData> kodikPlayers,
                                EpisodeResponse.PlayerData currentPlayerData,
                                boolean isLoading,
                                OnPlayerSelectedListener selectionListener,
                                OnDownloadRequestedListener downloadListener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.animelibPlayers = animelibPlayers != null ? animelibPlayers : new ArrayList<>();
        this.kodikPlayers = kodikPlayers != null ? kodikPlayers : new ArrayList<>();
        this.currentPlayerData = currentPlayerData;
        this.isLoading = isLoading;
        this.selectionListener = selectionListener;
        this.downloadListener = downloadListener;
        initTestPlayers();
    }

    private void initTestPlayers() {
        testPlayers = new ArrayList<>();

        EpisodeResponse.PlayerData testItem1 = new EpisodeResponse.PlayerData();
        testItem1.setPlayer("test");
        EpisodeResponse.Team team1 = new EpisodeResponse.Team();
        team1.setName("Тестовая озвучка #1");
        testItem1.setTeam(team1);

        EpisodeResponse.PlayerData testItem2 = new EpisodeResponse.PlayerData();
        testItem2.setPlayer("test");
        EpisodeResponse.Team team2 = new EpisodeResponse.Team();
        team2.setName("Тестовый дубляж Studio");
        testItem2.setTeam(team2);

        EpisodeResponse.PlayerData testItem3 = new EpisodeResponse.PlayerData();
        testItem3.setPlayer("test");
        EpisodeResponse.Team team3 = new EpisodeResponse.Team();
        team3.setName("Тестовый плеер (Demo 1080p)");
        testItem3.setTeam(team3);

        testPlayers.add(testItem1);
        testPlayers.add(testItem2);
        testPlayers.add(testItem3);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_voiceovers, null);
        setContentView(view);
        setCanceledOnTouchOutside(true);
        setCancelable(true);
        FloatingBottomSheetUtils.setupFloatingStyle(this);

        tabLayout = view.findViewById(R.id.tabLayout);
        rvVoiceovers = view.findViewById(R.id.rvVoiceovers);
        loadingOverlay = view.findViewById(R.id.menuLoadingOverlay);

        ImageButton closeButton = view.findViewById(R.id.closeMenuButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }

        ImageButton downloadButton = view.findViewById(R.id.btnDownloadFromMenu);
        if (downloadButton != null) {
            downloadButton.setOnClickListener(v -> {
                dismiss();
                if (downloadListener != null) {
                    downloadListener.onDownloadRequested();
                }
            });
        }

        setupRecyclerView();
        setupTabButtons();
        setLoading(isLoading);
        setupBottomSheetBehavior();

        FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.post(new Runnable() {
                @Override
                public void run() {
                    savedHeight = bottomSheet.getHeight();
                }
            });
        }
    }

    @NonNull
    @Override
    public BottomSheetBehavior<FrameLayout> getBehavior() {
        return super.getBehavior();
    }

    private void setupBottomSheetBehavior() {
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        if (behavior != null) {
            int maxHeightPx = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.65f);
            behavior.setMaxHeight(maxHeightPx);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);
            behavior.setFitToContents(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheetView, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss();
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheetView, float slideOffset) {
                }
            });

            FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.post(() -> {
                    behavior.setMaxHeight(maxHeightPx);
                    behavior.setSkipCollapsed(true);
                    behavior.setHideable(true);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                });
            }
        }
    }

    @Override
    public void show() {
        super.show();
        setupBottomSheetBehavior();
    }

    public void updateData(List<EpisodeResponse.PlayerData> animelibPlayers,
                           List<EpisodeResponse.PlayerData> kodikPlayers,
                           EpisodeResponse.PlayerData currentPlayerData) {
        this.animelibPlayers = animelibPlayers != null ? animelibPlayers : new ArrayList<>();
        this.kodikPlayers = kodikPlayers != null ? kodikPlayers : new ArrayList<>();
        this.currentPlayerData = currentPlayerData;

        determineActiveTab();
        updateSelectedTabContent();
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void setupRecyclerView() {
        if (rvVoiceovers == null) return;

        if (getContext() != null) {
            android.util.DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int halfScreenHeight = displayMetrics.heightPixels / 2;

            View playersContainer = findViewById(R.id.playersContainer);
            if (playersContainer != null) {
                android.view.ViewGroup.LayoutParams params = playersContainer.getLayoutParams();
                if (params != null) {
                    params.height = halfScreenHeight;
                    playersContainer.setLayoutParams(params);
                }
            }
        }

        rvVoiceovers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVoiceovers.setHasFixedSize(true);
        adapter = new PlayerOptionsAdapter(
                getCurrentListForActiveTab(),
                currentPlayerData,
                playerData -> {
                    dismiss();
                    if (selectionListener != null) {
                        selectionListener.onPlayerSelected(playerData);
                    }
                }
        );
        rvVoiceovers.setAdapter(adapter);
    }

    private void determineActiveTab() {
        boolean hasAnimelib = !animelibPlayers.isEmpty();
        boolean hasKodik = !kodikPlayers.isEmpty();

        if (currentPlayerData != null && currentPlayerData.getPlayer() != null) {
            String p = currentPlayerData.getPlayer().toLowerCase();
            if ("kodik".equals(p) && hasKodik) {
                activeTab = "kodik";
            } else if ("animelib".equals(p) && hasAnimelib) {
                activeTab = "animelib";
            } else if (hasAnimelib) {
                activeTab = "animelib";
            } else if (hasKodik) {
                activeTab = "kodik";
            } else {
                activeTab = "animelib";
            }
        } else if (!hasAnimelib && hasKodik) {
            activeTab = "kodik";
        } else {
            activeTab = "animelib";
        }
    }

    private void setupTabButtons() {
        determineActiveTab();

        if (tabLayout != null) {
            tabLayout.removeAllTabs();
            tabLayout.addTab(tabLayout.newTab().setText("AnimeLib"));
            tabLayout.addTab(tabLayout.newTab().setText("Kodik"));
            tabLayout.addTab(tabLayout.newTab().setText("Тест"));

            int selectedIndex = 0;
            if ("kodik".equals(activeTab)) selectedIndex = 1;
            else if ("test".equals(activeTab)) selectedIndex = 2;

            isProgrammaticTabSelect = true;
            TabLayout.Tab initialTab = tabLayout.getTabAt(selectedIndex);
            if (initialTab != null) {
                initialTab.select();
            }
            isProgrammaticTabSelect = false;

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab == null) return;
                    int pos = tab.getPosition();
                    String targetTab = pos == 0 ? "animelib" : (pos == 1 ? "kodik" : "test");

                    boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
                    boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

                    if (!isProgrammaticTabSelect) {
                        if ("animelib".equals(targetTab) && !hasAnimelib) {
                            com.example.animelib.util.CustomToast.showWarning(getContext(), "Плеер AnimeLib недоступен или пользователь не авторизован");
                            return;
                        } else if ("kodik".equals(targetTab) && !hasKodik) {
                            com.example.animelib.util.CustomToast.showWarning(getContext(), "Плеер Kodik недоступен");
                            return;
                        }
                    }

                    if (!targetTab.equals(activeTab)) {
                        switchTabWithAnimation(targetTab);
                    }

                    FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    BottomSheetBehavior<FrameLayout> behavior = getBehavior();
                    if (bottomSheet != null && behavior != null && savedHeight > 0) {
                        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                        params.height = savedHeight;
                        bottomSheet.setLayoutParams(params);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    if (tab == null) return;
                    FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    BottomSheetBehavior<FrameLayout> behavior = getBehavior();
                    if (bottomSheet != null && behavior != null && savedHeight > 0) {
                        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                        params.height = savedHeight;
                        bottomSheet.setLayoutParams(params);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            });
        }

        updateSelectedTabContent();
    }

    private void switchTabWithAnimation(String targetTab) {
        String prevTab = activeTab;
        activeTab = targetTab;

        boolean isMovingRight = "kodik".equals(targetTab) || ("test".equals(targetTab) && !"kodik".equals(prevTab));
        float offset = isMovingRight ? 60f : -60f;

        if (rvVoiceovers != null) {
            rvVoiceovers.animate().cancel();
            rvVoiceovers.animate()
                    .alpha(0f)
                    .translationX(-offset)
                    .setDuration(120)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (adapter != null) {
                            adapter.updatePlayers(getCurrentListForActiveTab(), currentPlayerData);
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
        } else if (adapter != null) {
            adapter.updatePlayers(getCurrentListForActiveTab(), currentPlayerData);
        }
    }

    private void updateSelectedTabContent() {
        if (tabLayout != null) {
            boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
            boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();
            boolean hasTest = testPlayers != null && !testPlayers.isEmpty();

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

            TabLayout.Tab tabTest = tabLayout.getTabAt(2);
            if (tabTest != null && tabTest.view != null) {
                tabTest.view.setEnabled(hasTest);
                tabTest.view.setAlpha(hasTest ? 1.0f : 0.4f);
            }

            int selectedIndex = 0;
            if ("kodik".equals(activeTab)) selectedIndex = 1;
            else if ("test".equals(activeTab)) selectedIndex = 2;

            if (tabLayout.getSelectedTabPosition() != selectedIndex) {
                isProgrammaticTabSelect = true;
                TabLayout.Tab tab = tabLayout.getTabAt(selectedIndex);
                if (tab != null) {
                    tab.select();
                }
                isProgrammaticTabSelect = false;
            }
        }

        if (adapter != null) {
            adapter.updatePlayers(getCurrentListForActiveTab(), currentPlayerData);
        }
    }

    private List<EpisodeResponse.PlayerData> getCurrentListForActiveTab() {
        if ("kodik".equals(activeTab)) {
            return kodikPlayers;
        } else if ("test".equals(activeTab)) {
            return testPlayers;
        }
        return animelibPlayers;
    }
}
