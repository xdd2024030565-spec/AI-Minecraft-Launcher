package com.aimc.ai_bridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 动作执行器
 *
 * 接收 AI 发来的 JSON 动作请求，在 Minecraft 主线程上执行对应的操作。
 * 所有 Minecraft API 调用必须通过 mc.execute() 在主线程执行。
 *
 * 支持的动作类型:
 *   move, look, jump, mine, place, attack, use,
 *   inventory_click, craft, drop_item, toggle_sprint, chat
 */
public class ActionExecutor {

    /**
     * 执行动作
     *
     * @param action 动作名称
     * @param params 动作参数 (来自 JSON 请求体)
     * @return 执行结果
     */
    public static ActionResult execute(String action, Map<String, Object> params) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return new ActionResult(false, "Player not connected to world");
        }

        try {
            switch (action) {
                case "move":       return move(params);
                case "look":       return look(params);
                case "jump":       return jump();
                case "mine":       return mine(params);
                case "place":      return place();
                case "attack":     return attack();
                case "use":        return use();
                case "inventory_click": return inventoryClick(params);
                case "craft":      return craft(params);
                case "drop_item":  return dropItem(params);
                case "toggle_sprint": return toggleSprint();
                default:
                    return new ActionResult(false, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }

    // ==================== 移动相关 ====================

    /**
     * 移动 (WASD)
     * 参数: direction (forward/back/left/right), duration (ticks, 默认10)
     */
    private static ActionResult move(Map<String, Object> params) {
        String direction = (String) params.get("direction");
        if (direction == null) {
            return new ActionResult(false, "'direction' is required (forward/back/left/right)");
        }

        int duration = getIntParam(params, "duration", 10);
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.execute(() -> {
            switch (direction) {
                case "forward": mc.options.forwardKey.setPressed(true); break;
                case "back":    mc.options.backKey.setPressed(true); break;
                case "left":    mc.options.leftKey.setPressed(true); break;
                case "right":   mc.options.rightKey.setPressed(true); break;
            }
        });

        // 延迟后释放按键
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(duration * 50L); } catch (InterruptedException ignored) {}
            mc.execute(() -> {
                switch (direction) {
                    case "forward": mc.options.forwardKey.setPressed(false); break;
                    case "back":    mc.options.backKey.setPressed(false); break;
                    case "left":    mc.options.leftKey.setPressed(false); break;
                    case "right":   mc.options.rightKey.setPressed(false); break;
                }
            });
        });

        return new ActionResult(true, "Moving " + direction + " for " + duration + " ticks");
    }

    /**
     * 转视角
     * 参数: yaw (-180~180), pitch (-90~90)
     */
    private static ActionResult look(Map<String, Object> params) {
        double yaw = getDoubleParam(params, "yaw", 0);
        double pitch = getDoubleParam(params, "pitch", 0);

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            mc.player.setYaw((float) yaw);
            mc.player.setPitch((float) pitch);
        });

        return new ActionResult(true, "Looking at yaw=" + yaw + ", pitch=" + pitch);
    }

    /**
     * 跳跃
     */
    private static ActionResult jump() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.jumpKey.setPressed(true);
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.jumpKey.setPressed(false));
        });
        return new ActionResult(true, "Jumped");
    }

    // ==================== 交互相关 ====================

    /**
     * 挖方块 (持续按住攻击键)
     * 参数: duration (ticks, 默认20)
     */
    private static ActionResult mine(Map<String, Object> params) {
        int duration = getIntParam(params, "duration", 20);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.attackKey.setPressed(true);

        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(duration * 50L); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.attackKey.setPressed(false));
        });

        return new ActionResult(true, "Mining for " + duration + " ticks");
    }

    /**
     * 放置方块 (短按使用键)
     */
    private static ActionResult place() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.useKey.setPressed(true);
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.useKey.setPressed(false));
        });
        return new ActionResult(true, "Placed block");
    }

    /**
     * 攻击实体 (短按攻击键)
     */
    private static ActionResult attack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.attackKey.setPressed(true);
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.attackKey.setPressed(false));
        });
        return new ActionResult(true, "Attacked");
    }

    /**
     * 使用物品 (短按使用键)
     */
    private static ActionResult use() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.useKey.setPressed(true);
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.useKey.setPressed(false));
        });
        return new ActionResult(true, "Used item");
    }

    // ==================== 背包相关 ====================

    /**
     * 点击背包槽位
     * 参数: slot (int), button (left/right, 默认left)
     */
    private static ActionResult inventoryClick(Map<String, Object> params) {
        int slot = getIntParam(params, "slot", -1);
        if (slot < 0) {
            return new ActionResult(false, "'slot' is required");
        }

        String button = (String) params.getOrDefault("button", "left");
        int buttonId = button.equals("right") ? 1 : 0;

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player.currentScreenHandler != null) {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    slot,
                    buttonId,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
        });

        return new ActionResult(true, "Clicked slot " + slot + " (" + button + ")");
    }

    /**
     * 合成物品 (后续实现)
     * 参数: item (String)
     */
    private static ActionResult craft(Map<String, Object> params) {
        String item = (String) params.get("item");
        // TODO: 使用 RecipeManager 查找并合成
        return new ActionResult(false, "Crafting not yet implemented (requested: " + item + ")");
    }

    /**
     * 丢弃物品
     * 参数: slot (int, 可选, 默认当前手持栏位)
     */
    private static ActionResult dropItem(Map<String, Object> params) {
        int slot = getIntParam(params, "slot", -1);
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (slot >= 0 && slot < 9) {
                mc.player.getInventory().selectedSlot = slot;
            }
            mc.player.dropSelectedItem(false);
        });
        return new ActionResult(true, "Dropped item from slot " + (slot >= 0 ? slot : "current"));
    }

    /**
     * 切换冲刺
     */
    private static ActionResult toggleSprint() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.sprintKey.setPressed(true);
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            mc.execute(() -> mc.options.sprintKey.setPressed(false));
        });
        return new ActionResult(true, "Toggled sprint");
    }

    // ==================== 聊天 ====================

    /**
     * 发送聊天消息
     */
    public static void sendChat(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }

    // ==================== 辅助方法 ====================

    private static int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private static double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    // ==================== 结果数据类 ====================

    public static class ActionResult {
        public boolean success;
        public String message;

        public ActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
