package com.aimc.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AI Minecraft Launcher 主界面
 *
 * 提供以下功能：
 * 1. 启动 Minecraft 游戏 (通过 FCL)
 * 2. 启动/停止 AI 控制器
 * 3. 查看游戏状态
 */
public class MainActivity extends AppCompatActivity {

    private Button btnStartMinecraft;
    private Button btnStartAi;
    private Button btnStopAi;
    private Button btnViewLogs;
    private Button btnRefreshStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        // 启动 Minecraft 按钮
        btnStartMinecraft.setOnClickListener(v -> {
            Toast.makeText(this, "正在启动 Minecraft...", Toast.LENGTH_SHORT).show();
            // TODO: 调用 FCL 或其他启动器启动游戏
        });

        // 启动 AI 按钮
        btnStartAi.setOnClickListener(v -> {
            // 从 Intent 或 SharedPreferences 读取配置
            android.contentSharedPreferences prefs =
                getSharedPreferences("ai_config", MODE_PRIVATE);
            String apiKey = prefs.getString("openai_api_key", "") ?: "";
            String task = prefs.getString("task",
                "Explore the world, mine resources, and survive.") ?: "";
            String model = prefs.getString("llm_model", "gpt-4o-mini") ?: "gpt-4o-mini";
            int port = prefs.getInt("bridge_port", 25580);

            Intent intent = new Intent(this, AiControllerService.class);
            intent.setAction(AiControllerService.ACTION_START);
            intent.putExtra(AiControllerService.EXTRA_API_KEY, apiKey);
            intent.putExtra(AiControllerService.EXTRA_TASK, task);
            intent.putExtra(AiControllerService.EXTRA_MODEL, model);
            intent.putExtra(AiControllerService.EXTRA_PORT, port);
            startService(intent);

            btnStartAi.setEnabled(false);
            btnStopAi.setEnabled(true);
            Toast.makeText(this, "AI 控制器已启动", Toast.LENGTH_SHORT).show();
        });

        // 停止 AI 按钮
        btnStopAi.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiControllerService.class);
            intent.setAction(AiControllerService.ACTION_STOP);
            stopService(intent);

            btnStartAi.setEnabled(true);
            btnStopAi.setEnabled(false);
            Toast.makeText(this, "AI 控制器已停止", Toast.LENGTH_SHORT).show();
        });

        // 查看日志按钮
        btnViewLogs.setOnClickListener(v -> {
            Toast.makeText(this, "日志功能待实现", Toast.LENGTH_SHORT).show();
        });

        // 刷新状态按钮
        btnRefreshStatus.setOnClickListener(v -> {
            Toast.makeText(this, "状态已刷新", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        btnStartMinecraft = findViewById(R.id.btn_start_minecraft);
        btnStartAi = findViewById(R.id.btn_start_ai);
        btnStopAi = findViewById(R.id.btn_stop_ai);
        btnViewLogs = findViewById(R.id.btn_view_logs);
        btnRefreshStatus = findViewById(R.id.btn_refresh_status);
    }
}