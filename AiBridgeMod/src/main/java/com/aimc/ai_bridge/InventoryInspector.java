package com.aimc.ai_bridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 背包检查器
 *
 * 查询玩家背包中的物品信息，包括主物品栏 (36格)、装备栏 (4格)、
 * 当前选中的快捷栏位以及副手。
 */
public class InventoryInspector {

    /**
     * 查询当前背包内容
     */
    public static InventoryData inspect() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return new InventoryData(false, null, null, -1, null);
        }

        PlayerInventory inv = mc.player.getInventory();

        // 主物品栏 (0-35: 9个快捷栏 + 27个背包栏)
        List<ItemInfo> main = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            main.add(new ItemInfo(
                stack.isEmpty() ? "air" : stack.getItem().toString(),
                stack.getCount(),
                i
            ));
        }

        // 装备栏 (36-39: 靴子、护腿、胸甲、头盔)
        List<ItemInfo> armor = new ArrayList<>();
        for (int i = 36; i < 40; i++) {
            ItemStack stack = inv.getStack(i);
            armor.add(new ItemInfo(
                stack.isEmpty() ? "air" : stack.getItem().toString(),
                stack.getCount(),
                i - 36
            ));
        }

        // 副手
        ItemStack offhand = inv.offHand.get(0);
        ItemInfo offhandInfo = new ItemInfo(
            offhand.isEmpty() ? "air" : offhand.getItem().toString(),
            offhand.getCount(),
            0
        );

        return new InventoryData(
            true,
            main,
            armor,
            inv.selectedSlot,
            offhandInfo
        );
    }

    // ==================== 数据类 ====================

    /**
     * 背包数据
     */
    public static class InventoryData {
        public boolean connected;
        public List<ItemInfo> mainInventory;   // 36格主物品栏
        public List<ItemInfo> armor;            // 4格装备栏
        public int selectedSlot;                // 当前选中的快捷栏位 (0-8)
        public ItemInfo offhand;                // 副手物品

        public InventoryData(boolean connected, List<ItemInfo> mainInventory,
                             List<ItemInfo> armor, int selectedSlot,
                             ItemInfo offhand) {
            this.connected = connected;
            this.mainInventory = mainInventory;
            this.armor = armor;
            this.selectedSlot = selectedSlot;
            this.offhand = offhand;
        }
    }

    /**
     * 单个物品信息
     */
    public static class ItemInfo {
        public String name;     // 物品ID (如 "minecraft:stone")
        public int count;       // 数量
        public int slot;        // 槽位编号

        public ItemInfo(String name, int count, int slot) {
            this.name = name;
            this.count = count;
            this.slot = slot;
        }
    }
}
