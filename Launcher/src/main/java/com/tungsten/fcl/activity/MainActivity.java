package com.tungsten.fcl.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.auth.AccountManager;
import com.tungsten.fcl.game.VersionManager;
import com.tungsten.fcl.launch.GameLauncher;

import java.util.List;

/**
 * FCL 主 Activity — 游戏启动器界面 (重写版)
 *
 * 仿 FCL 主界面，显示已安装的游戏版本列表。
 * 提供下载版本、Mod搜索、游戏目录、账户管理、版本设置入口。
 */
public class MainActivity extends AppCompatActivity {

    private FCLRepository repository;
    private VersionManager versionManager;
    private LinearLayout versionListContainer;
    private TextView statusText;
    private TextView emptyHint;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        versionManager = VersionManager.getInstance(repository);
        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshVersionList();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // 标题
        TextView title = new TextView(this);
        title.setText("Fold Craft Launcher");
        title.setTextSize(24);
        title.setPadding(0, 32, 0, 8);
        root.addView(title);

        // 游戏目录路径
        TextView tvPath = new TextView(this);
        tvPath.setText(repository.getGameDirectoryPath());
        tvPath.setTextSize(11);
        tvPath.setTextColor(0xFF888888);
        tvPath.setPadding(0, 0, 0, 16);
        root.addView(tvPath);

        // 功能按钮区
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);

        Button btnDownload = new MaterialButton(this);
        btnDownload.setText("⬇ 下载版本");
        btnDownload.setOnClickListener(v -> {
            startActivity(new Intent(this, DownloadActivity.class));
        });
        btnRow.addView(btnDownload, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnModSearch = new MaterialButton(this);
        btnModSearch.setText("📦 Mod");
        btnModSearch.setOnClickListener(v -> {
            startActivity(new Intent(this, ModSearchActivity.class));
        });
        btnRow.addView(btnModSearch, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(btnRow);

        // 第二行按钮
        LinearLayout btnRow2 = new LinearLayout(this);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        btnRow2.setGravity(Gravity.CENTER_VERTICAL);

        Button btnDir = new MaterialButton(this);
        btnDir.setText("📁 游戏目录");
        btnDir.setOnClickListener(v -> {
            startActivity(new Intent(this, GameDirectoryActivity.class));
        });
        btnRow2.addView(btnDir, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnAccount = new MaterialButton(this);
        btnAccount.setText("👤 账户");
        btnAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, AccountActivity.class));
        });
        btnRow2.addView(btnAccount, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(btnRow2);

        // 状态
        statusText = new TextView(this);
        statusText.setText("选择游戏版本");
        statusText.setPadding(0, 24, 0, 16);
        root.addView(statusText);

        // 版本列表容器
        versionListContainer = new LinearLayout(this);
        versionListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(versionListContainer);

        // 空提示
        emptyHint = new TextView(this);
        emptyHint.setText("尚未安装任何版本\n点击“下载版本”开始安装");
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setPadding(0, 64, 0, 64);
        emptyHint.setVisibility(View.GONE);
        root.addView(emptyHint);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void refreshVersionList() {
        versionListContainer.removeAllViews();
        List<String> versions = versionManager.getInstalledVersions();

        if (versions.isEmpty()) {
            emptyHint.setVisibility(View.VISIBLE);
            statusText.setText("未安装任何版本");
            return;
        }

        emptyHint.setVisibility(View.GONE);
        statusText.setText("已安装 " + versions.size() + " 个版本，点击启动");

        for (String versionId : versions) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(24, 20, 24, 20);
            item.setGravity(Gravity.CENTER_VERTICAL);

            // 版本名 + 状态
            TextView versionName = new TextView(this);
            versionName.setText(versionId);
            versionName.setTextSize(16);
            versionName.setTypeface(versionName.getTypeface(), android.graphics.Typeface.BOLD);
            item.addView(versionName);

            // 版本信息
            File jarFile = versionManager.getVersionJar(versionId);
            File modsDir = versionManager.getVersionModsDir(versionId);
            int modCount = (modsDir.exists() && modsDir.isDirectory()) ?
                    modsDir.listFiles((d, n) -> n.endsWith(".jar") || n.endsWith(".disabled")).length : 0;

            String info = (jarFile.exists() ? "✓ 完整" : "⚠ 不完整") +
                    (modCount > 0 ? "  |  Mod: " + modCount : "");

            TextView versionInfo = new TextView(this);
            versionInfo.setText(info);
            versionInfo.setTextSize(12);
            versionInfo.setTextColor(0xFF888888);
            item.addView(versionInfo);

            // 点击启动
            item.setOnClickListener(v -> launchGame(versionId));
            item.setOnLongClickListener(v -> {
                showVersionOptions(versionId);
                return true;
            });

            // 分隔线
            View divider = new View(this);
            divider.setBackgroundColor(0x10000000);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));

            versionListContainer.addView(item);
            versionListContainer.addView(divider);
        }
    }

    private void showVersionOptions(String versionId) {
        // 弹出选项: 启动/设置/删除
        String[] options = {"启动游戏", "版本设置", "删除版本"};
        new android.app.AlertDialog.Builder(this)
                .setTitle(versionId)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            launchGame(versionId);
                            break;
                        case 1:
                            Intent intent = new Intent(this, VersionSettingsActivity.class);
                            intent.putExtra("version_id", versionId);
                            startActivity(intent);
                            break;
                        case 2:
                            confirmDelete(versionId);
                            break;
                    }
                })
                .show();
    }

    private void confirmDelete(String versionId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("删除版本")
                .setMessage("确认删除 " + versionId + " ?")
                .setPositiveButton("删除", (d, w) -> {
                    versionManager.removeVersion(versionId);
                    refreshVersionList();
                    Toast.makeText(this, "已删除: " + versionId, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 启动游戏
     */
    private void launchGame(String versionId) {
        statusText.setText("正在启动 " + versionId + "...");
        new Thread(() -> {
            try {
                GameLauncher launcher = new GameLauncher(repository);
                AccountManager accountManager = AccountManager.getInstance();
                AccountManager.Account account = accountManager.getCurrentAccount();
                if (account == null) {
                    account = accountManager.createOfflineAccount("Player");
                }
                Process process = launcher.launch(versionId, account);
                mainHandler.post(() -> statusText.setText("游戏已启动: " + versionId + " (PID: " + process.pid() + ")"));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusText.setText("启动失败: " + e.getMessage());
                    Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
