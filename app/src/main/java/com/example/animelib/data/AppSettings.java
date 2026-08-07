package com.example.animelib.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettings {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String siteUrl;
    private boolean enable4K = false;
    private boolean enableAmbientLight = false;
    private boolean enableSurroundSound = true;
    private int surroundMode = 0; // 0 = Cinema 3D, 1 = Concert 3D, 2 = Voice 3D, 3 = Classic 5.1, 4 = Extreme 3D
    private float surroundSpatialWidth = 1.0f;
    private float surroundDialogueBoost = 1.0f;
    private float surroundBassBoost = 1.0f;
    private float surroundTrebleBoost = 1.0f;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int themeMode = 0; // 0 = light, 1 = dark, 2 = system

    public AppSettings() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public boolean isEnable4K() {
        return enable4K;
    }

    public void setEnable4K(boolean enable4K) {
        this.enable4K = enable4K;
    }

    public boolean isEnableAmbientLight() {
        return enableAmbientLight;
    }

    public void setEnableAmbientLight(boolean enableAmbientLight) {
        this.enableAmbientLight = enableAmbientLight;
    }

    public boolean isEnableSurroundSound() {
        return enableSurroundSound;
    }

    public void setEnableSurroundSound(boolean enableSurroundSound) {
        this.enableSurroundSound = enableSurroundSound;
    }

    public int getSurroundMode() {
        return surroundMode;
    }

    public void setSurroundMode(int surroundMode) {
        this.surroundMode = surroundMode;
    }

    public float getSurroundSpatialWidth() {
        return surroundSpatialWidth;
    }

    public void setSurroundSpatialWidth(float surroundSpatialWidth) {
        this.surroundSpatialWidth = surroundSpatialWidth;
    }

    public float getSurroundDialogueBoost() {
        return surroundDialogueBoost;
    }

    public void setSurroundDialogueBoost(float surroundDialogueBoost) {
        this.surroundDialogueBoost = surroundDialogueBoost;
    }

    public float getSurroundBassBoost() {
        return surroundBassBoost;
    }

    public void setSurroundBassBoost(float surroundBassBoost) {
        this.surroundBassBoost = surroundBassBoost;
    }

    public float getSurroundTrebleBoost() {
        return surroundTrebleBoost;
    }

    public void setSurroundTrebleBoost(float surroundTrebleBoost) {
        this.surroundTrebleBoost = surroundTrebleBoost;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public int getLongSkipDuration() {
        return longSkipDuration;
    }

    public void setLongSkipDuration(int longSkipDuration) {
        this.longSkipDuration = longSkipDuration;
    }

    public int getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(int themeMode) {
        this.themeMode = themeMode;
    }
}
