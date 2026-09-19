package com.tungsten.fclcore.game;

/**
 * 版本提供者 — 直接取自 FCL VersionProvider
 */
public interface VersionProvider {

    /**
     * 版本是否存在
     */
    boolean hasVersion(String id);

    /**
     * 获取版本
     */
    Version getVersion(String id) throws VersionNotFoundException;
}
