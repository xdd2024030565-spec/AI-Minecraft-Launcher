package com.tungsten.fcl.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.FCLApplication;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.download.AutoDownloadProvider;
import com.tungsten.fcl.download.DownloadProvider;
import com.tungsten.fcl.download.GameDownloader;
import com.tungsten.fcl.game.VersionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本下载页面 — 仿 FCL VersionInstallPage / InstallerListPage
 *
 * 显示 Mojang 版本清单，支持选择并下载安装。
 */
public class DownloadActivity extends AppCompatActivity {

    private FCLRepository repository;
    private GameDownloader gameDownloader;
    private VersionManager versionManager;

    private Spinner spinnerVersionType;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvStatus;
    private RecyclerView recyclerView;
    private Button btnRefresh;
    private Button btnDownload;

    private List<GameDownloader.RemoteVersionInfo> allVersions = new ArrayList<>();
    private List<GameDownloader.RemoteVersionInfo> filteredVersions = new ArrayList<>();
    private String selectedVersion = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        gameDownloader = new GameDownloader(repository);
        versionManager = VersionManager.getInstance(repository);
        setupUI();
        loadVersions();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        // 标题
        TextView title = new TextView(this);
        title.setText("下载游戏版本");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // 版本类型筛选
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView filterLabel = new TextView(this);
        filterLabel.setText("筛选: ");
        filterLabel.setTextSize(14);
        filterRow.addView(filterLabel);

        spinnerVersionType = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"全部", "正式版", "快照版"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVersionType.setAdapter(adapter);
        spinnerVersionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterVersions(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        filterRow.addView(spinnerVersionType);
        root.addView(filterRow);

        // 进度区
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 48));
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        tvProgress = new TextView(this);
        tvProgress.setVisibility(View.GONE);
        tvProgress.setPadding(0, 8, 0, 8);
        root.addView(tvProgress);

        tvStatus = new TextView(this);
        tvStatus.setText("正在加载版本列表...");
        tvStatus.setPadding(0, 16, 0, 16);
        root.addView(tvStatus);

        // 版本列表
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(recyclerView);

        // 按钮
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 16, 0, 0);

        btnRefresh = new MaterialButton(this);
        btnRefresh.setText("刷新列表");
        btnRefresh.setOnClickListener(v -> loadVersions());
        btnRow.addView(btnRefresh, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnDownload = new MaterialButton(this);
        btnDownload.setText("下载安装");
        btnDownload.setEnabled(false);
        btnDownload.setOnClickListener(v -> downloadSelectedVersion());
        btnRow.addView(btnDownload, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(btnRow);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void loadVersions() {
        tvStatus.setText("正在从 Mojang 获取版本清单...");
        btnRefresh.setEnabled(false);
        btnDownload.setEnabled(false);

        new Thread(() -> {
            try {
                allVersions = gameDownloader.getRemoteVersions();
                mainHandler.post(() -> {
                    filterVersions(spinnerVersionType.getSelectedItemPosition());
                    tvStatus.setText("共 " + allVersions.size() + " 个版本可用");
                    btnRefresh.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvStatus.setText("加载失败: " + e.getMessage());
                    btnRefresh.setEnabled(true);
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void filterVersions(int filterType) {
        filteredVersions.clear();
        for (GameDownloader.RemoteVersionInfo v : allVersions) {
            switch (filterType) {
                case 0: // 全部
                    filteredVersions.add(v);
                    break;
                case 1: // 正式版
                    if (v.isRelease()) filteredVersions.add(v);
                    break;
                case 2: // 快照版
                    if (v.isSnapshot()) filteredVersions.add(v);
                    break;
            }
        }
        recyclerView.setAdapter(new VersionListAdapter(filteredVersions, versionManager));
        if (!filteredVersions.isEmpty()) {
            selectedVersion = filteredVersions.get(0).getId();
            btnDownload.setEnabled(true);
        }
    }

    private void downloadSelectedVersion() {
        if (selectedVersion == null) return;

        String versionId = selectedVersion;
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        btnDownload.setEnabled(false);
        btnRefresh.setEnabled(false);

        new Thread(() -> {
            try {
                gameDownloader.downloadVersion(versionId, new GameDownloader.DownloadCallback() {
                    @Override
                    public void onProgress(String stage, int current, int total, String message) {
                        mainHandler.post(() -> {
                            progressBar.setMax(total);
                            progressBar.setProgress(current);
                            tvProgress.setText(message);
                        });
                    }

                    @Override
                    public void onComplete(String versionId) {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvProgress.setVisibility(View.GONE);
                            tvStatus.setText("下载完成: " + versionId);
                            btnDownload.setEnabled(true);
                            btnRefresh.setEnabled(true);
                            Toast.makeText(DownloadActivity.this,
                                    "安装成功: " + versionId, Toast.LENGTH_LONG).show();
                            // 刷新列表显示已安装状态
                            recyclerView.getAdapter().notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(String message, Exception e) {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvProgress.setVisibility(View.GONE);
                            tvStatus.setText("下载失败: " + message);
                            btnDownload.setEnabled(true);
                            btnRefresh.setEnabled(true);
                            Toast.makeText(DownloadActivity.this,
                                    "下载失败: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvProgress.setVisibility(View.GONE);
                    tvStatus.setText("下载失败: " + e.getMessage());
                    btnDownload.setEnabled(true);
                    btnRefresh.setEnabled(true);
                    Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // === 版本列表适配器 ===

    private static class VersionListAdapter extends RecyclerView.Adapter<VersionListAdapter.ViewHolder> {

        private final List<GameDownloader.RemoteVersionInfo> versions;
        private final VersionManager versionManager;
        private int selectedPos = 0;

        VersionListAdapter(List<GameDownloader.RemoteVersionInfo> versions, VersionManager versionManager) {
            this.versions = versions;
            this.versionManager = versionManager;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(15);
            tv.setPadding(24, 20, 24, 20);
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GameDownloader.RemoteVersionInfo v = versions.get(position);
            TextView tv = (TextView) holder.itemView;

            String status = versionManager.isVersionInstalled(v.getId()) ? " ✓ 已安装" : "";
            String type = v.isRelease() ? "[正式版]" : (v.isSnapshot() ? "[快照]" : "[其他]");
            tv.setText(type + " " + v.getId() + status);

            if (position == selectedPos) {
                tv.setBackgroundColor(0x22000000);
            } else {
                tv.setBackgroundColor(0x00000000);
            }

            holder.itemView.setOnClickListener(v1 -> {
                int oldPos = selectedPos;
                selectedPos = holder.getBindingAdapterPosition();
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPos);
            });
        }

        @Override
        public int getItemCount() {
            return versions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
