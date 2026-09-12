package com.aimc.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aimc.controller.DecisionEngine
import com.aimc.controller.GameApiClient
import com.aimc.controller.LlmClient
import com.aimc.launcher.AiConfig
import com.aimc.launcher.MainActivity
import com.aimc.launcher.R
import kotlinx.coroutines.*

/**
 * AI 控制器前台服务
 *
 * 在后台运行 AI 决策循环，持续与游戏内的 AI Bridge HTTP API 通信。
 * 配置通过 AiConfig (SharedPreferences) 读取。
 */
class AiControllerService : Service() {

    companion object {
        private const val TAG = "AiControllerService"
        private const val CHANNEL_ID = "ai_controller_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var decisionEngine: DecisionEngine? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("AI 控制器正在启动..."))
        if (!isRunning) {
            isRunning = true
            startDecisionLoop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startDecisionLoop() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 从 AiConfig 读取配置
                val apiKey = AiConfig.getApiKey(this@AiControllerService)
                val model = AiConfig.getModel(this@AiControllerService)
                val baseUrl = AiConfig.getBaseUrl(this@AiControllerService)
                val bridgePort = AiConfig.getBridgePort(this@AiControllerService)
                val task = AiConfig.getTask(this@AiControllerService)
                val cycleInterval = AiConfig.getCycleInterval(this@AiControllerService)

                if (apiKey.isEmpty()) {
                    updateNotification("错误: 未配置 API Key，请在设置中填写")
                    isRunning = false; stopForeground(true); stopSelf()
                    return@launch
                }

                val gameApi = GameApiClient(bridgePort)
                val llmClient = LlmClient(apiKey, model, baseUrl)
                decisionEngine = DecisionEngine(gameApi, llmClient)
                decisionEngine?.setCurrentTask(task)

                Log.i(TAG, "AI Controller initialized: port=$bridgePort, model=$model")

                updateNotification("正在等待 AI Bridge 连接...")
                waitForBridge(gameApi)
                updateNotification("AI 控制器已启动，正在控制游戏...")

                while (isActive && isRunning) {
                    try {
                        val result = decisionEngine?.runDecisionCycle()
                        when (result) {
                            is DecisionEngine.Success -> {
                                Log.i(TAG, "Cycle ${result.cycleNumber}: ${result.actionsExecuted}")
                                updateNotification("循环 #${result.cycleNumber} | 执行: ${result.actionsExecuted.joinToString(", ")}")
                            }
                            is DecisionEngine.Skip -> Log.d(TAG, "Skipped: ${result.reason}")
                            is DecisionEngine.Error -> { Log.e(TAG, "Error: ${result.message}"); updateNotification("错误: ${result.message}") }
                            null -> {}
                        }
                    } catch (e: Exception) { Log.e(TAG, "Decision cycle error", e) }
                    delay(cycleInterval)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AI controller", e)
                updateNotification("AI 控制器启动失败: ${e.message}")
            }
            isRunning = false; stopForeground(true); stopSelf()
        }
    }

    private suspend fun waitForBridge(gameApi: GameApiClient) {
        val maxRetries = 30
        var retries = 0
        while (retries < maxRetries && isRunning) {
            try { if (gameApi.getGameState().connected) return } catch (e: Exception) {}
            retries++; delay(3000)
        }
        if (retries >= maxRetries) throw RuntimeException("AI Bridge 连接超时")
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }
}
