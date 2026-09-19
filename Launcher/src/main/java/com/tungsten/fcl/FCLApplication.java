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
    private com.tungsten.fcl.auth.AccountManager accountManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化 Repository (游戏文件仓库)
        repository = FCLRepository.getInstance(this);

        // 初始化 AccountManager (加载已保存的账户)
        accountManager = com.tungsten.fcl.auth.AccountManager.getInstance();
        accountManager.loadFromPreferences(this);
    }

    public static FCLApplication getInstance() {
        return instance;
    }

    public FCLRepository getRepository() {
        return repository;
    }

    public com.tungsten.fcl.auth.AccountManager getAccountManager() {
        return accountManager;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
