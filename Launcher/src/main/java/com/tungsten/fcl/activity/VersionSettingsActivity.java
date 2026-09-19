package com.tungsten.fcl.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.launch.LaunchOptions;

/**
 * 版本设置页面 — 仿 FCL VersionSetting
 *
 * 配置单个游戏版本的启动参数：内存、分辨率、Java 路径等。
 */
public class VersionSettingsActivity extends AppCompatActivity {

    private String versionId;

    private SeekBar seekMaxMemory;
    private TextView tvMaxMemory;
    private SeekBar seekMinMemory;
    private TextView tvMinMemory;
    private EditText etJavaArgs;
    private EditText etMcArgs;
    private EditText etServerIp;
    private CheckBox cbFullscreen;
    private CheckBox cbIsolate;
    private EditText etWidth;
    private EditText etHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        versionId = getIntent().getStringExtra("version_id");
        if (versionId == null) {
            finish();
            return;
        }
        setupUI();
        loadSettings();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        // 标题
        TextView title = new TextView(this);
        title.setText("版本设置: " + versionId);
        title.setTextSize(20);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // 最大内存
        TextView tvMaxLabel = new TextView(this);
        tvMaxLabel.setText("最大内存 (MB)");
        tvMaxLabel.setTextSize(14);
        root.addView(tvMaxLabel);

        seekMaxMemory = new SeekBar(this);
        seekMaxMemory.setMax(8192);
        seekMaxMemory.setProgress(2048);
        seekMaxMemory.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMaxMemory.setText(progress + " MB");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(seekMaxMemory);

        tvMaxMemory = new TextView(this);
        tvMaxMemory.setText("2048 MB");
        tvMaxMemory.setGravity(Gravity.RIGHT);
        root.addView(tvMaxMemory);

        // 最小内存
        TextView tvMinLabel = new TextView(this);
        tvMinLabel.setText("最小内存 (MB)");
        tvMinLabel.setTextSize(14);
        tvMinLabel.setPadding(0, 16, 0, 0);
        root.addView(tvMinLabel);

        seekMinMemory = new SeekBar(this);
        seekMinMemory.setMax(2048);
        seekMinMemory.setProgress(512);
        seekMinMemory.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMinMemory.setText(progress + " MB");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(seekMinMemory);

        tvMinMemory = new TextView(this);
        tvMinMemory.setText("512 MB");
        tvMinMemory.setGravity(Gravity.RIGHT);
        root.addView(tvMinMemory);

        // Java 参数
        TextView tvJavaArgsLabel = new TextView(this);
        tvJavaArgsLabel.setText("JVM 参数");
        tvJavaArgsLabel.setTextSize(14);
        tvJavaArgsLabel.setPadding(0, 16, 0, 4);
        root.addView(tvJavaArgsLabel);

        etJavaArgs = new EditText(this);
        etJavaArgs.setHint("额外 JVM 参数 (可选)");
        etJavaArgs.setSingleLine(false);
        root.addView(etJavaArgs);

        // Minecraft 参数
        TextView tvMcArgsLabel = new TextView(this);
        tvMcArgsLabel.setText("Minecraft 参数");
        tvMcArgsLabel.setTextSize(14);
        tvMcArgsLabel.setPadding(0, 16, 0, 4);
        root.addView(tvMcArgsLabel);

        etMcArgs = new EditText(this);
        etMcArgs.setHint("额外 Minecraft 参数 (可选)");
        etMcArgs.setSingleLine(false);
        root.addView(etMcArgs);

        // 分辨率
        TextView tvResLabel = new TextView(this);
        tvResLabel.setText("分辨率");
        tvResLabel.setTextSize(14);
        tvResLabel.setPadding(0, 16, 0, 4);
        root.addView(tvResLabel);

        LinearLayout resRow = new LinearLayout(this);
        resRow.setOrientation(LinearLayout.HORIZONTAL);

        etWidth = new EditText(this);
        etWidth.setHint("宽");
        etWidth.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etWidth.setText("854");
        etWidth.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        resRow.addView(etWidth);

        TextView xLabel = new TextView(this);
        xLabel.setText(" × ");
        xLabel.setGravity(Gravity.CENTER);
        resRow.addView(xLabel);

        etHeight = new EditText(this);
        etHeight.setHint("高");
        etHeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etHeight.setText("480");
        etHeight.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        resRow.addView(etHeight);

        root.addView(resRow);

        // 服务器 IP
        TextView tvServerLabel = new TextView(this);
        tvServerLabel.setText("服务器 IP (进入游戏后自动连接)");
        tvServerLabel.setTextSize(14);
        tvServerLabel.setPadding(0, 16, 0, 4);
        root.addView(tvServerLabel);

        etServerIp = new EditText(this);
        etServerIp.setHint("例如: play.example.com:25565");
        root.addView(etServerIp);

        // 选项
        LinearLayout optionsBox = new LinearLayout(this);
        optionsBox.setOrientation(LinearLayout.VERTICAL);
        optionsBox.setPadding(0, 16, 0, 16);

        cbFullscreen = new CheckBox(this);
        cbFullscreen.setText("全屏模式");
        optionsBox.addView(cbFullscreen);

        cbIsolate = new CheckBox(this);
        cbIsolate.setText("独立游戏目录 (每个版本使用自己的 .minecraft)");
        cbIsolate.setChecked(true);
        optionsBox.addView(cbIsolate);

        root.addView(optionsBox);

        // 保存按钮
        Button btnSave = new MaterialButton(this);
        btnSave.setText("保存设置");
        btnSave.setOnClickListener(v -> saveSettings());
        root.addView(btnSave);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private String getPrefsKey() {
        return "version_settings_" + versionId;
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(getPrefsKey(), MODE_PRIVATE);
        int maxMem = prefs.getInt("maxMemory", 2048);
        int minMem = prefs.getInt("minMemory", 512);
        seekMaxMemory.setProgress(maxMem);
        seekMinMemory.setProgress(minMem);
        tvMaxMemory.setText(maxMem + " MB");
        tvMinMemory.setText(minMem + " MB");
        etJavaArgs.setText(prefs.getString("javaArgs", ""));
        etMcArgs.setText(prefs.getString("minecraftArgs", ""));
        etServerIp.setText(prefs.getString("serverIp", ""));
        etWidth.setText(String.valueOf(prefs.getInt("width", 854)));
        etHeight.setText(String.valueOf(prefs.getInt("height", 480)));
        cbFullscreen.setChecked(prefs.getBoolean("fullscreen", false));
        cbIsolate.setChecked(prefs.getBoolean("isolate", true));
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(getPrefsKey(), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("maxMemory", seekMaxMemory.getProgress());
        editor.putInt("minMemory", seekMinMemory.getProgress());
        editor.putString("javaArgs", etJavaArgs.getText().toString());
        editor.putString("minecraftArgs", etMcArgs.getText().toString());
        editor.putString("serverIp", etServerIp.getText().toString());
        editor.putInt("width", Integer.parseInt(etWidth.getText().toString().isEmpty() ? "854" : etWidth.getText().toString()));
        editor.putInt("height", Integer.parseInt(etHeight.getText().toString().isEmpty() ? "480" : etHeight.getText().toString()));
        editor.putBoolean("fullscreen", cbFullscreen.isChecked());
        editor.putBoolean("isolate", cbIsolate.isChecked());
        editor.apply();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * 加载版本设置到 LaunchOptions
     */
    public LaunchOptions toLaunchOptions() {
        SharedPreferences prefs = getSharedPreferences(getPrefsKey(), MODE_PRIVATE);
        LaunchOptions options = new LaunchOptions();
        options.setMaxMemory(prefs.getInt("maxMemory", 2048));
        options.setMinMemory(prefs.getInt("minMemory", 512));
        options.setServerIp(prefs.getString("serverIp", ""));
        options.setWidth(prefs.getInt("width", 854));
        options.setHeight(prefs.getInt("height", 480));
        options.setFullscreen(prefs.getBoolean("fullscreen", false));
        return options;
    }
}
