package com.aimc.launcher

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * AI 控制器设置界面
 *
 * 配置项:
 * - LLM API Key / Model / Base URL
 * - AI Bridge 端口
 * - AI 任务描述
 * - 决策循环间隔
 * - 视觉模式 (截图+多模态LLM)
 * - 记忆系统
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var etApiKey: EditText
    private lateinit var etModel: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var etBridgePort: EditText
    private lateinit var etTask: EditText
    private lateinit var etCycleInterval: EditText
    private lateinit var switchVisualMode: SwitchMaterial
    private lateinit var switchMemory: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
    }

    private fun initViews() {
        etApiKey = findViewById(R.id.et_api_key)
        etModel = findViewById(R.id.et_model)
        etBaseUrl = findViewById(R.id.et_base_url)
        etBridgePort = findViewById(R.id.et_bridge_port)
        etTask = findViewById(R.id.et_task)
        etCycleInterval = findViewById(R.id.et_cycle_interval)
        switchVisualMode = findViewById(R.id.switch_visual_mode)
        switchMemory = findViewById(R.id.switch_memory)
    }

    private fun loadSettings() {
        etApiKey.setText(AiConfig.getApiKey(this))
        etModel.setText(AiConfig.getModel(this))
        etBaseUrl.setText(AiConfig.getBaseUrl(this))
        etBridgePort.setText(AiConfig.getBridgePort(this).toString())
        etTask.setText(AiConfig.getTask(this))
        etCycleInterval.setText(AiConfig.getCycleInterval(this).toString())
        switchVisualMode.isChecked = AiConfig.getVisualMode(this)
        switchMemory.isChecked = AiConfig.getMemoryEnabled(this)
    }

    private fun saveSettings() {
        AiConfig.setApiKey(this, etApiKey.text.toString().trim())
        AiConfig.setModel(this, etModel.text.toString().trim().ifEmpty { "gpt-4o-mini" })
        AiConfig.setBaseUrl(this, etBaseUrl.text.toString().trim().ifEmpty { "https://api.openai.com/v1/" })
        AiConfig.setBridgePort(this, etBridgePort.text.toString().trim().toIntOrNull() ?: 25580)
        AiConfig.setTask(this, etTask.text.toString().trim())
        AiConfig.setCycleInterval(this, etCycleInterval.text.toString().trim().toLongOrNull() ?: 2000L)
        AiConfig.setVisualMode(this, switchVisualMode.isChecked)
        AiConfig.setMemoryEnabled(this, switchMemory.isChecked)

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
