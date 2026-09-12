package com.aimc.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * AI Minecraft Launcher 主 Activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnStartMinecraft: Button
    private lateinit var btnStartAi: Button
    private lateinit var btnStopAi: Button
    private lateinit var btnSettings: Button
    private lateinit var tvAiStatus: TextView
    private var isAiRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        btnStartMinecraft.setOnClickListener { startMinecraft() }
        btnStartAi.setOnClickListener { startAiController() }
        btnStopAi.setOnClickListener { stopAiController() }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun initViews() {
        btnStartMinecraft = findViewById(R.id.btn_start_minecraft)
        btnStartAi = findViewById(R.id.btn_start_ai)
        btnStopAi = findViewById(R.id.btn_stop_ai)
        btnSettings = findViewById(R.id.btn_settings)
        tvAiStatus = findViewById(R.id.tv_ai_status)
    }

    private fun startMinecraft() {
        Toast.makeText(this, "正在启动 Minecraft...", Toast.LENGTH_SHORT).show()
        try {
            val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.MainActivity"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动 Minecraft: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        startService(intent)
        Toast.makeText(this, "AI 控制器已启动", Toast.LENGTH_SHORT).show()
        btnStartAi.isEnabled = false
        btnStopAi.isEnabled = true
        isAiRunning = true
        tvAiStatus.text = getString(R.string.ai_status_running)
    }

    private fun stopAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        stopService(intent)
        Toast.makeText(this, "AI 控制器已停止", Toast.LENGTH_SHORT).show()
        btnStartAi.isEnabled = true
        btnStopAi.isEnabled = false
        isAiRunning = false
        tvAiStatus.text = getString(R.string.ai_status_stopped)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isAiRunning) stopAiController()
    }
}
