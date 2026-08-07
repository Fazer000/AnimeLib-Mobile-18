package com.example.animelib.models;

import java.io.Serializable;

public class DownloadTask implements Serializable {
    private String animeId;
    private String animeTitle;
    private String posterUrl;
    private int episodeId;
    private String episodeNumber;
    private String episodeName;
    private String teamName;
    private int teamId;
    private String playerType; // "animelib" or "kodik"
    private String quality; // "1080", "720", "480", "360"

    public DownloadTask(String animeId, String animeTitle, String posterUrl, int episodeId,
                        String episodeNumber, String episodeName, String teamName, int teamId,
                        String playerType, String quality) {
        this.animeId = animeId;
        this.animeTitle = animeTitle;
        this.posterUrl = posterUrl;
        this.episodeId = episodeId;
        this.episodeNumber = episodeNumber;
        this.episodeName = episodeName;
        this.teamName = teamName;
        this.teamId = teamId;
        this.playerType = playerType;
        this.quality = quality;
    }

    public String getAnimeId() { return animeId; }
    public String getAnimeTitle() { return animeTitle; }
    public String getPosterUrl() { return posterUrl; }
    public int getEpisodeId() { return episodeId; }
    public String getEpisodeNumber() { return episodeNumber; }
    public String getEpisodeName() { return episodeName; }
    public String getTeamName() { return teamName; }
    public int getTeamId() { return teamId; }
    public String getPlayerType() { return playerType; }
    public String getQuality() { return quality; }
}
