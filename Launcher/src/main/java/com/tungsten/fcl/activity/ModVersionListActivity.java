package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.game.VersionManager;
import com.tungsten.fcl.mod.ModDownloadManager;
import com.tungsten.fcl.mod.ModrinthApi;
import com.tungsten.fcl.setting.LauncherSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Mod 版本选择页面 — 对齐 FCL RemoteModVersionPage
 *
 * 从搜索列表点入，展示某个 Mod 的所有版本，选择后安装到目标游戏版本。
 * 支持 Modrinth 依赖自动下载。
 */
public class ModVersionListActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_GAME_VERSION = "game_version";

    private FCLRepository repository;
    private ModDownloadManager modDownloadManager;
    private LauncherSettings settings;

    private String projectId;
    private String title;
    private String sourceName;
    private String gameVersion;

    private LinearLayout versionListContainer;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private TextView tvTargetInfo;

    private String targetVersionId;
    private final List<ModrinthApi.ModVersion> versions = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        settings = LauncherSettings.getInstance(this);
        modDownloadManager = new ModDownloadManager(repository, this);

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        sourceName = getIntent().getStringExtra(EXTRA_SOURCE);
        gameVersion = getIntent().getStringExtra(EXTRA_GAME_VERSION);

        if (projectId == null) { finish(); return; }

        setupUI();
        loadVersions();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title == null ? "版本列表" : title);
        tvTitle.setTextSize(20);
        tvTitle.setPadding(0, 16, 0, 8);
        root.addView(tvTitle);

        TextView tvSource = new TextView(this);
        tvSource.setText("来源: " + (sourceName == null ? "Modrinth" : sourceName));
        tvSource.setTextSize(12);
        tvSource.setTextColor(0xFF888888);
        root.addView(tvSource);

        // 目标游戏版本选择
        LinearLayout targetBox = new LinearLayout(this);
        targetBox.setOrientation(LinearLayout.VERTICAL);
        targetBox.setPadding(0, 16, 0, 16);

        TextView tvTargetLabel = new TextView(this);
        tvTargetLabel.setText("安装到哪个游戏版本?");
        tvTargetLabel.setTextSize(14);
        tvTargetLabel.setTypeface(tvTargetLabel.getTypeface(), android.graphics.Typeface.BOLD);
        targetBox.addView(tvTargetLabel);

        tvTargetInfo = new TextView(this);
        tvTargetInfo.setTextSize(13);
        tvTargetInfo.setTextColor(0xFF666666);
        tvTargetInfo.setPadding(0, 8, 0, 0);
        targetBox.addView(tvTargetInfo);

        Button btnSelectTarget = new MaterialButton(this);
        btnSelectTarget.setText("选择目标游戏版本");
        btnSelectTarget.setOnClickListener(v -> showTargetPicker());
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnP.topMargin = 8;
        targetBox.addView(btnSelectTarget, btnP);

        root.addView(targetBox);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        tvStatus = new TextView(this);
        tvStatus.setText("正在加载版本列表...");
        tvStatus.setPadding(0, 8, 0, 16);
        root.addView(tvStatus);

        versionListContainer = new LinearLayout(this);
        versionListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(versionListContainer);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void loadVersions() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                List<ModrinthApi.ModVersion> list = modDownloadManager.getModrinthApi()
                        .getVersions(projectId, gameVersion, null);
                if (list.isEmpty()) {
                    list = modDownloadManager.getModrinthApi().getVersions(projectId);
                }
                final List<ModrinthApi.ModVersion> result = list;
                mainHandler.post(() -> {
                    versions.clear();
                    versions.addAll(result);
                    progressBar.setVisibility(View.GONE);
                    renderVersions();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("加载失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderVersions() {
        versionListContainer.removeAllViews();
        if (versions.isEmpty()) {
            tvStatus.setText("没有可用版本");
            return;
        }
        tvStatus.setText("共 " + versions.size() + " 个版本，点击安装");

        for (ModrinthApi.ModVersion v : versions) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(24, 20, 24, 20);

            String type = v.versionType == null ? "" : v.versionType;
            String typeTag = "release".equals(type) ? "[正式]" : ("beta".equals(type) ? "[测试]" : "[预览]");
            String vName = (v.versionTitle != null && !v.versionTitle.isEmpty()) ? v.versionTitle : v.name;

            TextView tvName = new TextView(this);
            tvName.setText(typeTag + " " + vName);
            tvName.setTextSize(16);
            tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
            item.addView(tvName);

            StringBuilder meta = new StringBuilder();
            if (!v.gameVersions.isEmpty()) {
                meta.append("MC: ").append(join(v.gameVersions, ", ")).append("\n");
            }
            if (!v.loaders.isEmpty()) {
                meta.append("加载器: ").append(join(v.loaders, ", "));
            }

            TextView tvMeta = new TextView(this);
            tvMeta.setText(meta.toString());
            tvMeta.setTextSize(12);
            tvMeta.setTextColor(0xFF888888);
            tvMeta.setPadding(0, 6, 0, 0);
            item.addView(tvMeta);

            ModrinthApi.ModVersionFile file = v.getPrimaryFile();
            if (file != null) {
                TextView tvFile = new TextView(this);
                tvFile.setText(file.filename + "  (" + formatSize(file.size) + ")");
                tvFile.setTextSize(12);
                tvFile.setTextColor(0xFF666666);
                tvFile.setPadding(0, 4, 0, 0);
                item.addView(tvFile);
            }

            item.setOnClickListener(v1 -> installVersion(v));

            View divider = new View(this);
            divider.setBackgroundColor(0x10000000);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));

            versionListContainer.addView(item);
            versionListContainer.addView(divider);
        }
    }

    private void showTargetPicker() {
        VersionManager vm = VersionManager.getInstance(repository);
        List<String> installed = vm.getInstalledVersions();
        if (installed.isEmpty()) {
            Toast.makeText(this, "没有已安装的游戏版本，请先下载", Toast.LENGTH_LONG).show();
            return;
        }
        String[] items = installed.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("选择目标游戏版本")
                .setItems(items, (d, which) -> {
                    targetVersionId = items[which];
                    tvTargetInfo.setText("目标版本: " + targetVersionId);
                })
                .show();
    }

    private void installVersion(ModrinthApi.ModVersion version) {
        if (targetVersionId == null) {
            Toast.makeText(this, "请先选择目标游戏版本", Toast.LENGTH_SHORT).show();
            showTargetPicker();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("正在安装...");

        String loader = settings.getModLoader();
        String mcVersion = gameVersion;

        modDownloadManager.installWithDependencies(
                projectId, version, mcVersion, loader, targetVersionId,
                new ModDownloadManager.DependencyCallback() {
                    @Override
                    public void onProgress(String message) {
                        mainHandler.post(() -> tvStatus.setText(message));
                    }

                    @Override
                    public void onSuccess(List<java.io.File> installed) {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("安装完成，共 " + installed.size() + " 个文件");
                            Toast.makeText(ModVersionListActivity.this,
                                    "安装成功，共 " + installed.size() + " 个文件", Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String message, Exception e) {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("安装失败: " + message);
                            Toast.makeText(ModVersionListActivity.this,
                                    "安装失败: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private static String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
