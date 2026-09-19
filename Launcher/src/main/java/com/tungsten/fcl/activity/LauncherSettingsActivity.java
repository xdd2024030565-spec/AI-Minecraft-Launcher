package com.tungsten.fcl.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.setting.LauncherSettings;

/**
 * 启动器全局设置页面 — 仿 FCL Settings
 *
 * 用户可在此配置:
 * - 下载源 (自动/BMCLAPI/Mojang)
 * - CurseForge API Key (用户自行填写)
 * - 镜像地址
 * - 下载并发数
 * - 完整性校验
 */
public class LauncherSettingsActivity extends AppCompatActivity {

    private LauncherSettings settings;

    private Spinner spinnerDownloadSource;
    private Spinner spinnerModSource;
    private EditText etCurseForgeKey;
    private EditText etBMCLAPIRoot;
    private EditText etModrinthMirror;
    private EditText etCurseForgeMirror;
    private SeekBar seekConcurrency;
    private TextView tvConcurrency;
    private CheckBox cbIntegrityCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = LauncherSettings.getInstance(this);
        setupUI();
        loadSettings();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("启动器设置");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // === 下载源 ===
        root.addView(sectionTitle("下载源"));

        TextView tvDownloadLabel = new TextView(this);
        tvDownloadLabel.setText("资源下载源");
        tvDownloadLabel.setTextSize(14);
        root.addView(tvDownloadLabel);

        spinnerDownloadSource = new Spinner(this);
        ArrayAdapter<String> dsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"自动 (BMCLAPI 优先)", "BMCLAPI 镜像", "Mojang 官方"});
        dsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDownloadSource.setAdapter(dsAdapter);
        root.addView(spinnerDownloadSource);

        // === Mod 源 ===
        root.addView(sectionTitle("Mod 下载源"));

        spinnerModSource = new Spinner(this);
        ArrayAdapter<String> msAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Modrinth", "CurseForge", "自动"});
        msAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModSource.setAdapter(msAdapter);
        root.addView(spinnerModSource);

        // === CurseForge API Key ===
        root.addView(sectionTitle("CurseForge API Key"));

        TextView tvKeyHint = new TextView(this);
        tvKeyHint.setText("从 https://console.curseforge.com/ 获取 API Key 后填入下方。");
        tvKeyHint.setTextSize(12);
        tvKeyHint.setTextColor(0xFF888888);
        tvKeyHint.setPadding(0, 4, 0, 8);
        root.addView(tvKeyHint);

        etCurseForgeKey = new EditText(this);
        etCurseForgeKey.setHint("$2a$10$...");
        etCurseForgeKey.setSingleLine(true);
        root.addView(etCurseForgeKey);

        // === 镜像地址 ===
        root.addView(sectionTitle("镜像地址 (高级)"));

        TextView tvBmclLabel = new TextView(this);
        tvBmclLabel.setText("BMCLAPI 根地址");
        tvBmclLabel.setTextSize(13);
        root.addView(tvBmclLabel);

        etBMCLAPIRoot = new EditText(this);
        etBMCLAPIRoot.setHint("https://bmclapi2.bangbang93.com");
        etBMCLAPIRoot.setSingleLine(true);
        root.addView(etBMCLAPIRoot);

        TextView tvMrLabel = new TextView(this);
        tvMrLabel.setText("Modrinth 镜像");
        tvMrLabel.setTextSize(13);
        tvMrLabel.setPadding(0, 12, 0, 0);
        root.addView(tvMrLabel);

        etModrinthMirror = new EditText(this);
        etModrinthMirror.setHint("https://mod.mcimirror.top/modrinth");
        etModrinthMirror.setSingleLine(true);
        root.addView(etModrinthMirror);

        TextView tvCfLabel = new TextView(this);
        tvCfLabel.setText("CurseForge 镜像");
        tvCfLabel.setTextSize(13);
        tvCfLabel.setPadding(0, 12, 0, 0);
        root.addView(tvCfLabel);

        etCurseForgeMirror = new EditText(this);
        etCurseForgeMirror.setHint("https://mod.mcimirror.top/curseforge");
        etCurseForgeMirror.setSingleLine(true);
        root.addView(etCurseForgeMirror);

        // === 下载行为 ===
        root.addView(sectionTitle("下载行为"));

        TextView tvConcLabel = new TextView(this);
        tvConcLabel.setText("下载并发数");
        tvConcLabel.setTextSize(14);
        root.addView(tvConcLabel);

        seekConcurrency = new SeekBar(this);
        seekConcurrency.setMax(32);
        seekConcurrency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvConcurrency.setText(Math.max(1, progress) + " 线程");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(seekConcurrency);

        tvConcurrency = new TextView(this);
        tvConcurrency.setText("8 线程");
        tvConcurrency.setGravity(Gravity.RIGHT);
        root.addView(tvConcurrency);

        cbIntegrityCheck = new CheckBox(this);
        cbIntegrityCheck.setText("下载后校验文件完整性 (SHA1)");
        cbIntegrityCheck.setPadding(0, 16, 0, 0);
        root.addView(cbIntegrityCheck);

        // === 保存 ===
        Button btnSave = new MaterialButton(this);
        btnSave.setText("保存设置");
        btnSave.setOnClickListener(v -> saveSettings());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 32;
        root.addView(btnSave, btnParams);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setPadding(0, 24, 0, 8);
        return tv;
    }

    private void loadSettings() {
        spinnerDownloadSource.setSelection(settings.getDownloadSource());
        spinnerModSource.setSelection(settings.getModSource());
        etCurseForgeKey.setText(settings.getCurseForgeApiKey());
        etBMCLAPIRoot.setText(settings.getBMCLAPIRoot());
        etModrinthMirror.setText(settings.getModrinthMirror());
        etCurseForgeMirror.setText(settings.getCurseForgeMirror());
        int conc = settings.getDownloadConcurrency();
        seekConcurrency.setProgress(conc);
        tvConcurrency.setText(conc + " 线程");
        cbIntegrityCheck.setChecked(settings.isIntegrityCheck());
    }

    private void saveSettings() {
        settings.setDownloadSource(spinnerDownloadSource.getSelectedItemPosition());
        settings.setModSource(spinnerModSource.getSelectedItemPosition());
        settings.setCurseForgeApiKey(etCurseForgeKey.getText().toString());
        settings.setBMCLAPIRoot(etBMCLAPIRoot.getText().toString().trim());
        settings.setModrinthMirror(etModrinthMirror.getText().toString().trim());
        settings.setCurseForgeMirror(etCurseForgeMirror.getText().toString().trim());
        settings.setDownloadConcurrency(Math.max(1, seekConcurrency.getProgress()));
        settings.setIntegrityCheck(cbIntegrityCheck.isChecked());

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}
