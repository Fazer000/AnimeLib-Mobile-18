package com.example.animelib.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_anime")
public class DownloadedAnimeEntity {
    @PrimaryKey
    @NonNull
    private String animeId;
    private String title;
    private String posterUrl;
    private long savedAt;

    public DownloadedAnimeEntity(@NonNull String animeId, String title, String posterUrl, long savedAt) {
        this.animeId = animeId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.savedAt = savedAt;
    }

    @NonNull
    public String getAnimeId() { return animeId; }
    public void setAnimeId(@NonNull String animeId) { this.animeId = animeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }
}
