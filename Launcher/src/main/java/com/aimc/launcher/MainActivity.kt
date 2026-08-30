package com.aimc.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * AI Minecraft Launcher 主 Activity
 *
 * 提供以下功能：
 * 1. 启动 Minecraft 游戏 (通过 FCL)
 * 2. 启动/停止 AI 控制器
 * 3. 查看游戏状态
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnStartMinecraft: Button
    private lateinit var btnStartAi: Button
    private lateinit var btnStopAi: Button
    private var isAiRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        // 启动 Minecraft 按钮
        btnStartMinecraft.setOnClickListener {
            startMinecraft()
        }

        // 启动 AI 按钮
        btnStartAi.setOnClickListener {
            startAiController()
        }

        // 停止 AI 按钮
        btnStopAi.setOnClickListener {
            stopAiController()
        }
    }

    private fun initViews() {
        btnStartMinecraft = findViewById(R.id.btn_start_minecraft)
        btnStartAi = findViewById(R.id.btn_start_ai)
        btnStopAi = findViewById(R.id.btn_stop_ai)
    }

    private fun startMinecraft() {
        Toast.makeText(this, "正在启动 Minecraft...", Toast.LENGTH_SHORT).show()

        // 启动 FCL 的主 Activity
        try {
            val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.MainActivity"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动 Minecraft: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun startAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        startService(intent)
        Toast.makeText(this, "AI 控制器已启动", Toast.LENGTH_SHORT).show()
        btnStartAi.isEnabled = false
        btnStopAi.isEnabled = true
        isAiRunning = true
    }

    private fun stopAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        stopService(intent)
        Toast.makeText(this, "AI 控制器已停止", Toast.LENGTH_SHORT).show()
        btnStartAi.isEnabled = true
        btnStopAi.isEnabled = false
        isAiRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isAiRunning) {
            stopAiController()
        }
    }
}