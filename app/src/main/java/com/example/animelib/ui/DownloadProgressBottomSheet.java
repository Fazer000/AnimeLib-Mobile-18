package com.example.animelib.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.DownloadTask;
import com.example.animelib.services.DownloadService;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DownloadProgressBottomSheet extends BottomSheetDialogFragment implements DownloadService.QueueProgressListener {

    private ImageButton btnProgressClose;
    private ImageButton btnHeaderOpenDownloadSheet;
    private ImageView ivProgressHeaderIcon;
    private TextView tvProgressHeaderTitle;
    private TextView tvProgressHeaderSubtitle;
    private TextView tvOverallText;
    private TextView tvOverallPercent;
    private ProgressBar pbOverall;
    private RecyclerView rvProgressTasks;
    private MaterialButton btnOpenDownloadSheet;
    private MaterialButton btnGoToDownloads;
    private MaterialButton btnStopDownload;

    private final List<DownloadService.TaskProgressItem> taskItems = new ArrayList<>();
    private TaskProgressAdapter adapter;

    public static DownloadProgressBottomSheet newInstance() {
        return new DownloadProgressBottomSheet();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_download_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() instanceof BottomSheetDialog) {
            com.example.animelib.util.FloatingBottomSheetUtils.setupFloatingStyle((BottomSheetDialog) getDialog());
        }

        btnProgressClose = view.findViewById(R.id.btnProgressClose);
        btnHeaderOpenDownloadSheet = view.findViewById(R.id.btnHeaderOpenDownloadSheet);
        ivProgressHeaderIcon = view.findViewById(R.id.ivProgressHeaderIcon);
        tvProgressHeaderTitle = view.findViewById(R.id.tvProgressHeaderTitle);
        tvProgressHeaderSubtitle = view.findViewById(R.id.tvProgressHeaderSubtitle);
        tvOverallText = view.findViewById(R.id.tvOverallText);
        tvOverallPercent = view.findViewById(R.id.tvOverallPercent);
        pbOverall = view.findViewById(R.id.pbOverall);
        rvProgressTasks = view.findViewById(R.id.rvProgressTasks);
        btnOpenDownloadSheet = view.findViewById(R.id.btnOpenDownloadSheet);
        btnGoToDownloads = view.findViewById(R.id.btnGoToDownloads);
        btnStopDownload = view.findViewById(R.id.btnStopDownload);

        if (btnProgressClose != null) {
            btnProgressClose.setOnClickListener(v -> dismiss());
        }

        View.OnClickListener openDownloadSheetListener = v -> {
            dismiss();
            if (getActivity() instanceof com.example.animelib.VideoPlayerActivity) {
                ((com.example.animelib.VideoPlayerActivity) getActivity()).showDownloadBottomSheet();
            }
        };

        if (btnHeaderOpenDownloadSheet != null) {
            btnHeaderOpenDownloadSheet.setOnClickListener(openDownloadSheetListener);
        }

        if (btnOpenDownloadSheet != null) {
            btnOpenDownloadSheet.setOnClickListener(openDownloadSheetListener);
        }

        if (btnGoToDownloads != null) {
            btnGoToDownloads.setOnClickListener(v -> {
                dismiss();
                DownloadsActivity.start(requireContext());
            });
        }

        rvProgressTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProgressTasks.setNestedScrollingEnabled(false);
        adapter = new TaskProgressAdapter();
        rvProgressTasks.setAdapter(adapter);

        btnStopDownload.setOnClickListener(v -> {
            if (DownloadService.isRunning()) {
                DownloadService.cancel(requireContext());
                Toast.makeText(requireContext(), "Скачивание остановлено", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                dismiss();
            }
        });

        DownloadService.setQueueProgressListener(this);
        updateProgressData();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            com.example.animelib.util.FloatingBottomSheetUtils.applyFloatingToView((BottomSheetDialog) getDialog());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DownloadService.setQueueProgressListener(this);
        updateProgressData();
    }

    @Override
    public void onPause() {
        super.onPause();
        DownloadService.setQueueProgressListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DownloadService.setQueueProgressListener(null);
    }

    @Override
    public void onQueueUpdated() {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(this::updateProgressData);
        }
    }

    private void updateProgressData() {
        List<DownloadService.TaskProgressItem> activeItems = DownloadService.getActiveTaskItems();
        taskItems.clear();
        taskItems.addAll(activeItems);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        int total = taskItems.size();
        int completed = 0;
        int activeDownloadingPercent = 0;

        for (DownloadService.TaskProgressItem item : taskItems) {
            if (item.status == DownloadService.TaskProgressItem.STATUS_COMPLETED) {
                completed++;
            } else if (item.status == DownloadService.TaskProgressItem.STATUS_DOWNLOADING) {
                activeDownloadingPercent = item.percent;
            }
        }

        boolean isRunning = DownloadService.isRunning();

        if (total == 0) {
            tvProgressHeaderSubtitle.setText("Нет активных скачиваний");
            tvOverallText.setText("Очередь пуста");
            tvOverallPercent.setText("0%");
            pbOverall.setProgress(0);
            btnStopDownload.setText("Закрыть");
            return;
        }

        int overallProgress = (int) (((completed * 100.0) + activeDownloadingPercent) / total);
        if (overallProgress > 100) overallProgress = 100;

        String animeName = !taskItems.isEmpty() && taskItems.get(0).task.getAnimeTitle() != null ?
                taskItems.get(0).task.getAnimeTitle() : "";

        if (isRunning) {
            tvProgressHeaderSubtitle.setText(animeName.isEmpty() ? "Загрузка серий..." : animeName);
            tvOverallText.setText("Обработано " + completed + " из " + total + " серий");
            tvOverallPercent.setText(overallProgress + "%");
            pbOverall.setProgress(overallProgress);
            btnStopDownload.setText("Остановить скачивание");
        } else {
            tvProgressHeaderSubtitle.setText("Завершено");
            tvOverallText.setText("Обработано " + completed + " из " + total + " серий");
            tvOverallPercent.setText(overallProgress + "%");
            pbOverall.setProgress(overallProgress);
            btnStopDownload.setText("Закрыть");
        }
    }

    private class TaskProgressAdapter extends RecyclerView.Adapter<TaskProgressAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_progress_task, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DownloadService.TaskProgressItem item = taskItems.get(position);
            DownloadTask task = item.task;

            holder.tvTaskTitle.setText("Серия " + task.getEpisodeNumber() + (task.getEpisodeName() != null && !task.getEpisodeName().isEmpty() ? " - " + task.getEpisodeName() : ""));
            holder.tvTaskSubtitle.setText(task.getTeamName() + " • " + task.getQuality() + "p");

            int secondaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color);
            int accentColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_text_color);

            switch (item.status) {
                case DownloadService.TaskProgressItem.STATUS_DOWNLOADING:
                    holder.itemContainer.setBackgroundResource(R.drawable.episode_item_selected);
                    holder.ivStatusIcon.setVisibility(View.VISIBLE);
                    holder.ivStatusIcon.setImageResource(R.drawable.ic_download);
                    holder.ivStatusIcon.setColorFilter(secondaryColor);
                    holder.pbStatusLoading.setVisibility(View.GONE);
                    holder.pbItemProgress.setVisibility(View.VISIBLE);
                    holder.pbItemProgress.setProgress(item.percent);
                    holder.tvPercent.setText(item.percent + "%");
                    holder.tvPercent.setTextColor(secondaryColor);
                    break;

                case DownloadService.TaskProgressItem.STATUS_COMPLETED:
                    holder.itemContainer.setBackgroundResource(R.drawable.episode_item_normal);
                    holder.ivStatusIcon.setVisibility(View.VISIBLE);
                    holder.ivStatusIcon.setImageResource(R.drawable.ic_check);
                    holder.ivStatusIcon.setColorFilter(0xFF10B981);
                    holder.pbStatusLoading.setVisibility(View.GONE);
                    holder.pbItemProgress.setVisibility(View.GONE);
                    holder.tvPercent.setText("Готово");
                    holder.tvPercent.setTextColor(0xFF10B981);
                    break;

                case DownloadService.TaskProgressItem.STATUS_ERROR:
                    holder.itemContainer.setBackgroundResource(R.drawable.episode_item_normal);
                    holder.ivStatusIcon.setVisibility(View.VISIBLE);
                    holder.ivStatusIcon.setImageResource(R.drawable.ic_close);
                    holder.ivStatusIcon.setColorFilter(0xFFEF4444);
                    holder.pbStatusLoading.setVisibility(View.GONE);
                    holder.pbItemProgress.setVisibility(View.GONE);
                    holder.tvPercent.setText("Ошибка");
                    holder.tvPercent.setTextColor(0xFFEF4444);
                    break;

                case DownloadService.TaskProgressItem.STATUS_WAITING:
                default:
                    holder.itemContainer.setBackgroundResource(R.drawable.episode_item_normal);
                    holder.ivStatusIcon.setVisibility(View.VISIBLE);
                    holder.ivStatusIcon.setImageResource(R.drawable.ic_download);
                    holder.ivStatusIcon.setColorFilter(accentColor);
                    holder.pbStatusLoading.setVisibility(View.GONE);
                    holder.pbItemProgress.setVisibility(View.GONE);
                    holder.tvPercent.setText("В очереди");
                    holder.tvPercent.setTextColor(accentColor);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return taskItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View itemContainer;
            ImageView ivStatusIcon;
            ProgressBar pbStatusLoading;
            TextView tvTaskTitle;
            TextView tvTaskSubtitle;
            ProgressBar pbItemProgress;
            TextView tvPercent;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemContainer = itemView.findViewById(R.id.itemContainer);
                ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
                pbStatusLoading = itemView.findViewById(R.id.pbStatusLoading);
                tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvTaskSubtitle = itemView.findViewById(R.id.tvTaskSubtitle);
                pbItemProgress = itemView.findViewById(R.id.pbItemProgress);
                tvPercent = itemView.findViewById(R.id.tvPercent);
            }
        }
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
}
