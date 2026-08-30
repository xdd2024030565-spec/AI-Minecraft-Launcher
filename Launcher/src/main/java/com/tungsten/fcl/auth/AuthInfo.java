package com.tungsten.fcl.auth;

import java.util.UUID;

/**
 * 认证信息
 *
 * 存储 Minecraft 账户的认证信息，包括访问令牌、玩家名称、UUID 等。
 */
public class AuthInfo {

    private String username;
    private String accessToken;
    private String refreshToken;
    private UUID uuid;
    private String xuid;
    private String userInfo;

    public AuthInfo() {
    }

    public AuthInfo(String username, String accessToken, UUID uuid) {
        this.username = username;
        this.accessToken = accessToken;
        this.uuid = uuid;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UUID getUUID() { return uuid; }
    public void setUUID(UUID uuid) { this.uuid = uuid; }

    public String getXuid() { return xuid; }
    public void setXuid(String xuid) { this.xuid = xuid; }

    public String getUserInfo() { return userInfo; }
    public void setUserInfo(String userInfo) { this.userInfo = userInfo; }
}