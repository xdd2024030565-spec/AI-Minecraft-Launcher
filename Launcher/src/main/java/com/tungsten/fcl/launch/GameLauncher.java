package com.tungsten.fcl.launch;

import com.aimc.launcher.mod.ModInjector;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.auth.AccountManager;
import com.tungsten.fcl.game.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏启动器
 *
 * 负责构建 JVM 命令行参数并启动 Minecraft 进程。
 * 启动前自动注入 AI Bridge Mod。
 */
public class GameLauncher {

    private static final int DEFAULT_BRIDGE_PORT = 25580;

    private final FCLRepository repository;
    private final VersionManager versionManager;

    public GameLauncher(FCLRepository repository) {
        this.repository = repository;
        this.versionManager = VersionManager.getInstance(repository);
    }

    /**
     * 启动游戏
     *
     * @param versionId 要启动的版本 ID
     * @param account   使用的账户 (可为 null，使用离线账户)
     * @return 游戏进程
     */
    public Process launch(String versionId, AccountManager.Account account) throws Exception {
        // 检查版本是否已安装
        if (!versionManager.isVersionInstalled(versionId)) {
            throw new RuntimeException("版本未安装: " + versionId);
        }

        // 注入 AI Bridge Mod
        ModInjector.injectAiBridge(repository, versionId);

        // 构建命令行
        List<String> command = buildCommand(versionId, account);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(repository.getRootDir());
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * 构建 JVM 启动命令
     */
    private List<String> buildCommand(String versionId, AccountManager.Account account) {
        List<String> cmd = new ArrayList<>();

        // Java 可执行文件 (TODO: 使用内置 JRE)
        cmd.add("java");

        // JVM 内存参数
        cmd.add("-Xmx2G");
        cmd.add("-Xms512M");

        // AI Bridge 端口配置 (可通过 
        cmd.add("-Dai.bridge.port=" + DEFAULT_BRIDGE_PORT);

        // 类路径
        StringBuilder classpath = new StringBuilder();
        File versionJar = versionManager.getVersionJar(versionId);
        classpath.append(versionJar.getAbsolutePath());

        File[] libs = collectLibraries();
        for (File lib : libs) {
            classpath.append(":").append(lib.getAbsolutePath());
        }

        cmd.add("-cp");
        cmd.add(classpath.toString());

        // 主类
        cmd.add("net.minecraft.client.main.Main");

        // Minecraft 参数
        String username = (account != null && account.username != null)
                ? account.username : "Player";
        String uuid = (account != null && account.uuid != null)
                ? account.uuid : java.util.UUID.randomUUID().toString();
        String accessToken = (account != null && account.accessToken != null)
                ? account.accessToken : "0";

        cmd.add("--username");
        cmd.add(username);
        cmd.add("--uuid");
        cmd.add(uuid);
        cmd.add("--accessToken");
        cmd.add(accessToken);
        cmd.add("--version");
        cmd.add(versionId);
        cmd.add("--gameDir");
        cmd.add(repository.getRootDir().getAbsolutePath());
        cmd.add("--assetsDir");
        cmd.add(new File(repository.getRootDir(), "assets").getAbsolutePath());

        return cmd;
    }

    /**
     * 收集所有已下载的库文件
     */
    private File[] collectLibraries() {
        File libsDir = repository.getLibrariesDir();
        if (!libsDir.exists() || !libsDir.isDirectory()) {
            return new File[0];
        }
        return libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
    }
}
