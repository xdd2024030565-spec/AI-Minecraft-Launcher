package com.aimc.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * AI 控制器服务
 *
 * 核心功能：
 * 1. 定期获取游戏状态
 * 2. 构建 Prompt 并发送到 LLM
 * 3. 解析返回的动作并执行
 *
 * 默认决策间隔: 2秒
 */
class AiControllerService : Service() {

    companion object {
        private const val TAG = "AiControllerService"
        private const val CHANNEL_ID = "ai_minecraft_channel"
        private const val DECISION_INTERVAL_MS = 2000L
    }

    // ==================== 本地绑定器 ====================
    inner class LocalBinder : Binder() {
        fun getService(): AiControllerService = this@AiControllerService
    }

    private val binder = LocalBinder()

    // ==================== 状态管理 ====================
    private var isRunning = false
    private val jobScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var decisionJob: Job? = null

    // ==================== HTTP 客户端 ====================
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // ==================== 游戏 API 端点 ====================
    private val apiBaseUrl = "http://127.0.0.1:25580"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动")
        isRunning = true

        // 开始决策循环
        decisionJob = jobScope.launch {
            aiDecisionLoop()
        }

        // 显示前台通知
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "服务销毁")
        isRunning = false
        decisionJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ==================== 核心决策循环 ====================
    private suspend fun aiDecisionLoop() {
        while (isRunning) {
            try {
                // 1. 获取游戏状态
                val gameState = fetchGameState()
                if (!gameState?.getBoolean("connected") ?: true) {
                    Log.w(TAG, "游戏未连接，等待...")
                    delay(5000)
                    continue
                }

                // 2. 获取背包信息
                val inventory = fetchJson("/api/inventory")

                // 3. 获取附近方块
                val blocks = fetchJson("/api/blocks?radius=5")

                // 4. 构建 Prompt
                val prompt = buildPrompt(gameState, inventory, blocks)

                // 5. 调用 LLM 获取动作
                val actions = callLLM(prompt)
                Log.d(TAG, "LLM 返回 ${actions?.length ?: 0} 个动作")

                // 6. 执行动作
                executeActions(actions)

            } catch (e: Exception) {
                Log.e(TAG, "决策循环异常: ${e.message}")
            } finally {
                delay(DECISION_INTERVAL_MS)
            }
        }
    }

    // ==================== API 调用 ====================
    private suspend fun fetchGameState(): JSONObject? = try {
        fetchJson("/api/state")
    } catch (e: Exception) {
        Log.e(TAG, "获取状态失败: ${e.message}")
        null
    }

    private suspend fun fetchJson(path: String): JSONObject? = try {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$apiBaseUrl$path")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                JSONObject(body)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "API 调用失败: ${e.message}")
        null
    }

    private suspend fun postJson(path: String, data: JSONObject): JSONObject? = try {
        withContext(Dispatchers.IO) {
            val body = data.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$apiBaseUrl$path")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseBody = response.body?.string() ?: return@withContext null
                JSONObject(responseBody)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "POST 失败: ${e.message}")
        null
    }

    // ==================== Prompt 构建 ====================
    private fun buildPrompt(
        state: JSONObject?,
        inventory: JSONObject?,
        blocks: JSONObject?
    ): String {
        val sb = StringBuilder()

        // 任务说明
        sb.appendLine("你是 AI Minecraft 玩家。你的当前任务: 生存并收集资源。")
        sb.appendLine()

        // 游戏状态
        sb.appendLine("=== 当前状态 ===")
        if (state != null) {
            sb.appendLine("位置: ${state.optDouble("position", -1).let { if (it < 0) "未知" else "[$it]" }}")
            sb.appendLine("生命: ${state.optDouble("health")}/20")
            sb.appendLine("饥饿: ${state.optInt("food")}/20")
            sb.appendLine("经验: ${state.optInt("xpLevel")}")
            sb.appendLine("维度: ${state.optString("dimension")}")
            sb.appendLine("时间: ${if (state.optBoolean("isDay")) "白天" else "夜晚"}")
        }

        // 背包
        sb.appendLine("\n=== 背包 ===")
        if (inventory != null) {
            val main = inventory.optJSONArray("mainInventory")
            if (main != null) {
                for (i in 0 until main.length()) {
                    val item = main.optJSONObject(i)
                    if (item != null) {
                        val name = item.optString("name", "未知")
                        val count = item.optInt("count", 0)
                        if (count > 0 && name != "air") {
                            sb.appendLine("- $countx $name")
                        }
                    }
                }
            }
        }

        // 附近方块
        sb.appendLine("\n=== 附近方块 ===")
        if (blocks != null) {
            val summary = blocks.optJSONObject("blockSummary")
            if (summary != null) {
                summary.keys().forEach { name ->
                    val count = summary.optInt(name, 0)
                    sb.appendLine("- $countx $name")
                }
            }
        }

        // 可用动作
        sb.appendLine("\n=== 可用动作 ===")
        sb.appendLine("- move: {direction: forward/back/left/right, duration: ticks}")
        sb.appendLine("- look: {yaw: -180~180, pitch: -90~90}")
        sb.appendLine("- jump: {}")
        sb.appendLine("- mine: {duration: ticks}")
        sb.appendLine("- place: {}")
        sb.appendLine("- attack: {}")
        sb.appendLine("- use: {}")
        sb.appendLine("- inventory_click: {slot: number, button: left/right}")
        sb.appendLine("- drop_item: {slot: number}")
        sb.appendLine("- toggle_sprint: {}")
        sb.appendLine("- chat: {message: string}")

        sb.appendLine("\n返回 JSON 数组格式:")
        sb.appendLine("[{\"action\":\"move\",\"direction\":\"forward\",\"duration\":10}]")

        return sb.toString()
    }

    // ==================== LLM 调用 (示例: OpenAI) ====================
    private suspend fun callLLM(prompt: String): String? {
        return try {
            // TODO: 支持多种 LLM (OpenAI, Claude, DeepSeek)
            withContext(Dispatchers.IO) {
                val body = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("temperature", 0.7)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "你是一个专业的 Minecraft 玩家 AI。")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }.toString()

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "LLM 调用失败: ${response.code}")
                        return@withContext null
                    }
                    val respBody = response.body?.string() ?: return@withContext null
                    val json = JSONObject(respBody)
                    val choices = json.getJSONArray("choices")
                    if (choices.length() > 0) {
                        choices.getJSONObject(0).getJSONObject("message").optString("content")
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM 调用异常: ${e.message}")
            null
        }
    }

    private fun getApiKey(): String =
        getSharedPreferences("ai_config", MODE_PRIVATE)
            .getString("openai_api_key", "") ?: ""

    // ==================== 动作执行 ====================
    private suspend fun executeActions(llmResponse: String?) {
        if (llmResponse.isNullOrEmpty()) return

        try {
            val actions = org.json.JSONArray(llmResponse)
            for (i in 0 until actions.length()) {
                val action = actions.getJSONObject(i)
                val actionType = action.optString("action")
                val success = postJson("/api/action", action)?.optBoolean("success") ?: false
                Log.d(TAG, "执行 $actionType: $success")
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行动作失败: ${e.message}")
        }
    }

    // ==================== 通知 ====================
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI 控制器",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AI Minecraft Launcher 后台服务"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 控制器运行中")
            .setContentText("正在控制 Minecraft 玩家...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
