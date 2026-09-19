package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.mod.LocalModManager;
import com.tungsten.fcl.mod.ModDownloadManager;
import com.tungsten.fcl.mod.ModrinthApi;

import java.io.File;
import java.util.List;

/**
 * 本地 Mod 管理页面 — 对齐 FCL LocalModListPage
 *
 * 列出已安装 Mod，支持启用/禁用/删除，SHA1 反查远程版本检查更新。
 */
public class LocalModActivity extends AppCompatActivity {

    public static final String EXTRA_VERSION_ID = "version_id";

    private FCLRepository repository;
    private ModDownloadManager modDownloadManager;
    private LocalModManager localModManager;

    private String versionId;
    private LinearLayout modListContainer;
    private TextView tvStatus;
    private TextView tvStats;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        modDownloadManager = new ModDownloadManager(repository, this);

        versionId = getIntent().getStringExtra(EXTRA_VERSION_ID);
        if (versionId == null) {
            // 默认取第一个已安装版本
            java.util.List<String> installed = com.tungsten.fcl.game.VersionManager
                    .getInstance(repository).getInstalledVersions();
            if (installed.isEmpty()) { finish(); return; }
            versionId = installed.get(0);
        }

        localModManager = new LocalModManager(repository.getVersionModsDir(versionId));
        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("已安装 Mod");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 4);
        root.addView(title);

        TextView tvVer = new TextView(this);
        tvVer.setText("游戏版本: " + versionId);
        tvVer.setTextSize(12);
        tvVer.setTextColor(0xFF888888);
        root.addView(tvVer);

        tvStats = new TextView(this);
        tvStats.setTextSize(13);
        tvStats.setPadding(0, 12, 0, 12);
        root.addView(tvStats);

        tvStatus = new TextView(this);
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(0xFF888888);
        tvStatus.setPadding(0, 0, 0, 12);
        root.addView(tvStatus);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnRefresh = new MaterialButton(this);
        btnRefresh.setText("刷新");
        btnRefresh.setOnClickListener(v -> refreshList());
        btnRow.addView(btnRefresh, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnCheckUpdate = new MaterialButton(this);
        btnCheckUpdate.setText("检查更新");
        btnCheckUpdate.setOnClickListener(v -> checkUpdates());
        btnRow.addView(btnCheckUpdate, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(btnRow);

        modListContainer = new LinearLayout(this);
        modListContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        listP.topMargin = 16;
        root.addView(modListContainer, listP);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void refreshList() {
        modListContainer.removeAllViews();
        List<File> mods = localModManager.getModFiles();
        LocalModManager.ModStats stats = localModManager.getStats();

        tvStats.setText("共 " + stats.total + " 个  |  启用 " + stats.enabled
                + "  禁用 " + stats.disabled + "  |  " + stats.formatSize());

        if (mods.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("尚未安装任何 Mod\n可从“Mod”页面搜索下载");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 64, 0, 64);
            modListContainer.addView(empty);
            return;
        }

        for (File mod : mods) {
            modListContainer.addView(buildModItem(mod));
        }
    }

    private View buildModItem(File mod) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);

        boolean disabled = LocalModManager.isDisabled(mod);

        // 第一行：复选框 + 文件名
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(!disabled);
        cb.setOnCheckedChangeListener((b, checked) -> {
            try {
                localModManager.toggleMod(mod);
                refreshList();
            } catch (Exception e) {
                Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        row1.addView(cb);

        TextView tvName = new TextView(this);
        tvName.setText(LocalModManager.getRealName(mod));
        tvName.setTextSize(15);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (disabled) {
            tvName.setAlpha(0.5f);
        }
        row1.addView(tvName);

        item.addView(row1);

        // 第二行：大小 + 状态 + 操作
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(40, 4, 0, 0);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(formatSize(mod.length()) + (disabled ? "  [已禁用]" : ""));
        tvInfo.setTextSize(12);
        tvInfo.setTextColor(0xFF888888);
        tvInfo.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row2.addView(tvInfo);

        Button btnDelete = new MaterialButton(this);
        btnDelete.setText("删除");
        btnDelete.setOnClickListener(v -> confirmDelete(mod));
        row2.addView(btnDelete);

        item.addView(row2);

        View divider = new View(this);
        divider.setBackgroundColor(0x10000000);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(item);
        wrapper.addView(divider);
        return wrapper;
    }

    private void confirmDelete(File mod) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("删除 Mod")
                .setMessage("确认删除 " + LocalModManager.getRealName(mod) + " ?")
                .setPositiveButton("删除", (d, w) -> {
                    localModManager.deleteMod(mod);
                    refreshList();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 检查更新 — 通过 SHA1 反查 Modrinth 得到远程版本
     */
    private void checkUpdates() {
        List<File> mods = localModManager.getModFiles();
        if (mods.isEmpty()) {
            Toast.makeText(this, "没有可检查的 Mod", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("正在检查更新...");
        new Thread(() -> {
            int matched = 0;
            int outdated = 0;
            int current = 0;
            int failed = 0;
            StringBuilder detail = new StringBuilder();

            for (File mod : mods) {
                try {
                    String sha1 = ModrinthApi.sha1Of(mod);
                    ModrinthApi.ModVersion remote = modDownloadManager.getModrinthApi()
                            .getVersionByHash(sha1);
                    if (remote == null) {
                        failed++;
                        continue;
                    }
                    matched++;

                    // 查询该项目最新版本是否有更新
                    List<ModrinthApi.ModVersion> latest = modDownloadManager.getModrinthApi()
                            .getVersions(remote.projectId);
                    if (latest.isEmpty()) { current++; continue; }

                    ModrinthApi.ModVersion newest = latest.get(0);
                    if (newest.id != null && !newest.id.equals(remote.id)) {
                        outdated++;
                        detail.append("• ").append(LocalModManager.getRealName(mod))
                                .append(" → ").append(newest.name).append("\n");
                    } else {
                        current++;
                    }
                } catch (Exception e) {
                    failed++;
                }
            }

            final int fMatched = matched, fOutdated = outdated, fCurrent = current, fFailed = failed;
            final String fDetail = detail.toString();

            mainHandler.post(() -> {
                String msg = "检查完成: 识别 " + fMatched + " 个 Mod\n"
                        + "最新 " + fCurrent + " 个  |  可更新 " + fOutdated + " 个\n"
                        + "未匹配 " + fFailed + " 个 (非 Modrinth 来源或已修改)";
                tvStatus.setText(msg);

                if (fOutdated > 0) {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("可更新的 Mod")
                            .setMessage(fDetail)
                            .setPositiveButton("知道了", null)
                            .show();
                } else {
                    Toast.makeText(this, "已是最新版本", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
