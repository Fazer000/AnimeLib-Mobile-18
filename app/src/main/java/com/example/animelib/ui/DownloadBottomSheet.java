package com.example.animelib.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.api.ApiService;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.DownloadedEpisodeEntity;
import com.example.animelib.models.DownloadTask;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.services.DownloadService;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class DownloadBottomSheet extends BottomSheetDialogFragment {

    public static class VoiceoverOption {
        public String title;
        public String teamName;
        public int teamId;
        public String playerType;

        public VoiceoverOption(String title, String teamName, int teamId, String playerType) {
            this.title = title;
            this.teamName = teamName;
            this.teamId = teamId;
            this.playerType = playerType;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }

    public static class EpisodeCheckItem {
        public EpisodesListResponse.EpisodeItem episode;
        public boolean isChecked;
        public boolean isAvailable;
        public boolean isAlreadyDownloaded;

        public EpisodeCheckItem(EpisodesListResponse.EpisodeItem episode, boolean isChecked, boolean isAvailable, boolean isAlreadyDownloaded) {
            this.episode = episode;
            this.isChecked = isChecked;
            this.isAvailable = isAvailable;
            this.isAlreadyDownloaded = isAlreadyDownloaded;
        }
    }

    private String animeId;
    private String animeTitle;
    private String posterUrl;
    private List<EpisodesListResponse.EpisodeItem> episodeItems = new ArrayList<>();
    private List<EpisodeResponse.PlayerData> currentPlayers = new ArrayList<>();

    // UI Elements - Navigation & Header
    private ImageButton btnBsBack;
    private ImageButton btnBsClose;
    private TextView tvTitle;
    private TextView tvSubtitle;

    // Step 1 UI Elements
    private View layoutStep1;
    private TextView chipPlayerAnimelib;
    private TextView chipPlayerKodik;
    private LinearLayout cgQualities;
    private TextView chipQ2160;
    private TextView chipQ1080;
    private TextView chipQ720;
    private TextView chipQ480;
    private TextView chipQ360;
    private RecyclerView rvVoiceovers;
    private MaterialButton btnNextStep;

    // Step 2 UI Elements
    private LinearLayout layoutStep2;
    private TextView btnSelectAll;
    private TextView btnDeselectAll;
    private TextView btnInvertSelection;
    private TextView tvSelectedCounter;
    private ProgressBar pbLoading;
    private RecyclerView rvEpisodes;
    private MaterialButton btnDownload;

    // State Variables
    private String selectedPlayerType = "animelib"; // "animelib" or "kodik"
    private String selectedQuality = "1080p";
    private VoiceoverOption selectedVoiceover;

    private final Map<String, Set<String>> kodikQualitiesCache = new HashMap<>();
    private final List<TextView> dynamicQualityChips = new ArrayList<>();

    private final List<VoiceoverOption> allVoiceovers = new ArrayList<>();
    private final List<VoiceoverOption> displayedVoiceovers = new ArrayList<>();
    private VoiceoverAdapter voiceoverAdapter;

    private final List<EpisodeCheckItem> checkItems = new ArrayList<>();
    private EpisodeAdapter episodeAdapter;

    private ApiService apiService;
    private DatabaseManager databaseManager;

    public static DownloadBottomSheet newInstance(String animeId, String animeTitle, String posterUrl,
                                                 List<EpisodesListResponse.EpisodeItem> episodeItems,
                                                 List<EpisodeResponse.PlayerData> currentPlayers) {
        DownloadBottomSheet fragment = new DownloadBottomSheet();
        fragment.animeId = animeId;
        fragment.animeTitle = animeTitle;
        fragment.posterUrl = posterUrl;
        if (episodeItems != null) fragment.episodeItems = episodeItems;
        if (currentPlayers != null) fragment.currentPlayers = currentPlayers;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() instanceof BottomSheetDialog) {
            com.example.animelib.util.FloatingBottomSheetUtils.setupFloatingStyle((BottomSheetDialog) getDialog());
        }

        Context ctx = requireContext();
        apiService = new ApiService(ctx);
        databaseManager = new DatabaseManager(ctx);

        // Header & Views init
        btnBsBack = view.findViewById(R.id.btnBsBack);
        btnBsClose = view.findViewById(R.id.btnBsClose);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);

        // Step 1 Views init
        layoutStep1 = view.findViewById(R.id.layoutStep1);
        chipPlayerAnimelib = view.findViewById(R.id.chipPlayerAnimelib);
        chipPlayerKodik = view.findViewById(R.id.chipPlayerKodik);
        cgQualities = view.findViewById(R.id.cgQualities);
        chipQ2160 = view.findViewById(R.id.chipQ2160);
        chipQ1080 = view.findViewById(R.id.chipQ1080);
        chipQ720 = view.findViewById(R.id.chipQ720);
        chipQ480 = view.findViewById(R.id.chipQ480);
        chipQ360 = view.findViewById(R.id.chipQ360);
        rvVoiceovers = view.findViewById(R.id.rvVoiceovers);
        btnNextStep = view.findViewById(R.id.btnNextStep);

        // Step 2 Views init
        layoutStep2 = view.findViewById(R.id.layoutStep2);
        btnSelectAll = view.findViewById(R.id.btnSelectAll);
        btnDeselectAll = view.findViewById(R.id.btnDeselectAll);
        btnInvertSelection = view.findViewById(R.id.btnInvertSelection);
        tvSelectedCounter = view.findViewById(R.id.tvSelectedCounter);
        pbLoading = view.findViewById(R.id.pbLoading);
        rvEpisodes = view.findViewById(R.id.rvEpisodes);
        btnDownload = view.findViewById(R.id.btnDownload);

        // Setup Header Listeners
        if (btnBsClose != null) btnBsClose.setOnClickListener(v -> dismiss());
        if (btnBsBack != null) btnBsBack.setOnClickListener(v -> showStep1());
        ImageButton btnBsGoToDownloads = view.findViewById(R.id.btnBsGoToDownloads);
        if (btnBsGoToDownloads != null) {
            btnBsGoToDownloads.setOnClickListener(v -> {
                dismiss();
                DownloadsActivity.start(requireContext());
            });
        }

        // Setup Step 1 Chips & Recycler
        setupPlayerChips();
        setupQualityChips();
        parseAllVoiceovers();
        updatePlayerChipsUI();
        setupVoiceoversList();

        btnNextStep.setOnClickListener(v -> {
            if (selectedVoiceover == null && !displayedVoiceovers.isEmpty()) {
                selectedVoiceover = displayedVoiceovers.get(0);
            }
            if (selectedVoiceover == null) {
                Toast.makeText(requireContext(), "Выберите озвучку", Toast.LENGTH_SHORT).show();
                return;
            }
            showStep2();
        });

        // Setup Step 2 Listeners & Recycler
        setupEpisodesList();

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                for (EpisodeCheckItem item : checkItems) {
                    if (item.isAvailable && !item.isAlreadyDownloaded) {
                        item.isChecked = true;
                    }
                }
                if (episodeAdapter != null) episodeAdapter.notifyDataSetChanged();
                updateDownloadButtonText();
            });
        }

        if (btnDeselectAll != null) {
            btnDeselectAll.setOnClickListener(v -> {
                for (EpisodeCheckItem item : checkItems) {
                    item.isChecked = false;
                }
                if (episodeAdapter != null) episodeAdapter.notifyDataSetChanged();
                updateDownloadButtonText();
            });
        }

        if (btnInvertSelection != null) {
            btnInvertSelection.setOnClickListener(v -> {
                for (EpisodeCheckItem item : checkItems) {
                    if (item.isAvailable && !item.isAlreadyDownloaded) {
                        item.isChecked = !item.isChecked;
                    }
                }
                if (episodeAdapter != null) episodeAdapter.notifyDataSetChanged();
                updateDownloadButtonText();
            });
        }

        btnDownload.setOnClickListener(v -> startDownloadQueue());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            com.example.animelib.util.FloatingBottomSheetUtils.applyFloatingToView((BottomSheetDialog) getDialog());
        }
    }

    private void showStep1() {
        btnBsBack.setVisibility(View.GONE);
        tvTitle.setText("Скачивание серий");
        tvSubtitle.setText("Шаг 1 из 2: Озвучка и качество");

        if (layoutStep2.getVisibility() == View.VISIBLE) {
            layoutStep2.animate()
                .alpha(0f)
                .translationX(100f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutStep2.setVisibility(View.GONE);
                    layoutStep1.setTranslationX(-100f);
                    layoutStep1.setAlpha(0f);
                    layoutStep1.setVisibility(View.VISIBLE);
                    layoutStep1.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(180)
                        .start();
                })
                .start();
        } else {
            layoutStep1.setVisibility(View.VISIBLE);
            layoutStep2.setVisibility(View.GONE);
        }
    }

    private void showStep2() {
        btnBsBack.setVisibility(View.VISIBLE);
        tvTitle.setText("Выбор серий");
        String info = (selectedVoiceover != null ? selectedVoiceover.teamName : "Озвучка") + " • " + selectedQuality;
        tvSubtitle.setText(info);

        if (selectedVoiceover != null) {
            checkVoiceoverAvailability(selectedVoiceover);
        }

        if (layoutStep1.getVisibility() == View.VISIBLE) {
            layoutStep1.animate()
                .alpha(0f)
                .translationX(-100f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutStep1.setVisibility(View.GONE);
                    layoutStep2.setTranslationX(100f);
                    layoutStep2.setAlpha(0f);
                    layoutStep2.setVisibility(View.VISIBLE);
                    layoutStep2.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(180)
                        .start();
                })
                .start();
        } else {
            layoutStep1.setVisibility(View.GONE);
            layoutStep2.setVisibility(View.VISIBLE);
        }
    }

    private void setupPlayerChips() {
        chipPlayerAnimelib.setOnClickListener(v -> {
            selectedPlayerType = "animelib";
            updatePlayerChipsUI();
            updateVoiceoversForSelectedPlayer();
        });

        chipPlayerKodik.setOnClickListener(v -> {
            selectedPlayerType = "kodik";
            updatePlayerChipsUI();
            updateVoiceoversForSelectedPlayer();
        });

        updatePlayerChipsUI();
    }

    private void updatePlayerChipsUI() {
        if (!isAdded() || getContext() == null) return;

        boolean hasAnimelib = false;
        boolean hasKodik = false;

        if (currentPlayers != null) {
            for (EpisodeResponse.PlayerData p : currentPlayers) {
                if (p != null && p.getPlayer() != null) {
                    String pType = p.getPlayer().toLowerCase();
                    if (pType.contains("animelib")) {
                        hasAnimelib = true;
                    }
                    if (pType.contains("kodik")) {
                        hasKodik = true;
                    }
                }
            }
        }

        for (VoiceoverOption vo : allVoiceovers) {
            if (vo != null && vo.playerType != null) {
                if (vo.playerType.contains("animelib")) hasAnimelib = true;
                if (vo.playerType.contains("kodik")) hasKodik = true;
            }
        }

        if (!hasAnimelib && !hasKodik) {
            hasAnimelib = true;
            hasKodik = true;
        }

        if (chipPlayerAnimelib != null) {
            chipPlayerAnimelib.setVisibility(hasAnimelib ? View.VISIBLE : View.GONE);
        }
        if (chipPlayerKodik != null) {
            chipPlayerKodik.setVisibility(hasKodik ? View.VISIBLE : View.GONE);
        }

        if ("animelib".equalsIgnoreCase(selectedPlayerType) && !hasAnimelib && hasKodik) {
            selectedPlayerType = "kodik";
        } else if ("kodik".equalsIgnoreCase(selectedPlayerType) && !hasKodik && hasAnimelib) {
            selectedPlayerType = "animelib";
        }

        int selectedTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text_color);
        int unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.primary_text_color);

        if ("animelib".equalsIgnoreCase(selectedPlayerType)) {
            if (chipPlayerAnimelib != null) {
                chipPlayerAnimelib.setBackgroundResource(R.drawable.chip_selected);
                chipPlayerAnimelib.setTextColor(selectedTextColor);
            }
            if (chipPlayerKodik != null) {
                chipPlayerKodik.setBackgroundResource(R.drawable.chip_unselected);
                chipPlayerKodik.setTextColor(unselectedTextColor);
            }
        } else {
            if (chipPlayerKodik != null) {
                chipPlayerKodik.setBackgroundResource(R.drawable.chip_selected);
                chipPlayerKodik.setTextColor(selectedTextColor);
            }
            if (chipPlayerAnimelib != null) {
                chipPlayerAnimelib.setBackgroundResource(R.drawable.chip_unselected);
                chipPlayerAnimelib.setTextColor(unselectedTextColor);
            }
        }
    }

    private void setupQualityChips() {
        View.OnClickListener listener = v -> {
            TextView tv = (TextView) v;
            selectedQuality = tv.getText().toString();
            updateQualityChipsUI();
        };

        if (chipQ2160 != null) chipQ2160.setOnClickListener(listener);
        if (chipQ1080 != null) chipQ1080.setOnClickListener(listener);
        if (chipQ720 != null) chipQ720.setOnClickListener(listener);
        if (chipQ480 != null) chipQ480.setOnClickListener(listener);
        if (chipQ360 != null) chipQ360.setOnClickListener(listener);

        updateQualityChipsUI();
    }

    private void updateQualityChipsUI() {
        List<TextView> allChips = new ArrayList<>();
        if (chipQ2160 != null) allChips.add(chipQ2160);
        if (chipQ1080 != null) allChips.add(chipQ1080);
        if (chipQ720 != null) allChips.add(chipQ720);
        if (chipQ480 != null) allChips.add(chipQ480);
        if (chipQ360 != null) allChips.add(chipQ360);
        allChips.addAll(dynamicQualityChips);

        if (!isAdded() || getContext() == null) return;
        int selectedTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text_color);
        int unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.primary_text_color);

        for (TextView chip : allChips) {
            if (chip != null) {
                if (chip.getText().toString().equalsIgnoreCase(selectedQuality)) {
                    chip.setBackgroundResource(R.drawable.chip_selected);
                    chip.setTextColor(selectedTextColor);
                } else {
                    chip.setBackgroundResource(R.drawable.chip_unselected);
                    chip.setTextColor(unselectedTextColor);
                }
            }
        }
    }

    private EpisodeResponse.PlayerData getSelectedPlayerData() {
        if (currentPlayers == null || currentPlayers.isEmpty()) {
            return null;
        }
        if (selectedVoiceover != null) {
            for (EpisodeResponse.PlayerData p : currentPlayers) {
                if (p != null && p.getPlayer() != null) {
                    boolean matchPlayer = p.getPlayer().equalsIgnoreCase(selectedPlayerType) ||
                            ("animelib".equalsIgnoreCase(selectedPlayerType) && p.getPlayer().toLowerCase().contains("animelib")) ||
                            ("kodik".equalsIgnoreCase(selectedPlayerType) && p.getPlayer().toLowerCase().contains("kodik"));
                    boolean matchTeam = p.getTeam() != null &&
                            (p.getTeam().getId() == selectedVoiceover.teamId ||
                             (p.getTeam().getName() != null && selectedVoiceover.teamName != null && p.getTeam().getName().equalsIgnoreCase(selectedVoiceover.teamName)));
                    if (matchPlayer && matchTeam) {
                        return p;
                    }
                }
            }
        }
        for (EpisodeResponse.PlayerData p : currentPlayers) {
            if (p != null && p.getPlayer() != null) {
                boolean matchPlayer = p.getPlayer().equalsIgnoreCase(selectedPlayerType) ||
                        ("animelib".equalsIgnoreCase(selectedPlayerType) && p.getPlayer().toLowerCase().contains("animelib")) ||
                        ("kodik".equalsIgnoreCase(selectedPlayerType) && p.getPlayer().toLowerCase().contains("kodik"));
                if (matchPlayer) {
                    return p;
                }
            }
        }
        return null;
    }

    private void updateAvailableQualities() {
        EpisodeResponse.PlayerData playerData = getSelectedPlayerData();
        Set<String> availableQualities = new HashSet<>();

        if (playerData != null) {
            if (playerData.getVideo() != null && playerData.getVideo().getQuality() != null && !playerData.getVideo().getQuality().isEmpty()) {
                for (EpisodeResponse.QualityData qd : playerData.getVideo().getQuality()) {
                    if (qd != null && qd.getQuality() > 0) {
                        availableQualities.add(String.valueOf(qd.getQuality()));
                    }
                }
            }

            if (availableQualities.isEmpty() && playerData.getSrc() != null && !playerData.getSrc().isEmpty()) {
                String src = playerData.getSrc();
                if (kodikQualitiesCache.containsKey(src)) {
                    availableQualities.addAll(kodikQualitiesCache.get(src));
                } else if ("kodik".equalsIgnoreCase(selectedPlayerType) || src.contains("kodik")) {
                    apiService.fetchKodikVideoLinks(src, new ApiService.KodikVideoCallback() {
                        @Override
                        public void onKodikVideoReceived(KodikResponse response) {
                            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                                Set<String> fetched = new HashSet<>(response.getData().keySet());
                                kodikQualitiesCache.put(src, fetched);
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> updateAvailableQualities());
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                        }
                    });
                }
            }
        }

        applyQualityFilter(availableQualities);
    }

    private void applyQualityFilter(Set<String> availableQualities) {
        if (!isAdded() || getContext() == null) return;

        if (cgQualities != null && !dynamicQualityChips.isEmpty()) {
            for (TextView dynamicChip : dynamicQualityChips) {
                cgQualities.removeView(dynamicChip);
            }
            dynamicQualityChips.clear();
        }

        TextView[] standardChips = new TextView[]{chipQ2160, chipQ1080, chipQ720, chipQ480, chipQ360};
        String[] standardKeys = new String[]{"2160", "1080", "720", "480", "360"};

        if (availableQualities == null || availableQualities.isEmpty()) {
            for (TextView chip : standardChips) {
                if (chip != null) chip.setVisibility(View.VISIBLE);
            }
            updateQualityChipsUI();
            return;
        }

        List<String> sortedQualities = new ArrayList<>(availableQualities);
        Collections.sort(sortedQualities, (q1, q2) -> {
            try {
                return Integer.compare(Integer.parseInt(q2), Integer.parseInt(q1));
            } catch (NumberFormatException e) {
                return q2.compareTo(q1);
            }
        });

        for (TextView chip : standardChips) {
            if (chip != null) chip.setVisibility(View.GONE);
        }

        for (String qKey : sortedQualities) {
            boolean matched = false;
            for (int i = 0; i < standardKeys.length; i++) {
                if (standardKeys[i].equalsIgnoreCase(qKey)) {
                    if (standardChips[i] != null) {
                        standardChips[i].setVisibility(View.VISIBLE);
                    }
                    matched = true;
                    break;
                }
            }

            if (!matched && cgQualities != null) {
                TextView dynamicChip = createDynamicQualityChip(qKey);
                dynamicQualityChips.add(dynamicChip);
                cgQualities.addView(dynamicChip);
            }
        }

        boolean isSelectedAvailable = false;
        for (String qKey : sortedQualities) {
            if (selectedQuality != null && selectedQuality.equalsIgnoreCase(qKey + "p")) {
                isSelectedAvailable = true;
                break;
            }
        }

        if (!isSelectedAvailable && !sortedQualities.isEmpty()) {
            selectedQuality = sortedQualities.get(0) + "p";
        }

        updateQualityChipsUI();
    }

    private TextView createDynamicQualityChip(String qualityKey) {
        TextView chip = new TextView(requireContext());
        chip.setText(qualityKey + "p");
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color));
        chip.setBackgroundResource(R.drawable.chip_unselected);
        int pdH = (int) (16 * getResources().getDisplayMetrics().density);
        int pdV = (int) (7 * getResources().getDisplayMetrics().density);
        chip.setPadding(pdH, pdV, pdH, pdV);
        chip.setClickable(true);
        chip.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            selectedQuality = qualityKey + "p";
            updateQualityChipsUI();
        });
        return chip;
    }

    private void parseAllVoiceovers() {
        allVoiceovers.clear();
        Set<String> addedKeys = new HashSet<>();

        if (currentPlayers != null) {
            for (EpisodeResponse.PlayerData p : currentPlayers) {
                if (p.getTeam() != null) {
                    String pType = p.getPlayer() != null ? p.getPlayer().toLowerCase() : "animelib";
                    String title = p.getTeam().getName();
                    String key = p.getTeam().getId() + "_" + pType;
                    if (!addedKeys.contains(key)) {
                        addedKeys.add(key);
                        allVoiceovers.add(new VoiceoverOption(title, p.getTeam().getName(), p.getTeam().getId(), pType));
                    }
                }
            }
        }
    }

    private void setupVoiceoversList() {
        rvVoiceovers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVoiceovers.setNestedScrollingEnabled(false);
        voiceoverAdapter = new VoiceoverAdapter();
        rvVoiceovers.setAdapter(voiceoverAdapter);

        updateVoiceoversForSelectedPlayer();
    }

    private void updateVoiceoversForSelectedPlayer() {
        displayedVoiceovers.clear();

        for (VoiceoverOption vo : allVoiceovers) {
            if (vo.playerType.equalsIgnoreCase(selectedPlayerType) ||
               ("animelib".equalsIgnoreCase(selectedPlayerType) && vo.playerType.contains("animelib")) ||
               ("kodik".equalsIgnoreCase(selectedPlayerType) && vo.playerType.contains("kodik"))) {
                displayedVoiceovers.add(vo);
            }
        }

        if (displayedVoiceovers.isEmpty()) {
            // Default fallback for chosen player
            String defaultTeamName = "animelib".equalsIgnoreCase(selectedPlayerType) ? "Основная озвучка (AnimeLib)" : "Основная озвучка (Kodik)";
            displayedVoiceovers.add(new VoiceoverOption(defaultTeamName, defaultTeamName, 0, selectedPlayerType));
        }

        selectedVoiceover = displayedVoiceovers.get(0);
        if (voiceoverAdapter != null) {
            voiceoverAdapter.notifyDataSetChanged();
        }
        updateAvailableQualities();
    }

    private void setupEpisodesList() {
        checkItems.clear();
        if (episodeItems != null && !episodeItems.isEmpty()) {
            for (EpisodesListResponse.EpisodeItem ep : episodeItems) {
                checkItems.add(new EpisodeCheckItem(ep, true, true, false));
            }
        }

        rvEpisodes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEpisodes.setNestedScrollingEnabled(false);
        episodeAdapter = new EpisodeAdapter();
        rvEpisodes.setAdapter(episodeAdapter);
        updateDownloadButtonText();

        if ((episodeItems == null || episodeItems.isEmpty()) && animeId != null) {
            fetchEpisodesFromApi();
        }
    }

    private void fetchEpisodesFromApi() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        apiService.fetchEpisodesList(animeId, new ApiService.EpisodesCallback() {
            @Override
            public void onEpisodesReceived(EpisodesListResponse response) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        if (response != null && response.getData() != null) {
                            episodeItems = response.getData();
                            checkItems.clear();
                            for (EpisodesListResponse.EpisodeItem ep : episodeItems) {
                                checkItems.add(new EpisodeCheckItem(ep, true, true, false));
                            }
                            if (episodeAdapter != null) episodeAdapter.notifyDataSetChanged();
                            if (selectedVoiceover != null) {
                                checkVoiceoverAvailability(selectedVoiceover);
                            } else {
                                updateDownloadButtonText();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        showErrorAlertDialog("Ошибка загрузки озвучек", "Не удалось загрузить список серий и озвучек:\n" + error, () -> fetchEpisodesFromApi());
                    });
                }
            }
        });
    }

    private void showErrorAlertDialog(String title, String message, Runnable retryAction) {
        if (!isAdded() || getContext() == null) return;
        try {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_video_error, null);
            TextView titleTv = dialogView.findViewById(R.id.errorTitleText);
            TextView messageTv = dialogView.findViewById(R.id.errorMessageText);
            TextView detailsTv = dialogView.findViewById(R.id.errorPlayerDetailsText);
            com.google.android.material.button.MaterialButton retryBtn = dialogView.findViewById(R.id.retryButton);
            com.google.android.material.button.MaterialButton switchPlayerBtn = dialogView.findViewById(R.id.switchPlayerButton);
            ImageButton closeCrossBtn = dialogView.findViewById(R.id.closeErrorCrossButton);

            if (titleTv != null && title != null) titleTv.setText(title);
            if (messageTv != null && message != null) messageTv.setText(message);
            if (detailsTv != null) detailsTv.setVisibility(View.GONE);
            if (switchPlayerBtn != null) switchPlayerBtn.setVisibility(View.GONE);

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setView(dialogView);
            androidx.appcompat.app.AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            if (retryBtn != null) {
                retryBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (retryAction != null) {
                        retryAction.run();
                    }
                });
            }

            if (closeCrossBtn != null) {
                closeCrossBtn.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkVoiceoverAvailability(VoiceoverOption option) {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            for (EpisodeCheckItem item : checkItems) {
                String epNum = item.episode.getNumber();
                DownloadedEpisodeEntity downloaded = databaseManager.findDownloadedEpisode(animeId, epNum, option.teamName);
                item.isAlreadyDownloaded = (downloaded != null);
                item.isAvailable = true; // By default available
                item.isChecked = item.isAvailable && !item.isAlreadyDownloaded;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (episodeAdapter != null) episodeAdapter.notifyDataSetChanged();
                    updateDownloadButtonText();
                });
            }
        });
    }

    private void updateDownloadButtonText() {
        int selectedCount = 0;
        for (EpisodeCheckItem item : checkItems) {
            if (item.isAvailable && !item.isAlreadyDownloaded && item.isChecked) {
                selectedCount++;
            }
        }

        if (tvSelectedCounter != null) {
            tvSelectedCounter.setText(selectedCount + " / " + checkItems.size());
        }

        if (selectedCount > 0) {
            btnDownload.setText("Скачать (" + selectedCount + " серий)");
            btnDownload.setEnabled(true);
        } else {
            btnDownload.setText("Выберите серии");
            btnDownload.setEnabled(false);
        }
    }

    private void startDownloadQueue() {
        if (selectedVoiceover == null || selectedQuality == null) return;
        String qualityNum = selectedQuality.replace("p", "");

        ArrayList<DownloadTask> tasks = new ArrayList<>();
        for (EpisodeCheckItem item : checkItems) {
            if (item.isChecked && item.isAvailable && !item.isAlreadyDownloaded) {
                String epNum = item.episode.getNumber();
                String epName = "Серия " + epNum + (item.episode.getName() != null && !item.episode.getName().isEmpty() ? " - " + item.episode.getName() : "");
                DownloadTask task = new DownloadTask(
                        animeId,
                        animeTitle != null ? animeTitle : "Аниме #" + animeId,
                        posterUrl,
                        item.episode.getId(),
                        epNum,
                        epName,
                        selectedVoiceover.teamName,
                        selectedVoiceover.teamId,
                        selectedVoiceover.playerType,
                        qualityNum
                );
                tasks.add(task);
            }
        }

        if (tasks.isEmpty()) {
            Toast.makeText(requireContext(), "Не выбрано ни одной серии", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadService.startQueue(requireContext(), tasks);
        Toast.makeText(requireContext(), "Скачивание запущено в фоне (" + tasks.size() + " серий)", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private int getAttrColor(int attrRes, int defaultColor) {
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (requireContext().getTheme().resolveAttribute(attrRes, typedValue, true)) {
                return typedValue.data;
            }
        } catch (Exception ignored) {}
        return defaultColor;
    }

    // ================= ADAPTERS =================

    private class VoiceoverAdapter extends RecyclerView.Adapter<VoiceoverAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voiceover_choice, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VoiceoverOption vo = displayedVoiceovers.get(position);
            holder.tvVoiceoverName.setText(vo.teamName);
            holder.tvVoiceoverDetails.setText("Плеер: " + vo.playerType.toUpperCase());

            boolean isSelected = (selectedVoiceover != null && selectedVoiceover.equals(vo));
            if (isSelected) {
                holder.itemContainer.setBackgroundResource(R.drawable.episode_item_selected);
                holder.ivCheck.setVisibility(View.VISIBLE);
            } else {
                holder.itemContainer.setBackgroundResource(R.drawable.episode_item_normal);
                holder.ivCheck.setVisibility(View.GONE);
            }

            holder.itemContainer.setOnClickListener(v -> {
                selectedVoiceover = vo;
                notifyDataSetChanged();
                updateAvailableQualities();
            });
        }

        @Override
        public int getItemCount() {
            return displayedVoiceovers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View itemContainer;
            TextView tvVoiceoverName;
            TextView tvVoiceoverDetails;
            ImageView ivCheck;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemContainer = itemView.findViewById(R.id.itemContainer);
                tvVoiceoverName = itemView.findViewById(R.id.tvVoiceoverName);
                tvVoiceoverDetails = itemView.findViewById(R.id.tvVoiceoverDetails);
                ivCheck = itemView.findViewById(R.id.ivCheck);
            }
        }
    }

    private class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_episode, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EpisodeCheckItem item = checkItems.get(position);
            String title = item.episode.getNumber() + " Серия";
            if (item.episode.getName() != null && !item.episode.getName().isEmpty()) {
                title += " (" + item.episode.getName() + ")";
            }
            holder.tvEpisodeName.setText(title);

            if (item.isAlreadyDownloaded) {
                holder.tvStatus.setText("Уже скачано");
                holder.tvStatus.setTextColor(0xFF4CAF50); // green
                holder.cbEpisode.setEnabled(false);
                holder.cbEpisode.setChecked(false);
                holder.itemView.setBackgroundResource(R.drawable.episode_item_normal);
                holder.itemView.setAlpha(0.6f);
            } else if (!item.isAvailable) {
                holder.tvStatus.setText("Нет в этой озвучке");
                holder.tvStatus.setTextColor(0xFFF44336); // red
                holder.cbEpisode.setEnabled(false);
                holder.cbEpisode.setChecked(false);
                holder.itemView.setBackgroundResource(R.drawable.episode_item_normal);
                holder.itemView.setAlpha(0.5f);
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.cbEpisode.setEnabled(true);
                holder.cbEpisode.setChecked(item.isChecked);

                if (item.isChecked) {
                    holder.tvStatus.setText("Выбрано для скачивания");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
                    holder.itemView.setBackgroundResource(R.drawable.episode_item_selected);
                } else {
                    holder.tvStatus.setText("Доступно");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_text_color));
                    holder.itemView.setBackgroundResource(R.drawable.episode_item_normal);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.isAvailable && !item.isAlreadyDownloaded) {
                    item.isChecked = !item.isChecked;
                    notifyItemChanged(holder.getBindingAdapterPosition());
                    updateDownloadButtonText();
                }
            });
        }

        @Override
        public int getItemCount() {
            return checkItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View itemContainer;
            CheckBox cbEpisode;
            TextView tvEpisodeName;
            TextView tvStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemContainer = itemView.findViewById(R.id.itemContainer);
                cbEpisode = itemView.findViewById(R.id.cbEpisode);
                tvEpisodeName = itemView.findViewById(R.id.tvEpisodeName);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}
