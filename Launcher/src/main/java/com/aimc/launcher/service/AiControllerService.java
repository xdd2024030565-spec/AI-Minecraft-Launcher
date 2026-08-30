package com.aimc.launcher.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.aimc.controller.DecisionEngine;
import com.aimc.controller.GameApiClient;
import com.aimc.controller.LlmClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI 控制器 Service
 *
 * 后台运行 AI 决策循环:
 * 1. 周期性查询 AI Bridge Mod 的 HTTP API
 * 2. 将游戏状态发送给 LLM 获取动作
 * 3. 执行动作控制 Minecraft
 */
public class AiControllerService extends Service {

    private static final String TAG = "AiControllerService";
    private static final long DECISION_INTERVAL_MS = 2000L;

    public static final String ACTION_START = "com.aimc.controller.START_AI";
    public static final String ACTION_STOP = "com.aimc.controller.STOP_AI";
    public static final String EXTRA_TASK = "extra_task";
    public static final String EXTRA_API_KEY = "extra_api_key";
    public static final String EXTRA_MODEL = "extra_model";
    public static final String EXTRA_PORT = "extra_port";

    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private GameApiClient gameApi;
    private LlmClient llmClient;
    private DecisionEngine engine;

    @Override
    public void onCreate() {
        super.onCreate();

        // 从 SharedPreferences 读取配置
        SharedPreferences prefs =
            getSharedPreferences("ai_config", MODE_PRIVATE);
        String apiKey = prefs.getString("openai_api_key", "");
        String model = prefs.getString("llm_model", "gpt-4o-mini");
        int port = prefs.getInt("bridge_port", 25580);

        // 初始化组件
        gameApi = new GameApiClient(port);
        llmClient = new LlmClient(apiKey, model);
        engine = new DecisionEngine(gameApi, llmClient);

        Log.i(TAG, "AiControllerService created. Port=" + port + ", Model=" + model);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            String task = intent.getStringExtra(EXTRA_TASK);
            if (task == null || task.isEmpty()) {
                task = "Explore the world, mine resources, and survive.";
            }
            startAi(task);
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAi();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 启动 AI 决策循环
     */
    private void startAi(String task) {
        if (isRunning) {
            Log.w(TAG, "AI is already running");
            return;
        }

        isRunning = true;
        engine.setCurrentTask(task);

        Log.i(TAG, "Starting AI with task: " + task);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning) return;

            try {
                DecisionEngine.DecisionResult result = engine.runDecisionCycle();
                if (result instanceof DecisionEngine.Success) {
                    DecisionEngine.Success success = (DecisionEngine.Success) result;
                    Log.d(TAG, "Cycle " + success.cycleNumber
                        + ": executed " + success.actionsExecuted);
                } else if (result instanceof DecisionEngine.Skip) {
                    DecisionEngine.Skip skip = (DecisionEngine.Skip) result;
                    Log.d(TAG, "Cycle skipped: " + skip.reason);
                } else if (result instanceof DecisionEngine.Error) {
                    DecisionEngine.Error error = (DecisionEngine.Error) result;
                    Log.w(TAG, "Cycle error: " + error.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Decision cycle failed", e);
            }
        }, 0, DECISION_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止 AI 决策循环
     */
    private void stopAi() {
        isRunning = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        Log.i(TAG, "AI stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAi();
        Log.i(TAG, "AiControllerService destroyed");
    }
}