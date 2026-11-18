package info.mudbourn.mmstweaks.client.slain;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * AbsolutRevive's downed overlay at full death danger, so the game over screen continues it without a swap.
 */
final class DeathTint {

    private DeathTint() {}

    static void render(GuiGraphicsExtractor graphics, int width, int height, float pulseTime, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        graphics.fill(0, 0, width, height, color(alpha, 178, 0, 0, 0));
        vignette(graphics, width, height, 0, 0, 0, 0.8F, 24, alpha);

        float pulse = 0.72F + 0.44F * Mth.sin(pulseTime * 0.1F);
        float intensity = Mth.clamp(pulse, 0.0F, 1.0F);
        graphics.fill(0, 0, width, height, color(alpha, 148, 90, 0, 0));
        vignette(graphics, width, height, 160, 10, 10, 0.18F + intensity * 0.82F, 28, alpha);
    }

    private static void vignette(
        GuiGraphicsExtractor graphics,
        int width,
        int height,
        int red,
        int green,
        int blue,
        float intensity,
        int baseThickness,
        float alpha
    ) {
        int layers = 7;
        for (int layer = 0; layer < layers; layer++) {
            float progress = (float) (layer + 1) / layers;
            float falloff = 1.0F - progress;
            int thickness = Math.max(4, Math.round(baseThickness * (0.35F + progress * (1.15F + intensity * 0.95F))));
            int edgeAlpha = Math.round((8.0F + 76.0F * intensity) * falloff * falloff);
            if (edgeAlpha > 0) {
                edge(graphics, width, height, color(alpha, edgeAlpha, red, green, blue), thickness);
            }
        }
        graphics.fill(0, 0, width, height, color(alpha, Math.round(6.0F + 18.0F * intensity), red, green, blue));
    }

    private static void edge(GuiGraphicsExtractor graphics, int width, int height, int color, int thickness) {
        graphics.fill(0, 0, width, thickness, color);
        graphics.fill(0, height - thickness, width, height, color);
        graphics.fill(0, thickness, thickness, height - thickness, color);
        graphics.fill(width - thickness, thickness, width, height - thickness, color);
    }

    private static int color(float scale, int alpha, int red, int green, int blue) {
        int a = Math.round(alpha * scale) & 0xFF;
        return a << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }
}
