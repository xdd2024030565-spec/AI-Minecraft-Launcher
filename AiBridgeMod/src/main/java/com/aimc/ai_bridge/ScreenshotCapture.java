package com.aimc.ai_bridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 截图捕获器
 *
 * 捕获当前 Minecraft 游戏画面，返回 PNG 格式的字节数组。
 * 用于多模态 LLM (如 GPT-4o / Claude Vision) 进行视觉决策。
 */
public class ScreenshotCapture {

    /**
     * 捕获当前游戏画面
     *
     * @return PNG 格式的字节数组，如果游戏未渲染则返回 null
     */
    public static byte[] capture() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.getFramebuffer() == null) {
                return null;
            }

            // 使用 Minecraft 自带的截图工具
            NativeImage nativeImage = takeScreenshot(mc);

            if (nativeImage == null) {
                return null;
            }

            // 转换为 BufferedImage 再写成 PNG
            byte[] png = nativeImageToPng(nativeImage);
            nativeImage.close();
            return png;

        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] 截图失败", e);
            return null;
        }
    }

    /**
     * 使用 Minecraft 内部 API 截图
     */
    private static NativeImage takeScreenshot(MinecraftClient mc) {
        try {
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer == null) return null;

            // 使用 ScreenshotRecorder 捕获帧缓冲
            return ScreenshotRecorder.takeScreenshot(
                framebuffer.textureWidth,
                framebuffer.textureHeight,
                framebuffer
            );
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] takeScreenshot 失败", e);
            return null;
        }
    }

    /**
     * 将 NativeImage 转换为 PNG 字节数组
     */
    private static byte[] nativeImageToPng(NativeImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            BufferedImage bufferedImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB
            );

            // 逐像素复制 (NativeImage 格式为 ABGR)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = image.getColor(x, y);
                    // 转换 ABGR -> ARGB
                    int a = (color >> 24) & 0xFF;
                    int b = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int r = color & 0xFF;
                    bufferedImage.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", out);
            return out.toByteArray();

        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] nativeImageToPng 失败", e);
            return null;
        }
    }
}
