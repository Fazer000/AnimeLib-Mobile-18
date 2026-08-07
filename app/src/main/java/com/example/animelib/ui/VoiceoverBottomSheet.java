package com.example.animelib.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
    private long lastSwipeTime = 0;

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
        setupSwipeGestures(rvVoiceovers);
    }

    private void switchToNextTab() {
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

    private void switchToPreviousTab() {
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
        if (recyclerView == null || getContext() == null) return;

        final GestureDetector gestureDetector = new GestureDetector(
                getContext(),
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
                                switchToNextTab();
                            } else {
                                switchToPreviousTab();
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
                                    switchToNextTab();
                                } else {
                                    switchToPreviousTab();
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

            int selectedIndex = "kodik".equals(activeTab) ? 1 : 0;

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
                    String targetTab = pos == 0 ? "animelib" : "kodik";

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

            int selectedIndex = "kodik".equals(activeTab) ? 1 : 0;

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
        }
        return animelibPlayers;
    }
}
