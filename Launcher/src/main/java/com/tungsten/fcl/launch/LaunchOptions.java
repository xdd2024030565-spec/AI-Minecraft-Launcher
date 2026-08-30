package com.tungsten.fcl.launch;

/**
 * 启动选项
 *
 * 配置 Minecraft 启动时的各种参数，包括内存、分辨率、服务器等。
 * 支持 AI 模式配置。
 */
public class LaunchOptions {

    private String javaPath = "/system/bin/java";
    private int maxMemory = 4096;
    private int minMemory = 512;
    private int width = 854;
    private int height = 480;
    private boolean fullscreen = false;
    private String serverIp = "";
    private int serverPort = 25565;

    // AI 模式配置
    private boolean aiModeEnabled = false;
    private int aiBridgePort = 25580;
    private String aiTask = "Explore the world and survive";

    public LaunchOptions() {
    }

    // Getters and Setters
    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }

    public int getMaxMemory() { return maxMemory; }
    public void setMaxMemory(int maxMemory) { this.maxMemory = maxMemory; }

    public int getMinMemory() { return minMemory; }
    public void setMinMemory(int minMemory) { this.minMemory = minMemory; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    public boolean isAiModeEnabled() { return aiModeEnabled; }
    public void setAiModeEnabled(boolean aiModeEnabled) { this.aiModeEnabled = aiModeEnabled; }

    public int getAiBridgePort() { return aiBridgePort; }
    public void setAiBridgePort(int aiBridgePort) { this.aiBridgePort = aiBridgePort; }

    public String getAiTask() { return aiTask; }
    public void setAiTask(String aiTask) { this.aiTask = aiTask; }
}