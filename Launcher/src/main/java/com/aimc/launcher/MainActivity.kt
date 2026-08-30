package com.aimc.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AI Minecraft Launcher 主界面
 *
 * 提供以下功能：
 * 1. 配置 LLM API Key 和任务
 * 2. 启动/停止 AI 控制器
 * 3. 查看游戏状态
 */
public class MainActivity extends AppCompatActivity {

    private EditText etApiKey;
    private EditText etTask;
    private EditText etModel;
    private EditText etPort;
    private Switch switchAutoStart;
    private Button btnStartAi;
    private Button btnStopAi;
    private Button btnOpenSettings;
    private boolean isAiRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadConfig();

        btnStartAi.setOnClickListener(v -> {
            saveConfig();
            String task = etTask.getText().toString().isEmpty()
                ? "Explore the world, mine resources, and survive."
                : etTask.getText().toString();

            Intent intent = new Intent(this, AiControllerService.class);
            intent.setAction(AiControllerService.ACTION_START);
            intent.putExtra(AiControllerService.EXTRA_TASK, task);
            startService(intent);

            isAiRunning = true;
            btnStartAi.setEnabled(false);
            btnStopAi.setEnabled(true);
            Toast.makeText(this, "AI started", Toast.LENGTH_SHORT).show();
        });

        btnStopAi.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiControllerService.class);
            intent.setAction(AiControllerService.ACTION_STOP);
            stopService(intent);

            isAiRunning = false;
            btnStartAi.setEnabled(true);
            btnStopAi.setEnabled(false);
            Toast.makeText(this, "AI stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        etApiKey = findViewById(R.id.et_api_key);
        etTask = findViewById(R.id.et_task);
        etModel = findViewById(R.id.et_model);
        etPort = findViewById(R.id.et_port);
        switchAutoStart = findViewById(R.id.switch_auto_start);
        btnStartAi = findViewById(R.id.btn_start_ai);
        btnStopAi = findViewById(R.id.btn_stop_ai);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
    }

    private void loadConfig() {
        android.contentSharedPreferences prefs =
            getSharedPreferences("ai_config", MODE_PRIVATE);
        etApiKey.setText(prefs.getString("openai_api_key", "") ?: "");
        etTask.setText(prefs.getString("task",
            "Explore the world, mine resources, and survive.") ?: "");
        etModel.setText(prefs.getString("llm_model", "gpt-4o-mini") ?: "");
        etPort.setText(String.valueOf(prefs.getInt("bridge_port", 25580)));
    }

    private void saveConfig() {
        android.contentSharedPreferences.Editor editor =
            getSharedPreferences("ai_config", MODE_PRIVATE).edit();
        editor.putBoolean("ai_enabled", true);
        editor.apply();
    }
}