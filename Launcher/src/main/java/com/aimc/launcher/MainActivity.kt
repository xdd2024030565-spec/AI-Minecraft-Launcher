package com.aimc.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aimc.launcher.service.AiControllerService
import com.tungsten.fcl.FCLApplication
import com.tungsten.fcl.FCLRepository

/**
 * AI Minecraft Launcher 主 Activity (增强版)
 *
 * 作为应用入口，提供进入 FCL 启动器和 AI 控制器的入口。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnStartMinecraft: Button
    private lateinit var btnStartAi: Button
    private lateinit var btnStopAi: Button
    private lateinit var btnSettings: Button
    private lateinit var btnGameDirectory: Button
    private lateinit var btnModSearch: Button
    private lateinit var btnAccount: Button
    private lateinit var tvAiStatus: TextView
    private lateinit var tvGameDir: TextView
    private var isAiRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateGameDirInfo()
    }

    override fun onResume() {
        super.onResume()
        updateGameDirInfo()
    }

    private fun initViews() {
        btnStartMinecraft = findViewById(R.id.btn_start_minecraft)
        btnStartAi = findViewById(R.id.btn_start_ai)
        btnStopAi = findViewById(R.id.btn_stop_ai)
        btnSettings = findViewById(R.id.btn_settings)
        btnGameDirectory = findViewById(R.id.btn_game_directory)
        btnModSearch = findViewById(R.id.btn_mod_search)
        btnAccount = findViewById(R.id.btn_account)
        tvAiStatus = findViewById(R.id.tv_ai_status)
        tvGameDir = findViewById(R.id.tv_game_dir)
    }

    private fun setupListeners() {
        // 进入 FCL 启动器主界面
        btnStartMinecraft.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.MainActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法启动: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // AI 控制器
        btnStartAi.setOnClickListener { startAiController() }
        btnStopAi.setOnClickListener { stopAiController() }

        // 设置
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 游戏目录
        btnGameDirectory.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.GameDirectoryActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Mod 搜索
        btnModSearch.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.ModSearchActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 账户管理
        btnAccount.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.tungsten.fcl.activity.AccountActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGameDirInfo() {
        try {
            val repo: FCLRepository = FCLApplication.getInstance().repository
            val versionsDir = repo.versionsDir
            val versionCount = if (versionsDir.exists() && versionsDir.isDirectory) {
                versionsDir.listFiles { f -> f.isDirectory }?.size ?: 0
            } else {
                0
            }
            tvGameDir.text = "游戏目录: ${repo.gameDirectoryPath}\n已安装版本: $versionCount"
        } catch (e: Exception) {
            tvGameDir.text = "游戏目录: 未初始化"
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
