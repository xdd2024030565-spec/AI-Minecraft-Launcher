package com.aimc.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aimc.launcher.databinding.ActivityMainBinding;
import com.aimc.launcher.service.AiControllerService;

/**
 * AI Minecraft Launcher 主界面
 *
 * 提供以下功能：
 * 1. 启动 Minecraft 游戏
 * 2. 控制 AI 控制器 (启动/停止)
 * 3. 查看游戏状态
 */
public class MainActivity extends AppCompatActivity {

    private lateinit var binding: ActivityMainBinding
    private AiControllerService.AiControllerBinder binder;
    private boolean isBound = false;

    companion object {
        private const val TAG = "AiMCLauncher"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置标题栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "AI Minecraft Launcher"

        // 启动 Minecraft 按钮
        binding.btnStartMinecraft.setOnClickListener {
            startMinecraft()
        }

        // 启动 AI 按钮
        binding.btnStartAi.setOnClickListener {
            startAiController()
        }

        // 停止 AI 按钮
        binding.btnStopAi.setOnClickListener {
            stopAiController()
        }

        // 查看日志按钮
        binding.btnViewLogs.setOnClickListener {
            openLogFile()
        }
    }

    private fun startMinecraft() {
        Toast.makeText(this, "正在启动 Minecraft...", Toast.LENGTH_SHORT).show()
        // TODO: 调用 FCL 或其他启动器启动游戏
        // 这里可以启动 FCL 的 Activity 或通过 Intent 调用
    }

    private fun startAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        startService(intent)
        Toast.makeText(this, "AI 控制器已启动", Toast.LENGTH_SHORT).show()
        binding.btnStartAi.isEnabled = false
        binding.btnStopAi.isEnabled = true
    }

    private fun stopAiController() {
        val intent = Intent(this, AiControllerService::class.java)
        stopService(intent)
        Toast.makeText(this, "AI 控制器已停止", Toast.LENGTH_SHORT).show()
        binding.btnStartAi.isEnabled = true
        binding.btnStopAi.isEnabled = false
    }

    private fun openLogFile() {
        // TODO: 打开日志文件
        Toast.makeText(this, "日志功能待实现", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
