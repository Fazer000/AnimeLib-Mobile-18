package com.example.animelib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animelib.R;
import com.example.animelib.models.BookmarksListResponse;
import com.example.animelib.util.ImageLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookmarksViewPagerAdapter extends RecyclerView.Adapter<BookmarksViewPagerAdapter.BookmarkViewHolder> {
    private final List<BookmarksListResponse.BookmarkItem> bookmarksList = new ArrayList<>();
    private OnBookmarkClickListener clickListener;
    private OnBookmarkDeleteListener deleteListener;
    private boolean isLoading = false;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(BookmarksListResponse.BookmarkItem bookmark);
    }

    public interface OnBookmarkDeleteListener {
        void onBookmarkDelete(BookmarksListResponse.BookmarkItem bookmark, int remainingCount);
    }

    public BookmarksViewPagerAdapter(BookmarksListResponse.BookmarkItem[] bookmarks, OnBookmarkClickListener clickListener) {
        if (bookmarks != null) {
            this.bookmarksList.addAll(Arrays.asList(bookmarks));
        }
        this.clickListener = clickListener;
        this.isLoading = false;
    }

    public BookmarksViewPagerAdapter(BookmarksListResponse.BookmarkItem[] bookmarks, OnBookmarkClickListener clickListener, OnBookmarkDeleteListener deleteListener) {
        if (bookmarks != null) {
            this.bookmarksList.addAll(Arrays.asList(bookmarks));
        }
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.isLoading = false;
    }

    public BookmarksViewPagerAdapter(boolean isLoading, OnBookmarkClickListener clickListener) {
        this.clickListener = clickListener;
        this.isLoading = isLoading;
    }

    public BookmarksViewPagerAdapter(boolean isLoading, OnBookmarkClickListener clickListener, OnBookmarkDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.isLoading = isLoading;
    }

    public void setBookmarks(BookmarksListResponse.BookmarkItem[] bookmarks) {
        this.bookmarksList.clear();
        if (bookmarks != null) {
            this.bookmarksList.addAll(Arrays.asList(bookmarks));
        }
        this.isLoading = false;
        notifyDataSetChanged();
    }

    public void setBookmarks(List<BookmarksListResponse.BookmarkItem> bookmarks) {
        this.bookmarksList.clear();
        if (bookmarks != null) {
            this.bookmarksList.addAll(bookmarks);
        }
        this.isLoading = false;
        notifyDataSetChanged();
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    public int getBookmarkCount() {
        return bookmarksList.size();
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark_wrapper, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        if (isLoading) {
            holder.bindLoading();
        } else if (position >= 0 && position < bookmarksList.size()) {
            BookmarksListResponse.BookmarkItem bookmark = bookmarksList.get(position);
            holder.bind(bookmark);
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading) {
            return 1;
        }
        return bookmarksList.size();
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private final ImageView animeCover;
        private final TextView animeTitle;
        private final TextView episodeInfo;
        private final ProgressBar progressBar;
        private final View btnDelete;

        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            animeCover = itemView.findViewById(R.id.anime_cover);
            animeTitle = itemView.findViewById(R.id.anime_title);
            episodeInfo = itemView.findViewById(R.id.episode_info);
            progressBar = itemView.findViewById(R.id.bookmark_item_progress_bar);
            btnDelete = null;

            View.OnClickListener clicker = v -> {
                int position = getAdapterPosition();
                if (!isLoading && position != RecyclerView.NO_POSITION && clickListener != null && position < bookmarksList.size()) {
                    clickListener.onBookmarkClick(bookmarksList.get(position));
                }
            };

            itemView.setOnClickListener(clicker);
            View card = itemView.findViewById(R.id.bookmark_item_card);
            if (card != null) {
                card.setOnClickListener(clicker);
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (!isLoading && position != RecyclerView.NO_POSITION && position < bookmarksList.size()) {
                        BookmarksListResponse.BookmarkItem removedItem = bookmarksList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, bookmarksList.size());
                        if (deleteListener != null) {
                            deleteListener.onBookmarkDelete(removedItem, bookmarksList.size());
                        }
                    }
                });
            }
        }

        public void bindLoading() {
            if (animeTitle != null) animeTitle.setText("Загрузка закладок...");
            if (episodeInfo != null) episodeInfo.setText("Пожалуйста, подождите...");
            if (progressBar != null) {
                progressBar.setMax(100);
                progressBar.setProgress(0);
            }
            if (animeCover != null) animeCover.setImageResource(R.drawable.placeholder_image);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }

        public void bind(BookmarksListResponse.BookmarkItem bookmark) {
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            if (bookmark == null) return;

            // Название аниме
            if (animeTitle != null) {
                String title = null;
                if (bookmark.getMedia() != null) {
                    title = bookmark.getMedia().getRusName();
                    if (title == null || title.isEmpty()) {
                        title = bookmark.getMedia().getName();
                    }
                    if (title == null || title.isEmpty()) {
                        title = bookmark.getMedia().getEngName();
                    }
                }
                animeTitle.setText(title != null && !title.isEmpty() ? title : "Неизвестное аниме");
            }

            // Номер эпизода и прогресс
            String episodeNum = "1";
            if (bookmark.getItem() != null && bookmark.getItem().getNumber() != null) {
                episodeNum = bookmark.getItem().getNumber();
            }

            bindProgressData(episodeNum, bookmark.getProgress());

            // Обложка
            if (animeCover != null) {
                String coverUrl = null;
                if (bookmark.getMedia() != null && bookmark.getMedia().getCover() != null) {
                    coverUrl = bookmark.getMedia().getCover().getThumbnail();
                }
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    ImageLoader.getInstance().loadInto(animeCover, coverUrl, R.drawable.placeholder_image);
                } else {
                    animeCover.setImageResource(R.drawable.placeholder_image);
                }
            }
        }

        private void bindProgressData(String episodeNum, String rawProgress) {
            int percent = 0;
            String progressText = "";

            if (rawProgress != null && !rawProgress.trim().isEmpty()) {
                String cleaned = rawProgress.trim();
                if (cleaned.contains("из") || cleaned.contains("/") || cleaned.contains("of")) {
                    String[] parts = cleaned.split("из|/|of");
                    if (parts.length >= 2) {
                        String currentStr = parts[0].trim();
                        String totalStr = parts[1].trim();
                        long currentSec = parseSeconds(currentStr);
                        long totalSec = parseSeconds(totalStr);
                        if (totalSec > 0) {
                            percent = (int) Math.min(100, Math.max(0, (currentSec * 100L) / totalSec));
                        }
                        progressText = currentStr + " из " + totalStr;
                    } else {
                        progressText = cleaned;
                    }
                } else {
                    long currentSec = parseSeconds(cleaned);
                    if (currentSec > 0) {
                        // По умолчанию для аниме эпизода принимаем ~24 мин (1440 сек)
                        long defaultTotal = 1440L;
                        percent = (int) Math.min(100, Math.max(0, (currentSec * 100L) / defaultTotal));
                        if (percent < 5) percent = 5;
                        progressText = cleaned;
                    } else {
                        progressText = cleaned;
                    }
                }
            } else {
                progressText = "00:00";
                percent = 0;
            }

            if (episodeInfo != null) {
                episodeInfo.setText("Эпизод " + episodeNum + " — " + progressText);
            }

            if (progressBar != null) {
                progressBar.setMax(100);
                progressBar.setProgress(percent);
            }
        }

        private long parseSeconds(String timeStr) {
            if (timeStr == null || timeStr.isEmpty()) return 0;
            try {
                String clean = timeStr.replaceAll("[^0-9:]", "");
                String[] parts = clean.split(":");
                if (parts.length == 2) {
                    long min = Long.parseLong(parts[0]);
                    long sec = Long.parseLong(parts[1]);
                    return min * 60L + sec;
                } else if (parts.length == 3) {
                    long hr = Long.parseLong(parts[0]);
                    long min = Long.parseLong(parts[1]);
                    long sec = Long.parseLong(parts[2]);
                    return hr * 3600L + min * 60L + sec;
                }
            } catch (Exception ignored) {
            }
            return 0;
        }
    }
}
