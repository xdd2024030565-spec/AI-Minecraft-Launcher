package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;

import java.io.File;

/**
 * 游戏目录管理页面 — 仿 FCL Profile / Profiles
 *
 * 允许用户选择游戏目录，可以共享 FCL 的游戏目录。
 */
public class GameDirectoryActivity extends AppCompatActivity {

    private FCLRepository repository;

    private Spinner spinnerDirectory;
    private TextView tvCurrentPath;
    private TextView tvVersionCount;
    private TextView tvModCount;
    private TextView tvTotalSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        setupUI();
        refreshInfo();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        // 标题
        TextView title = new TextView(this);
        title.setText("游戏目录");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // 当前路径
        tvCurrentPath = new TextView(this);
        tvCurrentPath.setTextSize(13);
        tvCurrentPath.setPadding(0, 8, 0, 16);
        root.addView(tvCurrentPath);

        // 目录选择
        LinearLayout dirRow = new LinearLayout(this);
        dirRow.setOrientation(LinearLayout.HORIZONTAL);
        dirRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dirLabel = new TextView(this);
        dirLabel.setText("选择目录: ");
        dirRow.addView(dirLabel);

        spinnerDirectory = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 添加候选目录
        adapter.add("默认 (应用内部)");
        adapter.add("共享 FCL 目录");
        adapter.add("内部存储 /sdcard/FCL");
        adapter.add("内部存储 /sdcard/Minecraft");

        // 检测 FCL 安装路径
        File fclDir = new File("/sdcard/Android/data/com.fcl.android/files/games");
        if (fclDir.exists()) {
            adapter.add("FCL 实际安装路径");
        }

        spinnerDirectory.setAdapter(adapter);
        spinnerDirectory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 仅显示信息，不立即切换
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        dirRow.addView(spinnerDirectory);
        root.addView(dirRow);

        // 切换按钮
        Button btnSwitch = new MaterialButton(this);
        btnSwitch.setText("切换到此目录");
        btnSwitch.setOnClickListener(v -> switchDirectory());
        root.addView(btnSwitch);

        // 信息区
        LinearLayout infoBox = new LinearLayout(this);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        infoBox.setPadding(32, 32, 32, 32);
        infoBox.setBackgroundColor(0x10000000);

        TextView infoTitle = new TextView(this);
        infoTitle.setText("当前目录信息");
        infoTitle.setTextSize(16);
        infoTitle.setTypeface(infoTitle.getTypeface(), android.graphics.Typeface.BOLD);
        infoBox.addView(infoTitle);

        tvVersionCount = new TextView(this);
        tvVersionCount.setTextSize(14);
        tvVersionCount.setPadding(0, 16, 0, 4);
        infoBox.addView(tvVersionCount);

        tvModCount = new TextView(this);
        tvModCount.setTextSize(14);
        tvModCount.setPadding(0, 4, 0, 4);
        infoBox.addView(tvModCount);

        tvTotalSize = new TextView(this);
        tvTotalSize.setTextSize(14);
        tvTotalSize.setPadding(0, 4, 0, 0);
        infoBox.addView(tvTotalSize);

        root.addView(infoBox);

        // 说明
        TextView note = new TextView(this);
        note.setText("\n说明:\n" +
                "• 选择“共享 FCL 目录”可直接使用 FCL 已下载的游戏\n" +
                "• 切换目录后，已安装的版本和 Mod 将从新目录加载\n" +
                "• 建议与 FCL 使用相同目录以节省空间");
        note.setTextSize(12);
        note.setPadding(0, 24, 0, 0);
        root.addView(note);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void switchDirectory() {
        int pos = spinnerDirectory.getSelectedItemPosition();
        File newDir = null;

        switch (pos) {
            case 0: // 默认
                newDir = new File(getFilesDir(), "FCL");
                break;
            case 1: // 共享 FCL 目录
                newDir = new File(getExternalFilesDir(null), "games");
                break;
            case 2: // /sdcard/FCL
                newDir = new File("/sdcard/FCL");
                break;
            case 3: // /sdcard/Minecraft
                newDir = new File("/sdcard/Minecraft");
                break;
            case 4: // FCL 实际安装路径
                newDir = new File("/sdcard/Android/data/com.fcl.android/files/games");
                break;
        }

        if (newDir != null) {
            if (!newDir.exists()) {
                newDir.mkdirs();
            }
            repository.changeDirectory(newDir);
            Toast.makeText(this, "已切换到: " + newDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            refreshInfo();
        }
    }

    private void refreshInfo() {
        tvCurrentPath.setText("当前路径: " + repository.getGameDirectoryPath());

        File versionsDir = repository.getVersionsDir();
        int versionCount = 0;
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            File[] dirs = versionsDir.listFiles(File::isDirectory);
            if (dirs != null) versionCount = dirs.length;
        }
        tvVersionCount.setText("已安装版本: " + versionCount);

        File modsDir = repository.getModDir();
        int modCount = 0;
        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] mods = modsDir.listFiles((d, n) -> n.endsWith(".jar"));
            if (mods != null) modCount = mods.length;
        }
        tvModCount.setText("全局 Mod 数量: " + modCount);

        long totalSize = calculateDirSize(repository.getRootDir());
        tvTotalSize.setText("总占用空间: " + formatSize(totalSize));
    }

    private long calculateDirSize(File dir) {
        long size = 0;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        size += f.length();
                    } else {
                        size += calculateDirSize(f);
                    }
                }
            }
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
