package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.tungsten.fcl.mod.ModDownloadManager;
import com.tungsten.fcl.setting.LauncherSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Mod 搜索下载页面 — 仿 FCL DownloadPage / RemoteModDownloadPage
 *
 * 从这里读取 LauncherSettings 的源偏好和 API Key。
 */
public class ModSearchActivity extends AppCompatActivity {

    private FCLRepository repository;
    private ModDownloadManager modDownloadManager;
    private LauncherSettings settings;

    private Spinner spinnerType;
    private Spinner spinnerSource;
    private EditText etSearch;
    private Button btnSearch;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private RecyclerView recyclerView;
    private ResultAdapter adapter;

    private final List<ModDownloadManager.UnifiedModResult> searchResults = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FCLApplication.getInstance().getRepository();
        settings = LauncherSettings.getInstance(this);
        modDownloadManager = new ModDownloadManager(repository, this);
        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (modDownloadManager != null) modDownloadManager.refreshApiKey();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("Mod / 整合包 搜索");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // 类型
        LinearLayout typeRow = new LinearLayout(this);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        typeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView typeLabel = new TextView(this);
        typeLabel.setText("类型: ");
        typeRow.addView(typeLabel);
        spinnerType = new Spinner(this);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Mod", "整合包", "资源包", "光影包"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        typeRow.addView(spinnerType);
        root.addView(typeRow);

        // 来源
        LinearLayout sourceRow = new LinearLayout(this);
        sourceRow.setOrientation(LinearLayout.HORIZONTAL);
        sourceRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView sourceLabel = new TextView(this);
        sourceLabel.setText("来源: ");
        sourceRow.addView(sourceLabel);
        spinnerSource = new Spinner(this);
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Modrinth", "CurseForge", "自动"});
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(sourceAdapter);
        sourceRow.addView(spinnerSource);
        root.addView(sourceRow);

        // 搜索框
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        etSearch = new EditText(this);
        etSearch.setHint("搜索关键词...");
        etSearch.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        searchRow.addView(etSearch);
        btnSearch = new MaterialButton(this);
        btnSearch.setText("搜索");
        btnSearch.setOnClickListener(v -> doSearch());
        searchRow.addView(btnSearch);
        root.addView(searchRow);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        tvStatus = new TextView(this);
        tvStatus.setText("输入关键词并搜索");
        tvStatus.setPadding(0, 16, 0, 16);
        root.addView(tvStatus);

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        adapter = new ResultAdapter(searchResults);
        recyclerView.setAdapter(adapter);
        root.addView(recyclerView);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void doSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        ModDownloadManager.AddonType type =
                ModDownloadManager.AddonType.values()[spinnerType.getSelectedItemPosition()];
        ModDownloadManager.ModSource source =
                ModDownloadManager.ModSource.values()[spinnerSource.getSelectedItemPosition()];

        searchResults.clear();
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("正在搜索...");
        btnSearch.setEnabled(false);

        modDownloadManager.searchMods(type, "", query, 0, 20, source,
                new ModDownloadManager.SearchCallback() {
                    @Override
                    public void onResult(ModDownloadManager.UnifiedModResult result) {
                        mainHandler.post(() -> {
                            searchResults.add(result);
                            adapter.notifyItemInserted(searchResults.size() - 1);
                        });
                    }

                    @Override
                    public void onComplete() {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("找到 " + searchResults.size() + " 个结果");
                            btnSearch.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String message, Exception e) {
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("搜索失败: " + message);
                            btnSearch.setEnabled(true);
                            Toast.makeText(ModSearchActivity.this,
                                    "搜索失败: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private static class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
        private final List<ModDownloadManager.UnifiedModResult> results;

        ResultAdapter(List<ModDownloadManager.UnifiedModResult> results) {
            this.results = results;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(parent.getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(24, 16, 24, 16);
            item.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView title = new TextView(parent.getContext());
            title.setTextSize(16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            item.addView(title);

            TextView desc = new TextView(parent.getContext());
            desc.setTextSize(13);
            desc.setMaxLines(2);
            item.addView(desc);

            TextView meta = new TextView(parent.getContext());
            meta.setTextSize(12);
            meta.setTextColor(0xFF888888);
            item.addView(meta);

            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ModDownloadManager.UnifiedModResult r = results.get(position);
            LinearLayout item = (LinearLayout) holder.itemView;

            TextView title = (TextView) item.getChildAt(0);
            TextView desc = (TextView) item.getChildAt(1);
            TextView meta = (TextView) item.getChildAt(2);

            title.setText(r.title);
            desc.setText(r.description);
            meta.setText(r.source.getDisplayName() + " | 下载量: " + r.downloadCount);
        }

        @Override
        public int getItemCount() { return results.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) { super(itemView); }
        }
    }
}
