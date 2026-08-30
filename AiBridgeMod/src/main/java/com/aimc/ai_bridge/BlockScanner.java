package com.aimc.ai_bridge;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * 方块扫描器
 *
 * 扫描玩家附近的方块和实体信息。
 * 返回相对坐标 (相对于玩家位置的偏移)，方便 AI 理解空间关系。
 */
public class BlockScanner {

    /**
     * 扫描附近方块和实体
     *
     * @param radius 扫描半径 (最大 20)
     * @return 扫描结果
     */
    public static BlockScanResult scan(int radius) {
        // 限制最大半径，避免性能问题
        radius = Math.min(Math.max(radius, 1), 20);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return new BlockScanResult(false, null, null, null, radius);
        }

        BlockPos playerPos = mc.player.getBlockPos();
        World world = mc.world;

        // 收集非空气方块
        List<BlockInfo> blocks = new ArrayList<>();
        Map<String, Integer> blockCounts = new LinkedHashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();

                    if (block != Blocks.AIR) {
                        String name = block.toString();

                        // 只保存靠近玩家的方块 (避免列表太长)
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance <= radius) {
                            blocks.add(new BlockInfo(
                                name,
                                x, y, z
                            ));
                        }

                        // 统计方块数量
                        blockCounts.merge(name, 1, Integer::sum);
                    }
                }
            }
        }

        // 收集附近的实体
        List<EntityInfo> entities = new ArrayList<>();
        double entityRadius = radius * radius;
        for (Entity entity : world.getEntities()) {
            if (entity == mc.player) continue;

            double dx = entity.getX() - mc.player.getX();
            double dy = entity.getY() - mc.player.getY();
            double dz = entity.getZ() - mc.player.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < entityRadius) {
                entities.add(new EntityInfo(
                    entity.getType().toString(),
                    dx, dy, dz,
                    Math.sqrt(distSq)
                ));
            }
        }

        // 按距离排序
        entities.sort(Comparator.comparingDouble(e -> e.distance));

        // 如果方块列表太长，只返回最近的
        if (blocks.size() > 500) {
            blocks = blocks.subList(0, 500);
        }

        return new BlockScanResult(
            true,
            blocks,
            blockCounts,
            entities,
            radius
        );
    }

    // ==================== 数据类 ====================

    /**
     * 扫描结果
     */
    public static class BlockScanResult {
        public boolean connected;
        public List<BlockInfo> nearbyBlocks;       // 附近的方块 (相对坐标)
        public Map<String, Integer> blockSummary;  // 方块数量统计
        public List<EntityInfo> nearbyEntities;     // 附近的实体
        public int scanRadius;                       // 扫描半径

        public BlockScanResult(boolean connected, List<BlockInfo> nearbyBlocks,
                               Map<String, Integer> blockSummary,
                               List<EntityInfo> nearbyEntities, int scanRadius) {
            this.connected = connected;
            this.nearbyBlocks = nearbyBlocks;
            this.blockSummary = blockSummary;
            this.nearbyEntities = nearbyEntities;
            this.scanRadius = scanRadius;
        }
    }

    /**
     * 方块信息 (相对坐标)
     */
    public static class BlockInfo {
        public String name;   // 方块ID
        public int dx;        // 相对玩家 X 偏移
        public int dy;        // 相对玩家 Y 偏移
        public int dz;        // 相对玩家 Z 偏移

        public BlockInfo(String name, int dx, int dy, int dz) {
            this.name = name;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    /**
     * 实体信息 (相对坐标 + 距离)
     */
    public static class EntityInfo {
        public String type;     // 实体类型
        public double dx;        // 相对 X 偏移
        public double dy;        // 相对 Y 偏移
        public double dz;        // 相对 Z 偏移
        public double distance;  // 与玩家的距离

        public EntityInfo(String type, double dx, double dy, double dz, double distance) {
            this.type = type;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.distance = distance;
        }
    }
}
