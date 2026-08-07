package com.example.animelib.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.adapters.PlayerOptionsAdapter;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.FloatingBottomSheetUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

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
    private TextView tvEmptyVoiceovers;
    private FrameLayout playersContainer;
    private PlayerOptionsAdapter adapter;

    private String activeTab = "animelib";
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
        tvEmptyVoiceovers = view.findViewById(R.id.tvEmptyVoiceovers);
        playersContainer = view.findViewById(R.id.playersContainer);

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
    }

    @NonNull
    @Override
    public BottomSheetBehavior<FrameLayout> getBehavior() {
        return super.getBehavior();
    }

    private void setupBottomSheetBehavior() {
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        if (behavior != null) {
            behavior.setFitToContents(true);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);
            behavior.setPeekHeight(0);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void setupRecyclerView() {
        if (rvVoiceovers == null) return;

        if (playersContainer instanceof MaxHeightFrameLayout) {
            MaxHeightFrameLayout mhl = (MaxHeightFrameLayout) playersContainer;
            mhl.setMaxHeightRatio(0.45f);
            mhl.setFixedHeight(false);
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
        boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
        boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

        if (currentPlayerData != null && currentPlayerData.getPlayer() != null) {
            String p = currentPlayerData.getPlayer().toLowerCase();
            if ("kodik".equals(p) && hasKodik) {
                activeTab = "kodik";
                return;
            } else if ("animelib".equals(p) && hasAnimelib) {
                activeTab = "animelib";
                return;
            }
        }

        if (!hasAnimelib && hasKodik) {
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
                    if (tab == null || isProgrammaticTabSelect) return;
                    int pos = tab.getPosition();
                    String targetTab = pos == 0 ? "animelib" : "kodik";

                    boolean hasAnimelib = animelibPlayers != null && !animelibPlayers.isEmpty();
                    boolean hasKodik = kodikPlayers != null && !kodikPlayers.isEmpty();

                    if ("animelib".equals(targetTab) && !hasAnimelib) {
                        CustomToast.showWarning(getContext(), "Озвучка AnimeLib недоступна");
                        revertToPreviousTab();
                        return;
                    } else if ("kodik".equals(targetTab) && !hasKodik) {
                        CustomToast.showWarning(getContext(), "Озвучка Kodik недоступна");
                        revertToPreviousTab();
                        return;
                    }

                    activeTab = targetTab;
                    updateTabContent();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        updateTabContent();
    }

    private void revertToPreviousTab() {
        if (tabLayout == null) return;
        int targetPos = "kodik".equals(activeTab) ? 1 : 0;
        isProgrammaticTabSelect = true;
        TabLayout.Tab tab = tabLayout.getTabAt(targetPos);
        if (tab != null) {
            tab.select();
        }
        isProgrammaticTabSelect = false;
    }

    private void updateTabContent() {
        List<EpisodeResponse.PlayerData> currentList = getCurrentListForActiveTab();
        if (adapter != null) {
            adapter.updatePlayers(currentList, currentPlayerData);
        }

        boolean isEmpty = currentList == null || currentList.isEmpty();
        if (tvEmptyVoiceovers != null) {
            tvEmptyVoiceovers.setVisibility(isEmpty && !isLoading ? View.VISIBLE : View.GONE);
        }
        if (rvVoiceovers != null) {
            rvVoiceovers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            rvVoiceovers.post(() -> {
                BottomSheetBehavior<FrameLayout> behavior = getBehavior();
                if (behavior != null && isShowing()) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
        }
    }

    public void updateData(List<EpisodeResponse.PlayerData> animelibPlayers,
                           List<EpisodeResponse.PlayerData> kodikPlayers,
                           EpisodeResponse.PlayerData currentPlayerData) {
        this.animelibPlayers = animelibPlayers != null ? animelibPlayers : new ArrayList<>();
        this.kodikPlayers = kodikPlayers != null ? kodikPlayers : new ArrayList<>();
        this.currentPlayerData = currentPlayerData;

        determineActiveTab();

        if (tabLayout != null) {
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

        updateTabContent();
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (!loading) {
            updateTabContent();
        }
    }

    private List<EpisodeResponse.PlayerData> getCurrentListForActiveTab() {
        if ("kodik".equals(activeTab)) {
            return kodikPlayers;
        }
        return animelibPlayers;
    }
}
