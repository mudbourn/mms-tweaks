package info.mudbourn.mmstweaks.client.revive;

import com.mojang.blaze3d.platform.InputConstants;
import goetic.mods.absolutrevive.client.ClientEventHandler;
import info.mudbourn.mmstweaks.MmsTweaks;
import info.mudbourn.mmstweaks.client.ReviveOverlayFade;
import info.mudbourn.mmstweaks.net.DownedStatus;
import info.mudbourn.mmstweaks.net.SelfReviveRequest;
import info.mudbourn.mmstweaks.revive.Downed;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Client side of self reviving: the hold key, its progress bar under the downed HUD and the server's status.
 */
public final class SelfReviveHold {

    public static final KeyMapping KEY = new KeyMapping(
        "key.mms_tweaks.self_revive",
        InputConstants.KEY_R,
        KeyMapping.Category.register(MmsTweaks.id("mms_tweaks"))
    );

    private static boolean lootable;

    private static int downedTicks;

    private static int holdTicks;

    private static int lastHoldTicks;

    private static boolean sent;

    private SelfReviveHold() {}

    public static void register() {
        KeyMappingHelper.registerKeyMapping(KEY);
        ClientPlayNetworking.registerGlobalReceiver(DownedStatus.TYPE, (payload, context) -> {
            lootable = payload.lootable();
        });
        ClientTickEvents.END_CLIENT_TICK.register(SelfReviveHold::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static boolean lootable() {
        return lootable;
    }

    private static boolean downed(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.isAlive() && ClientEventHandler.isUnconscious(minecraft.player);
    }

    private static void tick(Minecraft minecraft) {
        lastHoldTicks = holdTicks;
        if (!downed(minecraft)) {
            downedTicks = 0;
            holdTicks = 0;
            lastHoldTicks = 0;
            sent = false;
            return;
        }
        downedTicks++;
        boolean held = minecraft.gui.screen() == null && KEY.isDown();
        if (!held) {
            sent = false;
            holdTicks = Math.max(0, holdTicks - 3);
            return;
        }
        if (sent) {
            return;
        }
        holdTicks = Math.min(Downed.SELF_REVIVE_TICKS, holdTicks + 1);
        if (holdTicks >= Downed.SELF_REVIVE_TICKS && downedTicks >= Downed.SELF_REVIVE_TICKS) {
            ClientPlayNetworking.send(new SelfReviveRequest());
            sent = true;
            holdTicks = 0;
            lastHoldTicks = 0;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!downed(minecraft) || minecraft.gui.hud.isHidden()) {
            return;
        }
        int centerX = graphics.guiWidth() / 2;
        int top = graphics.guiHeight() / 2 + 80;
        Component prompt = Component.translatable("mms_tweaks.downed.self_prompt", KEY.getTranslatedKeyMessage());
        graphics.centeredText(minecraft.font, prompt, centerX, top, ReviveOverlayFade.apply(0xFFFFE08A));

        float partial = delta.getGameTimeDeltaTicks();
        float progress = Mth.lerp(partial, lastHoldTicks, holdTicks) / Downed.SELF_REVIVE_TICKS;
        if (progress <= 0.0F) {
            return;
        }
        int barLeft = centerX - 72;
        int barTop = top + 14;
        int fill = Math.round(142.0F * Mth.clamp(progress, 0.0F, 1.0F));
        graphics.fill(barLeft, barTop, barLeft + 144, barTop + 8, ReviveOverlayFade.apply(0xB4201806));
        graphics.fill(barLeft + 1, barTop + 1, barLeft + 143, barTop + 7, ReviveOverlayFade.apply(0xB4403010));
        graphics.fill(barLeft + 1, barTop + 1, barLeft + 1 + fill, barTop + 7, ReviveOverlayFade.apply(0xDCE0B040));
        String seconds = String.format(Locale.ROOT, "%.1f", progress * Downed.SELF_REVIVE_TICKS / 20.0F);
        graphics.centeredText(
            minecraft.font,
            Component.translatable(
                "mms_tweaks.downed.self_progress",
                seconds,
                String.format(Locale.ROOT, "%.1f", Downed.SELF_REVIVE_TICKS / 20.0F)
            ),
            centerX,
            barTop + 12,
            ReviveOverlayFade.apply(0xFFFFE08A)
        );
    }

    private static void reset() {
        lootable = false;
        downedTicks = 0;
        holdTicks = 0;
        lastHoldTicks = 0;
        sent = false;
    }
}
