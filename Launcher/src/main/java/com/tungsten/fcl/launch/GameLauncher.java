package com.tungsten.fcl.launch;

import com.aimc.launcher.mod.ModInjector;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.auth.AccountManager;
import com.tungsten.fcl.game.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏启动器 (重写版) — 仿 FCL DefaultLauncher
 *
 * 负责构建 JVM 命令行参数并启动 Minecraft 进程。
 * 支持从版本设置读取配置，自动注入 AI Bridge Mod。
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
     * 启动游戏 (使用默认设置)
     */
    public Process launch(String versionId, AccountManager.Account account) throws Exception {
        return launch(versionId, account, new LaunchOptions());
    }

    /**
     * 启动游戏 (使用自定义设置)
     */
    public Process launch(String versionId, AccountManager.Account account, LaunchOptions options) throws Exception {
        // 检查版本是否已安装
        if (!versionManager.isVersionInstalled(versionId)) {
            throw new RuntimeException("版本未安装: " + versionId);
        }

        // 注入 AI Bridge Mod
        ModInjector.injectAiBridge(repository, versionId);

        // 构建命令行
        List<String> command = buildCommand(versionId, account, options);

        ProcessBuilder pb = new ProcessBuilder(command);
        // 使用版本目录或根目录
        File workingDir = options.isIsolateGameDir()
                ? repository.getVersionDir(versionId)
                : repository.getRootDir();
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * 构建 JVM 启动命令 — 仿 FCL DefaultLauncher.generateCommandLine
     */
    private List<String> buildCommand(String versionId, AccountManager.Account account, LaunchOptions options) {
        List<String> cmd = new ArrayList<>();

        // Java 可执行文件
        cmd.add(options.getJavaPath());

        // JVM 内存参数
        cmd.add("-Xmx" + options.getMaxMemory() + "m");
        if (options.getMinMemory() > 0) {
            cmd.add("-Xms" + options.getMinMemory() + "m");
        }

        // 原生库路径
        cmd.add("-Djava.library.path=" + repository.getLibrariesDir().getAbsolutePath());

        // 启动器标识
        cmd.add("-Dminecraft.launcher.brand=FCL");
        cmd.add("-Dminecraft.launcher.version=1.0.0");

        // AI Bridge 端口配置
        if (options.isAiModeEnabled()) {
            cmd.add("-Dai.bridge.enabled=true");
            cmd.add("-Dai.bridge.port=" + options.getAiBridgePort());
        } else {
            cmd.add("-Dai.bridge.port=" + DEFAULT_BRIDGE_PORT);
        }

        // 额外 JVM 参数
        if (options.getExtraJavaArgs() != null && !options.getExtraJavaArgs().isEmpty()) {
            for (String arg : options.getExtraJavaArgs().split("\\s+")) {
                if (!arg.isEmpty()) cmd.add(arg);
            }
        }

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
        cmd.add("--versionType");
        cmd.add("FCL");

        // 游戏目录
        File gameDir = options.isIsolateGameDir()
                ? repository.getVersionDir(versionId)
                : repository.getRootDir();
        cmd.add("--gameDir");
        cmd.add(gameDir.getAbsolutePath());

        // 资源目录
        cmd.add("--assetsDir");
        cmd.add(repository.getAssetsDir().getAbsolutePath());

        // 分辨率
        cmd.add("--width");
        cmd.add(String.valueOf(options.getWidth()));
        cmd.add("--height");
        cmd.add(String.valueOf(options.getHeight()));

        // 全屏
        if (options.isFullscreen()) {
            cmd.add("--fullscreen");
        }

        // 服务器连接
        if (options.getServerIp() != null && !options.getServerIp().isEmpty()) {
            String[] parts = options.getServerIp().split(":");
            cmd.add("--server");
            cmd.add(parts[0]);
            if (parts.length > 1) {
                cmd.add("--port");
                cmd.add(parts[1]);
            } else {
                cmd.add("--port");
                cmd.add("25565");
            }
        }

        // 额外 Minecraft 参数
        if (options.getExtraMinecraftArgs() != null && !options.getExtraMinecraftArgs().isEmpty()) {
            for (String arg : options.getExtraMinecraftArgs().split("\\s+")) {
                if (!arg.isEmpty()) cmd.add(arg);
            }
        }

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
        // 递归收集所有 .jar 文件
        List<File> jars = new ArrayList<>();
        collectJars(libsDir, jars);
        return jars.toArray(new File[0]);
    }

    private void collectJars(File dir, List<File> jars) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".jar")) {
                jars.add(f);
            } else if (f.isDirectory()) {
                collectJars(f, jars);
            }
        }
    }
}
