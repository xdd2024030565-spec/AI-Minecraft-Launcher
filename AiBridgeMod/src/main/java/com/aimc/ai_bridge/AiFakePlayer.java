package com.aimc.ai_bridge;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * AI 假人玩家实体 —— 服务端
 *
 * 基于 ServerPlayerEntity 的最小实现（Carpet /player 风格），
 * 由 AI 控制器通过 FakePlayerManager 驱动。
 *
 * 仅用于本地存档（集成服务器）；远程服务器请使用「远程假人（小号实例）」模式。
 */
public class AiFakePlayer extends ServerPlayerEntity {

    /** 是否为 AI 控制（预留：可切为手动模式） */
    private boolean aiControlled = true;

    public AiFakePlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
    }

    public boolean isAiControlled() {
        return aiControlled;
    }

    public void setAiControlled(boolean aiControlled) {
        this.aiControlled = aiControlled;
    }
}
