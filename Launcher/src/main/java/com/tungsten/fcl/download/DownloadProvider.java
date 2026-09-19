package com.tungsten.fcl.download;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * 下载提供者接口 — 仿 FCL DownloadProvider
 *
 * 提供版本清单URL、资源文件URL注入等功能。
 * 支持官方源和BMCLAPI镜像源切换。
 */
public interface DownloadProvider {

    /**
     * 获取版本清单 URL 列表 (多候选源)
     */
    List<URL> getVersionListURLs();

    /**
     * 注入/替换 URL — 将 Mojang 原始 URL 替换为镜像源
     */
    String injectURL(String baseURL);

    /**
     * 注入 URL 并返回候选列表
     */
    default List<URL> injectURLWithCandidates(String baseURL) {
        try {
            return Collections.singletonList(new URL(injectURL(baseURL)));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取资源文件下载候选 URL
     * @param assetObjectLocation 资源路径 (如 aa/bb/aabb...hash)
     */
    default List<URL> getAssetObjectCandidates(String assetObjectLocation) {
        try {
            return Collections.singletonList(new URL("https://resources.download.minecraft.net/" + assetObjectLocation));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取显示名称
     */
    String getName();

    /**
     * 最大并发下载数
     */
    default int getConcurrency() {
        return 8;
    }
}
