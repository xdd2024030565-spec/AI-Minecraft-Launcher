package com.aimc.ai_bridge;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地假人管理器 —— Carpet /player 风格
 *
 * 在本地存档（集成服务器）中生成真实的 ServerPlayerEntity 假人：
 * - 服务器将其视为真实玩家（出现在 TAB、能挖矿/战斗/被怪物攻击）
 * - 由 AI 控制器通过 HTTP API 驱动
 *
 * 限制：仅本地存档可用；远程服务器请使用「远程假人（小号实例）」。
 */
public final class FakePlayerManager {

    private static final Map<String, ServerPlayerEntity> FAKE_PLAYERS = new ConcurrentHashMap<>();

    private FakePlayerManager() {
    }

    // ==================== 生成 / 移除 ====================

    /**
     * 生成假人（出现在主世界指定坐标）
     *
     * @return 生成的假人；失败返回 null
     */
    public static ServerPlayerEntity spawn(MinecraftServer server, String name,
                                           double x, double y, double z,
                                           float yaw, float pitch) {
        if (server == null) {
            return null;
        }
        if (FAKE_PLAYERS.containsKey(name)) {
            remove(server, name);
        }

        ServerWorld world = server.getOverworld();
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("AIFake:" + name).getBytes()), name);

        try {
            AiFakePlayer fake = new AiFakePlayer(server, world, profile);
            fake.refreshPositionAndAngles(x, y, z, yaw, pitch);

            // 注册进 PlayerManager（服务器视为真实玩家加入）
            ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
            server.getPlayerManager().onPlayerConnect(connection, fake);

            FAKE_PLAYERS.put(name, fake);
            AiBridgeMod.LOGGER.info("[AI Bridge] 假人已生成: {} at [{}, {}, {}]", name, x, y, z);
            return fake;
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] 假人生成失败: " + name, e);
            return null;
        }
    }

    /**
     * 移除假人
     */
    public static boolean remove(MinecraftServer server, String name) {
        ServerPlayerEntity fake = FAKE_PLAYERS.remove(name);
        if (fake == null) {
            return false;
        }
        try {
            if (server != null) {
                server.getPlayerManager().remove(fake);
            }
            AiBridgeMod.LOGGER.info("[AI Bridge] 假人已移除: {}", name);
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] 假人移除失败: " + name, e);
        }
        return true;
    }

    public static void removeAll(MinecraftServer server) {
        for (String name : new ArrayList<>(FAKE_PLAYERS.keySet())) {
            remove(server, name);
        }
    }

    public static ServerPlayerEntity get(String name) {
        return FAKE_PLAYERS.get(name);
    }

    public static boolean has(String name) {
        return FAKE_PLAYERS.containsKey(name);
    }

    public static List<String> listNames() {
        return new ArrayList<>(FAKE_PLAYERS.keySet());
    }

    // ==================== 状态 ====================

    public static Map<String, Object> state(String name) {
        Map<String, Object> result = new HashMap<>();
        ServerPlayerEntity fp = FAKE_PLAYERS.get(name);
        if (fp == null) {
            result.put("online", false);
            return result;
        }
        result.put("online", true);
        result.put("name", fp.getName().getString());
        result.put("x", fp.getX());
        result.put("y", fp.getY());
        result.put("z", fp.getZ());
        result.put("yaw", fp.getYaw());
        result.put("pitch", fp.getPitch());
        result.put("health", fp.getHealth());
        result.put("dimension", fp.getWorld().getRegistryKey().getValue().toString());
        result.put("onGround", fp.isOnGround());
        return result;
    }

    // ==================== 动作执行 ====================

    /**
     * 执行假人动作（供 HTTP /api/fakeplayer/action 调用）
     */
    public static Map<String, Object> execute(String name, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        ServerPlayerEntity fp = FAKE_PLAYERS.get(name);
        if (fp == null) {
            result.put("success", false);
            result.put("message", "假人不存在: " + name);
            return result;
        }

        String action = request.get("action") == null ? "" : request.get("action").toString();
        try {
            switch (action) {
                case "goto": {
                    double x = toDouble(request.get("x"), fp.getX());
                    double y = toDouble(request.get("y"), fp.getY());
                    double z = toDouble(request.get("z"), fp.getZ());
                    float yaw = (float) toDouble(request.get("yaw"), fp.getYaw());
                    float pitch = (float) toDouble(request.get("pitch"), fp.getPitch());
                    tp(fp, x, y, z, yaw, pitch);
                    break;
                }
                case "look": {
                    float yaw = (float) toDouble(request.get("yaw"), fp.getYaw());
                    float pitch = (float) toDouble(request.get("pitch"), fp.getPitch());
                    tp(fp, fp.getX(), fp.getY(), fp.getZ(), yaw, pitch);
                    break;
                }
                case "lookAt": {
                    double x = toDouble(request.get("x"), fp.getX());
                    double y = toDouble(request.get("y"), fp.getY());
                    double z = toDouble(request.get("z"), fp.getZ());
                    lookAt(fp, x, y, z);
                    break;
                }
                case "mine": {
                    int bx = (int) Math.floor(toDouble(request.get("x"), fp.getX()));
                    int by = (int) Math.floor(toDouble(request.get("y"), fp.getY()));
                    int bz = (int) Math.floor(toDouble(request.get("z"), fp.getZ()));
                    fp.interactionManager.tryBreakBlock(new BlockPos(bx, by, bz));
                    break;
                }
                case "attack": {
                    LivingEntity target = findTarget(fp, toDouble(request.get("radius"), 6.0));
                    if (target == null) {
                        result.put("success", false);
                        result.put("message", "附近没有可攻击目标");
                        return result;
                    }
                    lookAt(fp, target.getX(), target.getEyeY(), target.getZ());
                    fp.attack(target);
                    break;
                }
                case "jump": {
                    if (fp.isOnGround()) {
                        fp.jump();
                    }
                    break;
                }
                case "sneak": {
                    fp.setSneaking(toBool(request.get("value"), true));
                    break;
                }
                case "sprint": {
                    fp.setSprinting(toBool(request.get("value"), true));
                    break;
                }
                case "chat": {
                    String message = request.get("message") == null ? "" : request.get("message").toString();
                    if (!message.isEmpty()) {
                        fp.getServer().getPlayerManager().broadcast(
                                Text.literal("<" + name + "> " + message), false);
                    }
                    break;
                }
                default: {
                    result.put("success", false);
                    result.put("message", "未知假人动作: " + action);
                    return result;
                }
            }
            result.put("success", true);
            result.put("action", action);
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
            return result;
        }
    }

    // ==================== 内部工具 ====================

    private static void tp(ServerPlayerEntity fp, double x, double y, double z, float yaw, float pitch) {
        try {
            fp.networkHandler.requestTeleport(x, y, z, yaw, pitch);
        } catch (Exception e) {
            fp.refreshPositionAndAngles(x, y, z, yaw, pitch);
        }
    }

    private static void lookAt(ServerPlayerEntity fp, double x, double y, double z) {
        double dx = x - fp.getX();
        double dy = y - fp.getEyeY();
        double dz = z - fp.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        tp(fp, fp.getX(), fp.getY(), fp.getZ(), yaw, pitch);
    }

    private static LivingEntity findTarget(ServerPlayerEntity fp, double radius) {
        Box box = fp.getBoundingBox().expand(radius);
        List<Entity> entities = fp.getWorld().getOtherEntities(fp, box);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            if (entity instanceof ServerPlayerEntity) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (!living.isAlive()) {
                continue;
            }
            double dist = fp.squaredDistanceTo(entity);
            // 优先怪物，其次其它生物
            double score = entity instanceof Monster ? dist : dist + 1000.0;
            if (score < bestDist) {
                bestDist = score;
                best = living;
            }
        }
        return best;
    }

    private static double toDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean toBool(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }
}
