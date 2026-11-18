package info.mudbourn.mmstweaks.client.slain;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * After the respawn: fades the text and tint out over black, then eases the black away.
 */
final class SlainAftermath {

    private static final int TEXT_TICKS = 12;

    private static final int BLACK_TICKS = 24;

    private final SlainCard card;

    private final SlainAudio audio;

    private final float startBlack;

    private final int pulseBase;

    private int ticks;

    private int textTicks = TEXT_TICKS;

    private int blackTicks = BLACK_TICKS;

    private boolean woke;

    SlainAftermath(SlainCard card, SlainAudio audio, float startBlack, int pulseBase) {
        this.card = card;
        this.audio = audio;
        this.startBlack = startBlack;
        this.pulseBase = pulseBase;
    }

    // Advances one tick and returns false once the aftermath is over.
    boolean tick(Minecraft minecraft) {
        audio.tick(minecraft);
        if (InputConstants.isKeyDown(InputConstants.KEY_SPACE)) {
            audio.fadeOut(2);
            textTicks = Math.min(textTicks, ticks);
            blackTicks = 0;
        }
        ticks++;
        if (!woke && ticks >= textTicks) {
            woke = true;
            SlainFlow.releaseClone();
        }
        if (woke && ticks >= textTicks + blackTicks) {
            audio.release(minecraft);
            return false;
        }
        return true;
    }

    void release(Minecraft minecraft) {
        audio.release(minecraft);
    }

    void render(GuiGraphicsExtractor graphics, float partial) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float time = ticks + partial;

        float textPhase = textTicks <= 0 ? 1.0F : Mth.clamp(time / textTicks, 0.0F, 1.0F);
        float shade;
        if (!woke) {
            shade = DeathPan.active() ? startBlack * (1.0F - textPhase) : Mth.lerp(textPhase, startBlack, 1.0F);
        } else if (DeathPan.active()) {
            shade = 0.0F;
        } else {
            float out = blackTicks <= 0 ? 1.0F : Mth.clamp((time - textTicks) / blackTicks, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - out) * (1.0F - out) * (1.0F - out);
            shade = 1.0F - eased;
        }
        SlainFlow.drawBlack(graphics, width, height, shade);

        float fade = 1.0F - textPhase;
        DeathTint.render(graphics, width, height, pulseBase + time, fade);
        card.draw(graphics, minecraft.font, width, height, fade, null, null, false, 0.0F);
    }
}
