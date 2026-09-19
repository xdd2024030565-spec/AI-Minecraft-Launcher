package com.tungsten.fclcore.game;

/**
 * 版本未找到异常 — 直接取自 FCL VersionNotFoundException
 */
public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException() {}
    public VersionNotFoundException(String message) { super(message); }
}
