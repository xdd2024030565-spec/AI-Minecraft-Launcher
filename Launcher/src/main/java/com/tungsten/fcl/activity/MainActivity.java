package com.tungsten.fcl.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * FCL 主 Activity
 *
 * 提供游戏版本列表、启动游戏、管理 Mod 等功能。
 * 这是 FCL 启动器的主入口。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 简单布局: 显示游戏版本列表
        Button btnLaunch = new Button(this);
        btnLaunch.setText("启动 Minecraft");
        btnLaunch.setOnClickListener(v -> {
            // 启动 Minecraft
            Toast.makeText(this, "启动 Minecraft...", Toast.LENGTH_SHORT).show();
            // TODO: 调用 FCL 启动逻辑
        });

        setContentView(btnLaunch);
    }
}