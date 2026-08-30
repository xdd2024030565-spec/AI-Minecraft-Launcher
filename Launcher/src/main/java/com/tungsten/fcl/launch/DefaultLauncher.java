package com.tungsten.fcl.launch;

import android.content.Context;

import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.auth.AuthInfo;
import com.tungsten.fcl.game.Version;

/**
 * 默认启动器
 *
 * 负责构造 Minecraft 启动命令行参数并启动游戏进程。
 * 这是 FCL 启动器的核心类。
 */
public class DefaultLauncher {

    private final Context context;
    private final FCLRepository repository;
    private final Version version;
    private final AuthInfo authInfo;
    private final LaunchOptions options;

    public DefaultLauncher(Context context, FCLRepository repository,
                           Version version, AuthInfo authInfo,
                           LaunchOptions options) {
        this.context = context;
        this.repository = repository;
        this.version = version;
        this.authInfo = authInfo;
        this.options = options;
    }

    /**
     * 生成 Minecraft 启动命令行参数
     *
     * @return 命令行参数数组
     */
    public String[] generateCommandLine() {
        java.util.List<String> command = new java.util.ArrayList<>();

        // Java 可执行文件
        command.add(options.getJavaPath());

        // JVM 参数
        command.add("-Xmx" + options.getMaxMemory() + "m");
        command.add("-Xms" + options.getMinMemory() + "m");
        command.add("-Djava.library.path=" + repository.getLibrariesDir().getAbsolutePath());
        command.add("-Dminecraft.launcher.brand=FCL");
        command.add("-Dminecraft.launcher.version=1.0.0");

        // Minecraft 参数
        command.add("--version");
        command.add(version.getId());
        command.add("--username");
        command.add(authInfo.getUsername());
        command.add("--uuid");
        command.add(authInfo.getUUID().toString());
        command.add("--accessToken");
        command.add(authInfo.getAccessToken());
        command.add("--clientid");
        command.add("0");
        command.add("--versionType");
        command.add(version.getType());

        // 游戏目录
        command.add("--gameDir");
        command.add(repository.getVersionDir(version.getId()).getAbsolutePath());

        // 资源目录
        command.add("--assetsDir");
        command.add(repository.getResourceDir().getAbsolutePath());

        // 库目录
        command.add("--libraryDir");
        command.add(repository.getLibrariesDir().getAbsolutePath());

        // 分辨率
        command.add("--width");
        command.add(String.valueOf(options.getWidth()));
        command.add("--height");
        command.add(String.valueOf(options.getHeight()));

        // 全屏
        if (options.isFullscreen()) {
            command.add("--fullscreen");
        }

        // 服务器连接
        if (options.getServerIp() != null && !options.getServerIp().isEmpty()) {
            command.add("--server");
            command.add(options.getServerIp());
            if (options.getServerPort() > 0) {
                command.add("--port");
                command.add(String.valueOf(options.getServerPort()));
            }
        }

        // AI Bridge Mod 注入
        if (options.isAiModeEnabled()) {
            command.add("-Dai.bridge.enabled=true");
            command.add("-Dai.bridge.port=" + options.getAiBridgePort());
        }

        return command.toArray(new String[0]);
    }

    /**
     * 启动 Minecraft
     */
    public Process launch() throws Exception {
        String[] command = generateCommandLine();

        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(repository.getRootDir())
            .redirectErrorStream(false);

        return builder.start();
    }
}