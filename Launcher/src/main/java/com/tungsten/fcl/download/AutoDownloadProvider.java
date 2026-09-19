package com.tungsten.fcl.download;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 自动下载提供者 — 仿 FCL AutoDownloadProvider
 *
 * 聚合多个下载源，优先使用镜像源，失败时回退到官方源。
 * 提供多候选 URL 列表。
 */
public class AutoDownloadProvider implements DownloadProvider {

    private final MojangDownloadProvider mojang = new MojangDownloadProvider();
    private final BMCLAPIDownloadProvider bmclapi = new BMCLAPIDownloadProvider();
    private boolean preferBMCLAPI = true;

    public AutoDownloadProvider(boolean preferBMCLAPI) {
        this.preferBMCLAPI = preferBMCLAPI;
    }

    public AutoDownloadProvider() {
        this(true);
    }

    @Override
    public List<URL> getVersionListURLs() {
        LinkedHashSet<URL> urls = new LinkedHashSet<>();
        if (preferBMCLAPI) {
            urls.addAll(bmclapi.getVersionListURLs());
            urls.addAll(mojang.getVersionListURLs());
        } else {
            urls.addAll(mojang.getVersionListURLs());
            urls.addAll(bmclapi.getVersionListURLs());
        }
        return new ArrayList<>(urls);
    }

    @Override
    public String injectURL(String baseURL) {
        return preferBMCLAPI ? bmclapi.injectURL(baseURL) : mojang.injectURL(baseURL);
    }

    @Override
    public List<URL> injectURLWithCandidates(String baseURL) {
        LinkedHashSet<URL> result = new LinkedHashSet<>();
        // 先尝试镜像源
        result.addAll(bmclapi.injectURLWithCandidates(baseURL));
        // 回退到官方源
        result.addAll(mojang.injectURLWithCandidates(baseURL));
        return new ArrayList<>(result);
    }

    @Override
    public List<URL> getAssetObjectCandidates(String assetObjectLocation) {
        LinkedHashSet<URL> result = new LinkedHashSet<>();
        if (preferBMCLAPI) {
            result.addAll(bmclapi.getAssetObjectCandidates(assetObjectLocation));
            result.addAll(mojang.getAssetObjectCandidates(assetObjectLocation));
        } else {
            result.addAll(mojang.getAssetObjectCandidates(assetObjectLocation));
            result.addAll(bmclapi.getAssetObjectCandidates(assetObjectLocation));
        }
        return new ArrayList<>(result);
    }

    @Override
    public String getName() {
        return "自动 (BMCLAPI 优先)";
    }
}
