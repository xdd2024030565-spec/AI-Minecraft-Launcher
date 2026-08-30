package com.aimc.controller

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 游戏 API 客户端
 *
 * 封装对 AI Bridge Mod HTTP API 的所有请求。
 * 调用方 (Android Service) 使用此客户端查询游戏状态、发送动作。
 */
class GameApiClient(
    private val port: Int = 25580
) {
    private val retrofit: Retrofit
    private val api: GameApi

    private val gson = Gson()

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:$port/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GameApi::class.java)
    }

    // ==================== API 接口 ====================

    private interface GameApi {
        @GET("api/state")
        suspend fun getState(): GameStateResponse

        @GET("api/inventory")
        suspend fun getInventory(): InventoryResponse

        @GET("api/blocks")
        suspend fun getBlocks(@Query("radius") radius: Int): BlocksResponse

        @POST("api/action")
        suspend fun postAction(@Body action: ActionRequest): ActionResult

        @POST("api/chat")
        suspend fun sendChat(@Body request: ChatRequest): ActionResult

        @GET("api/screenshot")
        suspend fun getScreenshot(): Response<okhttp3.RequestBody>
    }

    // ==================== 响应数据类 ====================

    data class GameStateResponse(
        val connected: Boolean,
        val position: List<Double>? = null,
        val health: Float = 0f,
        val food: Int = 0,
        val saturation: Float = 0f,
        val xpLevel: Int = 0,
        val xpProgress: Float = 0f,
        val rotation: List<Double>? = null,
        val dimension: String? = null,
        val worldTime: Long = 0L,
        val isDay: Boolean = true,
        val sprinting: Boolean = false,
        val sneaking: Boolean = false,
        val onGround: Boolean = true,
        val inWater: Boolean = false,
        val hasSkyLight: Boolean = true,
        val serverPort: Int = 0
    )

    data class InventoryResponse(
        val connected: Boolean,
        val mainInventory: List<ItemInfo>? = null,
        val armor: List<ItemInfo>? = null,
        val selectedSlot: Int = 0,
        val offhand: ItemInfo? = null
    )

    data class ItemInfo(
        val name: String,
        val count: Int,
        val slot: Int
    )

    data class BlocksResponse(
        val connected: Boolean,
        val nearbyBlocks: List<BlockInfo>? = null,
        val blockSummary: Map<String, Int>? = null,
        val nearbyEntities: List<EntityInfo>? = null,
        val scanRadius: Int = 5
    )

    data class BlockInfo(
        val name: String,
        val dx: Int,
        val dy: Int,
        val dz: Int
    )

    data class EntityInfo(
        val type: String,
        val dx: Double,
        val dy: Double,
        val dz: Double,
        val distance: Double
    )

    data class ActionResult(
        val success: Boolean,
        val message: String? = null
    )

    data class ChatRequest(val message: String)
    data class ActionRequest(val action: String, val params: Map<String, Any> = emptyMap())

    // ==================== 公开方法 ====================

    /** 获取游戏状态 */
    suspend fun getGameState(): GameStateResponse = api.getState()

    /** 获取背包信息 */
    suspend fun getInventory(): InventoryResponse = api.getInventory()

    /** 扫描附近方块和实体 */
    suspend fun getNearbyBlocks(radius: Int = 6): BlocksResponse = api.getBlocks(radius)

    /** 执行动作 */
    suspend fun executeAction(action: String, params: Map<String, Any>): ActionResult =
        api.postAction(ActionRequest(action, params))

    /** 发送聊天消息 */
    suspend fun sendChat(message: String): ActionResult =
        api.sendChat(ChatRequest(message))

    /** 获取游戏截图 (PNG 字节数组) */
    suspend fun getScreenshot(): ByteArray {
        val response = api.getScreenshot()
        if (!response.isSuccessful) {
            throw Exception("Screenshot request failed: ${response.code()}")
        }
        return response.body()?.bytes() ?: throw Exception("Empty screenshot response")
    }

    /** 健康检查 */
    suspend fun healthCheck(): Map<String, Any> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/api/health")
            .build()
        val resp = client.newCall(request).execute()
        if (!resp.isSuccessful) {
            throw Exception("Health check failed: ${resp.code()}")
        }
        val json = resp.body?.string() ?: "{}"
        return gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
    }
}