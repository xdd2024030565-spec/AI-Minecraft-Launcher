package com.aimc.ai_bridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 游戏状态收集器
 *
 * 收集玩家当前的核心信息：位置、朝向、生命值、饥饿值、经验等级、维度、世界时间等。
 * 在每 tick 结束时更新缓存，供 HTTP API 快速返回。
 */
public class GameStateCollector {

    // tick 缓存（由 AiBridgeMod 的 ClientTickEvents 更新）
    private static GameState tickCache;

    /**
     * 在每 tick 结束时更新缓存
     * 由 AiBridgeMod.onInitialize() 中注册的 ClientTickEvents 回调调用
     */
    public static void updateTickCache(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            tickCache = new GameState(false);
            return;
        }
        tickCache = collect();
    }

    /**
     * 实时收集当前游戏状态
     */
    public static GameState collect() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return new GameState(false);
        }

        ClientPlayerEntity player = mc.player;
        World world = mc.world;

        return new GameState(
                true,
                new double[]{player.getX(), player.getY(), player.getZ()},
                player.getHealth(),
                player.getHungerManager().getFoodLevel(),
                player.getHungerManager().getSaturationLevel(),
                player.experienceLevel,
                player.experienceProgress,
                new double[]{player.getYaw(), player.getPitch()},
                world.getRegistryKey().getValue().toString(),
                world.getTimeOfDay(),
                world.getTimeOfDay() < 13000,
                player.isSprinting(),
                player.isSneaking(),
                player.isOnGround(),
                player.isTouchingWater(),
                mc.world != null && mc.world.getDimension().hasSkyLight(),
                player.getServer() != null ? player.getServer().getServerPort() : 0
        );
    }

    /**
     * 获取 Minecraft 版本号
     */
    public static String getMinecraftVersion() {
        try {
            return MinecraftClient.getInstance().getGame().getVersion().getName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 游戏状态数据结构
     */
    public static class GameState {
        public boolean connected;
        public double[] position;        // [x, y, z]
        public float health;            // 0-20
        public int food;                 // 0-20
        public float saturation;        // 饱和度
        public int xpLevel;             // 经验等级
        public float xpProgress;        // 经验进度 0-1
        public double[] rotation;       // [yaw, pitch]
        public String dimension;        // "minecraft:overworld" 等
        public long worldTime;          // 世界时间 (ticks)
        public boolean isDay;           // 是否白天
        public boolean sprinting;       // 是否冲刺
        public boolean sneaking;        // 是否潜行
        public boolean onGround;        // 是否在地面
        public boolean inWater;         // 是否在水中
        public boolean hasSkyLight;     // 是否有天空光照
        public int serverPort;          // 服务器端口 (0 = 单机)

        // 无参构造（未连接时）
        public GameState(boolean connected) {
            this.connected = connected;
        }

        public GameState(boolean connected, double[] position, float health,
                         int food, float saturation, int xpLevel, float xpProgress,
                         double[] rotation, String dimension, long worldTime,
                         boolean isDay, boolean sprinting, boolean sneaking,
                         boolean onGround, boolean inWater, boolean hasSkyLight,
                         int serverPort) {
            this.connected = connected;
            this.position = position;
            this.health = health;
            this.food = food;
            this.saturation = saturation;
            this.xpLevel = xpLevel;
            this.xpProgress = xpProgress;
            this.rotation = rotation;
            this.dimension = dimension;
            this.worldTime = worldTime;
            this.isDay = isDay;
            this.sprinting = sprinting;
            this.sneaking = sneaking;
            this.onGround = onGround;
            this.inWater = inWater;
            this.hasSkyLight = hasSkyLight;
            this.serverPort = serverPort;
        }
    }
}
