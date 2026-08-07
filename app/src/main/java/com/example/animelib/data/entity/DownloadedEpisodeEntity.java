package com.example.animelib.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_episodes")
public class DownloadedEpisodeEntity {
    @PrimaryKey
    @NonNull
    private String id; // animeId_episodeNumber_teamName
    private String animeId;
    private String animeTitle;
    private String episodeNumber;
    private String episodeName;
    private String teamName;
    private String playerType; // animelib / kodik
    private String localFilePath;
    private long fileSize;
    private long downloadedAt;
    private String quality;

    public DownloadedEpisodeEntity(@NonNull String id, String animeId, String animeTitle,
                                   String episodeNumber, String episodeName, String teamName,
                                   String playerType, String localFilePath, long fileSize,
                                   long downloadedAt, String quality) {
        this.id = id;
        this.animeId = animeId;
        this.animeTitle = animeTitle;
        this.episodeNumber = episodeNumber;
        this.episodeName = episodeName;
        this.teamName = teamName;
        this.playerType = playerType;
        this.localFilePath = localFilePath;
        this.fileSize = fileSize;
        this.downloadedAt = downloadedAt;
        this.quality = quality;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getAnimeTitle() { return animeTitle; }
    public void setAnimeTitle(String animeTitle) { this.animeTitle = animeTitle; }

    public String getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(String episodeNumber) { this.episodeNumber = episodeNumber; }

    public String getEpisodeName() { return episodeName; }
    public void setEpisodeName(String episodeName) { this.episodeName = episodeName; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getPlayerType() { return playerType; }
    public void setPlayerType(String playerType) { this.playerType = playerType; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(long downloadedAt) { this.downloadedAt = downloadedAt; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}
