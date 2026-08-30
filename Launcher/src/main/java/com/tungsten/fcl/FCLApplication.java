package com.tungsten.fcl;

import android.app.Application;
import android.content.Context;

/**
 * FCL Application 类
 *
 * 作为 FCL 启动器的 Application 入口。
 * 负责初始化全局状态和依赖注入。
 */
public class FCLApplication extends Application {

    private static FCLApplication instance;
    private FCLRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化 Repository
        repository = FCLRepository.getInstance(this);
    }

    public static FCLApplication getInstance() {
        return instance;
    }

    public FCLRepository getRepository() {
        return repository;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}