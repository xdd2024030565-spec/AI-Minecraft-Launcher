package com.tungsten.fcl.download;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BMCLAPI 下载提供者 — 仿 FCL BMCLAPIDownloadProvider
 *
 * 使用 BMCLAPI 镜像源加速下载，适合中国大陆用户。
 * 同时提供 Modrinth 和 CurseForge 镜像回退。
 */
public class BMCLAPIDownloadProvider implements DownloadProvider {

    private static final String BMCLAPI_ROOT = "https://bmclapi2.bangbang93.com";
    private static final String MODRINTH_MIRROR = "https://mod.mcimirror.top/modrinth";
    private static final String CURSEFORGE_MIRROR = "https://mod.mcimirror.top/curseforge";

    @Override
    public List<URL> getVersionListURLs() {
        try {
            return Collections.singletonList(new URL(BMCLAPI_ROOT + "/mc/game/version_manifest_v2.json"));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public String injectURL(String baseURL) {
        // Mojang 官方 → BMCLAPI 镜像
        String result = baseURL
                .replace("https://launchermeta.mojang.com", BMCLAPI_ROOT)
                .replace("https://piston-meta.mojang.com", BMCLAPI_ROOT)
                .replace("https://libraries.minecraft.net", BMCLAPI_ROOT + "/maven")
                .replace("https://resources.download.minecraft.net", BMCLAPI_ROOT + "/assets");

        // Modrinth 镜像
        result = result.replace("https://api.modrinth.com", MODRINTH_MIRROR)
                .replace("https://cdn.modrinth.com", "https://mod.mcimirror.top");

        // CurseForge 镜像
        result = result.replace("https://api.curseforge.com", CURSEFORGE_MIRROR)
                .replace("https://edge.forgecdn.net", "https://mod.mcimirror.top");

        return result;
    }

    @Override
    public List<URL> getAssetObjectCandidates(String assetObjectLocation) {
        try {
            return Collections.singletonList(
                    new URL(BMCLAPI_ROOT + "/assets/" + assetObjectLocation));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return "BMCLAPI 镜像";
    }
}
