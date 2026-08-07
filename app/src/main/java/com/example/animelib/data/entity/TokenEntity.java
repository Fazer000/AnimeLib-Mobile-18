package com.example.animelib.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity для хранения токенов авторизации
 */
@Entity(tableName = "tokens")
public class TokenEntity {
    @PrimaryKey
    private int id = 1; // Всегда один токен
    
    private String tokenType;
    private long expiresIn;
    private String accessToken;
    private String refreshToken;
    private long timestamp;
    private String userId;
    
    public TokenEntity() {}
    
    public TokenEntity(String tokenType, long expiresIn, String accessToken, String refreshToken, long timestamp, String userId) {
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    @androidx.room.Ignore
    public TokenEntity(String tokenType, long expiresIn, String accessToken, String refreshToken, long timestamp) {
        this(tokenType, expiresIn, accessToken, refreshToken, timestamp, null);
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
