package com.aimc.launcher

import android.content.Context
import android.content.SharedPreferences

/**
 * AI 配置管理
 */
object AiConfig {
    private const val PREFS_NAME = "ai_config"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_BRIDGE_PORT = "bridge_port"
    private const val KEY_TASK = "task"
    private const val KEY_CYCLE_INTERVAL = "cycle_interval"
    private const val KEY_VISUAL_MODE = "visual_mode"
    private const val KEY_MEMORY_ENABLED = "memory_enabled"
    private const val KEY_AGENT_MODE = "agent_mode"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(context: Context, key: String) =
        getPrefs(context).edit().putString(KEY_API_KEY, key).apply()
    fun getModel(context: Context): String =
        getPrefs(context).getString(KEY_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
    fun setModel(context: Context, model: String) =
        getPrefs(context).edit().putString(KEY_MODEL, model).apply()
    fun getBaseUrl(context: Context): String =
        getPrefs(context).getString(KEY_BASE_URL, "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/"
    fun setBaseUrl(context: Context, url: String) =
        getPrefs(context).edit().putString(KEY_BASE_URL, url).apply()
    fun getBridgePort(context: Context): Int =
        getPrefs(context).getInt(KEY_BRIDGE_PORT, 25580)
    fun setBridgePort(context: Context, port: Int) =
        getPrefs(context).edit().putInt(KEY_BRIDGE_PORT, port).apply()
    fun getTask(context: Context): String =
        getPrefs(context).getString(KEY_TASK, "Explore the world and survive. Collect resources, craft tools, build shelter, and stay alive.") ?: ""
    fun setTask(context: Context, task: String) =
        getPrefs(context).edit().putString(KEY_TASK, task).apply()
    fun getCycleInterval(context: Context): Long =
        getPrefs(context).getLong(KEY_CYCLE_INTERVAL, 2000L)
    fun setCycleInterval(context: Context, interval: Long) =
        getPrefs(context).edit().putLong(KEY_CYCLE_INTERVAL, interval).apply()
    fun getVisualMode(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_VISUAL_MODE, false)
    fun setVisualMode(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_VISUAL_MODE, enabled).apply()
    fun getMemoryEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_MEMORY_ENABLED, false)
    fun setMemoryEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
    fun getAgentMode(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AGENT_MODE, false)
    fun setAgentMode(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_AGENT_MODE, enabled).apply()
}
