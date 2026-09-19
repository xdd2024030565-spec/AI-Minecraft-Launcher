package com.tungsten.fcl.download;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Mojang 官方下载提供者 — 仿 FCL MojangDownloadProvider
 *
 * 直接使用 Mojang 官方源，部分地区可能较慢。
 * OptiFine 下载回退到 BMCLAPI。
 */
public class MojangDownloadProvider implements DownloadProvider {

    private static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private static final String ASSET_BASE = "https://resources.download.minecraft.net/";

    private static final String BMCLAPI_ROOT = "https://bmclapi2.bangbang93.com";

    @Override
    public List<URL> getVersionListURLs() {
        try {
            return Collections.singletonList(new URL(VERSION_MANIFEST_URL));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public String injectURL(String baseURL) {
        // Mojang 官方源不替换，但部分库 URL 可以用 BMCLAPI 加速
        if (baseURL.contains("launchermeta") || baseURL.contains("piston-meta")
                || baseURL.contains("libraries.minecraft.net")) {
            // 可选 BMCLAPI 加速
            String result = baseURL
                    .replace("https://launchermeta.mojang.com", BMCLAPI_ROOT)
                    .replace("https://piston-meta.mojang.com", BMCLAPI_ROOT)
                    .replace("https://libraries.minecraft.net", BMCLAPI_ROOT + "/maven");
            return result;
        }
        return baseURL;
    }

    @Override
    public List<URL> getAssetObjectCandidates(String assetObjectLocation) {
        try {
            return Collections.singletonList(new URL(ASSET_BASE + assetObjectLocation));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return "Mojang 官方";
    }
}
