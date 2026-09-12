package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.game.VersionManager;

import java.util.List;

/**
 * FCL 主 Activity — 游戏启动器界面
 *
 * 显示已安装的游戏版本列表，允许用户选择并启动游戏。
 */
public class MainActivity extends AppCompatActivity {

    private FCLRepository repository;
    private VersionManager versionManager;
    private LinearLayout versionListContainer;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = FCLApplication.getInstance().getRepository();
        versionManager = VersionManager.getInstance(repository);

        setupUI();
        refreshVersionList();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(64, 64, 64, 64);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // 标题
        TextView title = new TextView(this);
        title.setText("Fold Craft Launcher");
        title.setTextSize(24);
        title.setPadding(0, 32, 0, 16);
        root.addView(title);

        // 状态文本
        statusText = new TextView(this);
        statusText.setText("选择游戏版本");
        statusText.setPadding(0, 0, 0, 32);
        root.addView(statusText);

        // 版本列表容器
        versionListContainer = new LinearLayout(this);
        versionListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(versionListContainer);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void refreshVersionList() {
        versionListContainer.removeAllViews();

        List<String> versions = versionManager.getInstalledVersions();
        if (versions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("尚未安装任何游戏版本");
            empty.setPadding(0, 32, 0, 32);
            versionListContainer.addView(empty);
            return;
        }

        for (String versionId : versions) {
            TextView versionItem = new TextView(this);
            versionItem.setText("▶ " + versionId);
            versionItem.setTextSize(16);
            versionItem.setPadding(16, 24, 16, 24);
            versionItem.setOnClickListener(v -> launchGame(versionId));
            versionListContainer.addView(versionItem);
        }
    }

    private void launchGame(String versionId) {
        statusText.setText("正在启动 " + versionId + "...");
        Toast.makeText(this, "启动 " + versionId, Toast.LENGTH_SHORT).show();
        // TODO: 调用 GameLauncher 实际启动游戏
    }
}
