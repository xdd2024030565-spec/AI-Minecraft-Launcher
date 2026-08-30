package com.aimc.controller

import java.util.concurrent.CompletableFuture

/**
 * AI 决策引擎
 *
 * 将游戏状态转换为 LLM Prompt，获取动作列表，并通过 GameApiClient 执行。
 * 支持多种任务模式（探索、收集、建造等）。
 */
class DecisionEngine(
    private val gameApi: GameApiClient,
    private val llmClient: LlmClient
) {
    companion object {
        private const val MAX_ACTIONS_PER_CYCLE = 5
        private const val MAX_PROMPT_LENGTH = 4000
    }

    /** 当前任务目标 */
    var currentTask: String = "Explore the world and survive"
        set(value) {
            field = value
            taskHistory.add(0, System.currentTimeMillis() to value)
            if (taskHistory.size > 50) taskHistory.removeLast()
        }

    /** 任务历史记录 */
    val taskHistory = mutableListOf<Pair<Long, String>>()

    /** 决策循环统计 */
    var cycleCount = 0
        private set
    var lastError: String? = null
        private set

    /**
     * 执行一次决策循环:
     * 1. 收集游戏状态
     * 2. 构建 Prompt
     * 3. 调用 LLM 获取动作
     * 4. 执行动作
     *
     * @return 执行结果摘要
     */
    suspend fun runDecisionCycle(): DecisionResult {
        cycleCount++
        lastError = null

        return try {
            // 1. 收集游戏状态
            val gameState = gameApi.getGameState()
            if (!gameState.connected) {
                return DecisionResult.Skip("Player not connected to world")
            }

            val inventory = runCatching { gameApi.getInventory() }.getOrNull()
            val blocks = runCatching { gameApi.getNearbyBlocks(6) }.getOrNull()

            // 2. 构建 Prompt
            val prompt = buildPrompt(gameState, inventory, blocks)

            // 3. 调用 LLM 获取动作
            val systemPrompt = buildSystemPrompt()
            val actions = llmClient.getActions(systemPrompt, prompt)

            if (actions.isEmpty()) {
                return DecisionResult.Skip("LLM returned no actions")
            }

            // 4. 执行动作 (限制每周期最多执行 MAX_ACTIONS_PER_CYCLE)
            val executed =(minOf(actions.size, MAX_ACTIONS_PER_CYCLE)).let { limit ->
                actions.take(limit).mapNotNull { action ->
                    runCatching {
                        val actionName = action["action"] as? String ?: return@mapNotNull null
                        gameApi.executeAction(actionName, action)
                        actionName
                    }.getOrNull()
                }
            }

            DecisionResult.Success(
                cycleNumber = cycleCount,
                actionsExecuted = executed,
                promptLength = prompt.length
            )

        } catch (e: Exception) {
            lastError = e.message
            DecisionResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 构建系统提示词
     */
    private fun buildSystemPrompt(): String {
        return """
            You are an AI playing Minecraft. Your goal is to complete the current task.
            
            Available actions:
            - move: {direction: forward/back/left/right, duration: ticks}
            - look: {yaw: -180~180, pitch: -90~90}
            - jump: {}
            - mine: {duration: ticks}
            - place: {}
            - attack: {}
            - use: {}
            - inventory_click: {slot: 0-35, button: left/right}
            - drop_item: {slot: 0-35}
            - toggle_sprint: {}
            - chat: {message: string}
            
            Rules:
            - Return ONLY a JSON array of actions, no explanation.
            - Example: [{"action":"move","direction":"forward","duration":10}]
            - Be efficient and goal-oriented.
            - If the task requires crafting, first mine necessary resources.
            - If health < 5, consider searching for food or retreating.
        """.trimIndent()
    }

    /**
     * 构建用户提示词 (包含当前游戏状态)
     */
    private fun buildPrompt(
        gameState: GameApiClient.GameStateResponse,
        inventory: GameApiClient.InventoryResponse?,
        blocks: GameApiClient.BlocksResponse?
    ): String {
        val sb = StringBuilder()

        sb.appendLine("Current task: $currentTask")
        sb.appendLine("=== PLAYER STATE ===")
        sb.appendLine("Position: [${gameState.position?.joinToString { "%.1f".format(it) } ?: "??"}]")
        sb.appendLine("Health: ${gameState.health}/20")
        sb.appendLine("Food: ${gameState.food}/20")
        sb.appendLine("Level: ${gameState.xpLevel} (${(gameState.xpProgress * 100).toInt()}%)")
        sb.appendLine("Rotation: yaw=${gameState.rotation?.getOrNull(0)?.let { "%.1f".format(it) } ?: "??"}, pitch=${gameState.rotation?.getOrNull(1)?.let { "%.1f".format(it) } ?: "??"}")
        sb.appendLine("Dimension: ${gameState.dimension ?: "unknown"}")
        sb.appendLine("Time: ${if (gameState.isDay) "DAY" else "NIGHT"} (tick ${gameState.worldTime})")
        sb.appendLine("Sprinting: ${gameState.sprinting}, Sneaking: ${gameState.sneaking}, OnGround: ${gameState.onGround}")

        // 背包摘要
        if (inventory != null && inventory.mainInventory != null) {
            sb.appendLine("=== INVENTORY ===")
            val items = inventory.mainInventory.filter { it.name != "air" && it.count > 0 }
            if (items.isNotEmpty()) {
                items.take(10).forEach { item ->
                    sb.appendLine("  [${item.slot}] ${item.count}x ${item.name}")
                }
                if (items.size > 10) {
                    sb.appendLine("  ... and ${items.size - 10} more items")
                }
            } else {
                sb.appendLine("  (empty)")
            }
        }

        // 附近方块
        if (blocks != null && blocks.blockSummary != null) {
            sb.appendLine("=== NEARBY BLOCKS (radius ${blocks.scanRadius}) ===")
            blocks.blockSummary.entries.take(10).forEach { (name, count) ->
                sb.appendLine("  $count x $name")
            }
        }

        // 附近实体
        if (blocks != null && blocks.nearbyEntities != null && blocks.nearbyEntities.isNotEmpty()) {
            sb.appendLine("=== NEARBY ENTITIES ===")
            blocks.nearbyEntities.take(5).forEach { entity ->
                sb.appendLine("  ${entity.type} at distance ${"%.1f".format(entity.distance)}")
            }
        }

        // 截断过长的 prompt
        val prompt = sb.toString()
        return if (prompt.length > MAX_PROMPT_LENGTH) {
            prompt.substring(0, MAX_PROMPT_LENGTH) + "\n... [truncated]"
        } else {
            prompt
        }
    }
}

/**
 * 决策结果
 */
sealed class DecisionResult {
    data class Success(
        val cycleNumber: Int,
        val actionsExecuted: List<String>,
        val promptLength: Int
    ) : DecisionResult()

    data class Skip(val reason: String) : DecisionResult()
    data class Error(val message: String) : DecisionResult()
}