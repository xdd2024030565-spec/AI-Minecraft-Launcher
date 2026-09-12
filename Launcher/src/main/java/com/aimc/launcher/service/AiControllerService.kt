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
import com.aimc.controller.*
import com.aimc.launcher.AiConfig
import com.aimc.launcher.MainActivity
import com.aimc.launcher.R
import kotlinx.coroutines.*

/**
 * AI 控制器前台服务
 *
 * 支持单智能体和多智能体协作模式。
 */
class AiControllerService : Service() {

    companion object {
        private const val TAG = "AiControllerService"
        private const val CHANNEL_ID = "ai_controller_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var decisionEngine: DecisionEngine? = null
    private var agentManager: AiAgentManager? = null
    private var isRunning = false
    private var agentMode = false

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
        if (agentMode && agentManager != null) {
            agentManager?.shutdown()
        }
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
                val apiKey = AiConfig.getApiKey(this@AiControllerService)
                val model = AiConfig.getModel(this@AiControllerService)
                val baseUrl = AiConfig.getBaseUrl(this@AiControllerService)
                val bridgePort = AiConfig.getBridgePort(this@AiControllerService)
                val task = AiConfig.getTask(this@AiControllerService)
                val cycleInterval = AiConfig.getCycleInterval(this@AiControllerService)
                val agentModeEnabled = AiConfig.getAgentMode(this@AiControllerService)

                if (apiKey.isEmpty()) {
                    updateNotification("错误: 未配置 API Key，请在设置中填写")
                    isRunning = false; stopForeground(true); stopSelf()
                    return@launch
                }

                val gameApi = GameApiClient(bridgePort)
                val llmClient = LlmClient(apiKey, model, baseUrl)

                if (agentModeEnabled) {
                    // 多智能体模式
                    agentManager = AiAgentManager()
                    agentMode = true

                    // 添加预设智能体
                    for (role in AiAgentManager.PresetRoles.getPresetNames()) {
                        agentManager?.addPresetAgent(role, gameApi, llmClient)
                    }

                    Log.i(TAG, "Multi-agent mode: " + agentManager?.getAgentCount() + " agents")
                    updateNotification("多智能体模式: ${agentManager?.getAgentCount()} 个智能体")
                } else {
                    // 单智能体模式
                    decisionEngine = DecisionEngine(gameApi, llmClient)
                    decisionEngine?.setCurrentTask(task)
                    agentMode = false
                    Log.i(TAG, "Single-agent mode")
                    updateNotification("AI 控制器已启动")
                }

                updateNotification("正在等待 AI Bridge 连接...")
                waitForBridge(gameApi)
                updateNotification("AI 控制器已启动，正在控制游戏...")

                while (isActive && isRunning) {
                    try {
                        if (agentMode && agentManager != null) {
                            // 多智能体: 轮询运行
                            val result = agentManager?.runNextTurn()
                            Log.d(TAG, result ?: "No agents active")
                        } else if (decisionEngine != null) {
                            // 单智能体
                            val result = decisionEngine?.runDecisionCycle()
                            when (result) {
                                is DecisionEngine.Success -> {
                                    Log.i(TAG, "Cycle ${result.cycleNumber}: ${result.actionsExecuted}")
                                }
                                is DecisionEngine.Skip -> Log.d(TAG, "Skipped: ${result.reason}")
                                is DecisionEngine.Error -> Log.e(TAG, "Error: ${result.message}")
                                null -> {}
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Decision cycle error", e)
                    }
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
