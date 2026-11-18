package info.mudbourn.mmstweaks.client.slain;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;

/**
 * The text of one death: headline, cause, coordinates, and how it is laid out on screen.
 */
final class SlainCard {

    private static final int HEADLINE = 0xFF2B2B;

    private static final int CAUSE = 0xE6E6E6;

    private static final int COORDS = 0xFFD966;

    private static final int COORDS_HOVER = 0xFFFFFF;

    private static final int NOTICE = 0x7CFF7C;

    private static final int TIMER = 0xF0F0F0;

    private static final int HINT = 0xA8A8A8;

    final Component headline;

    final Component cause;

    final boolean hardcore;

    private final BlockPos pos;

    private final String dimension;

    SlainCard(Component headline, Component cause, boolean hardcore, BlockPos pos, Identifier dimension) {
        this.headline = headline;
        this.cause = cause;
        this.hardcore = hardcore;
        this.pos = pos;
        this.dimension = prettify(dimension);
    }

    Component coordsLine() {
        return Component.translatable("mms_tweaks.slain.coords", pos.getX(), pos.getY(), pos.getZ(), dimension);
    }

    Component chatLine() {
        String copy = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return Component.translatable("mms_tweaks.slain.coords_chat", pos.getX(), pos.getY(), pos.getZ(), dimension)
            .withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent.CopyToClipboard(copy))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("mms_tweaks.slain.coords_copy"))));
    }

    boolean overCoords(Font font, int width, int height, double mouseX, double mouseY) {
        int half = font.width(coordsLine()) / 2 + 2;
        int y = coordsY(height);
        return mouseX >= width / 2.0 - half && mouseX <= width / 2.0 + half && mouseY >= y - 2 && mouseY <= y + font.lineHeight + 1;
    }

    void draw(
        GuiGraphicsExtractor graphics,
        Font font,
        int width,
        int height,
        float alpha,
        Component timer,
        Component hint,
        boolean coordsHover,
        float notice
    ) {
        if (alpha <= 0.02F) {
            return;
        }
        int centerX = width / 2;
        int top = height / 4;

        graphics.pose().pushMatrix();
        graphics.pose().scale(2.0F, 2.0F);
        graphics.centeredText(font, headline, centerX / 2, (top - 10) / 2, argb(alpha, HEADLINE));
        graphics.pose().popMatrix();

        if (cause != null) {
            graphics.centeredText(font, cause, centerX, top + 24, argb(alpha, CAUSE));
        }
        Component coords = coordsLine().copy().withStyle(ChatFormatting.UNDERLINE);
        graphics.centeredText(font, coords, centerX, coordsY(height), argb(alpha, coordsHover ? COORDS_HOVER : COORDS));
        if (notice > 0.02F) {
            graphics.centeredText(font, Component.translatable("mms_tweaks.slain.coords_saved"), centerX, coordsY(height) + 14, argb(alpha * notice, NOTICE));
        }
        if (timer != null) {
            graphics.centeredText(font, timer, centerX, height - 52, argb(alpha, TIMER));
        }
        if (hint != null) {
            graphics.centeredText(font, hint, centerX, height - 40, argb(alpha, HINT));
        }
    }

    private static int coordsY(int height) {
        return height / 4 + 40;
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(4, Math.round(Math.min(1.0F, alpha) * 255.0F));
        return a << 24 | rgb;
    }

    private static String prettify(Identifier dimension) {
        String[] words = dimension.getPath().split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
