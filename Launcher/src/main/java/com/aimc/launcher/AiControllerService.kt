package com.aimc.controller

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.BatteryManager

/**
 * AI 控制器 Service
 *
 * 后台运行 AI 决策循环:
 * 1. 周期性查询 AI Bridge Mod 的 HTTP API
 * 2. 将游戏状态发送给 LLM 获取动作
 * 3. 执行动作控制 Minecraft
 *
 * 可以通过 startService / stopService 启动和停止。
 */
class AiControllerService : Service() {

    companion object {
        private const val TAG = "AiControllerService"
        private const val DECISION_INTERVAL_MS = 2000L // 每 2 秒决策一次

        const val ACTION_START = "com.aimc.controller.START_AI"
        const val ACTION_STOP = "com.aimc.controller.STOP_AI"
        const val EXTRA_TASK = "extra_task"
        const val EXTRA_API_KEY = "extra_api_key"
        const val EXTRA_MODEL = "extra_model"
        const val EXTRA_PORT = "extra_port"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var decisionJob: Job? = null

    private lateinit var gameApi: GameApiClient
    private lateinit var llmClient: LlmClient
    private lateinit var engine: DecisionEngine

    private var isRunning = false

    override fun onCreate() {
        super.onCreate()

        // 从 Intent 或 SharedPreferences 读取配置
        val prefs = getSharedPreferences("ai_config", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("openai_api_key", "") ?: ""
        val model = prefs.getString("llm_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        val port = prefs.getInt("bridge_port", 25580)

        // 初始化组件
        gameApi = GameApiClient(port = port)
        llmClient = LlmClient(
            apiKey = apiKey,
            model = model
        )
        engine = DecisionEngine(gameApi, llmClient)

        Log.i(TAG, "AiControllerService created. Port=$port, Model=$model")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val task = intent(EXTRA_TASK) ?: "Explore the world and survive. Collect resources, build shelter, and stay alive."
                startAi(task)
            }
            ACTION_STOP -> {
                stopAi()
                stopSelfResult()
            }
        }
        return START_NOT_STICKY
    }

    override fun IBinder? = null

    /**
     * 启动 AI 决策循环
     */
    private fun startAi(task: String) {
        if (isRunning) {
            Log.w(TAG, "AI is already running")
            return
        }

        isRunning = true
        engine.currentTask = task

        Log.i(TAG, "Starting AI with task: $task")

        decisionJob = serviceScope.launch {
            while (isActive && isRunning) {
                try {
                    val result = engine.runDecisionCycle()
                    when (result) {
                        is DecisionResult.Success -> {
                            Log.d(TAG, "Cycle ${result.cycleNumber}: executed ${result.actionsExecuted}")
                        }
                        is DecisionResult.Skip -> {
                            Log.d(TAG, "Cycle skipped: ${result.reason}")
                        }
                        is DecisionResult.Error -> {
                            Log.w(TAG, "Cycle error: ${result.message}")
                        }
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Decision cycle failed", e)
                    delay(5000) // 出错后等待再重试
                }

                delay(DECISION_INTERVAL_MS)
            }
        }

        // 前台通知 (可选，让 Service 更稳定)
        try {
            val notification = android.app.Notification.Builder(this, "ai_controller")
                .setSmallIcon(android.R.color.transparent)
                .setContentTitle("AI Minecraft Controller")
                .setContentText("AI is playing Minecraft...")
                .setPriority(android.app.Notification.PRIORITY_LOW)
                .build()
            startForeground(1001, notification)
        } catch (e: Exception) {
            // 忽略通知创建失败
        }
    }

    /**
     * 停止 AI 决策循环
     */
    private fun stopAi() {
        isRunning = false
        decisionJob?.cancel()
        decisionJob = null
        Log.i(TAG, "AI stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAi()
        serviceScope.cancel()
        Log.i(TAG, "AiControllerService destroyed")
    }
}