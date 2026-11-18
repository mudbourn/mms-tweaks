package info.mudbourn.mmstweaks.client.slain;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmstweaks.MmsTweaks;
import info.mudbourn.mmstweaks.client.CinematicBridge;
import info.mudbourn.mmstweaks.client.revive.DownedCamera;
import info.mudbourn.mmstweaks.client.xaero.XaeroDeathpoints;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shared state of the death flow across the game over screen, the aftermath and Cinematic Respawn.
 */
public final class SlainFlow {

    private static final boolean XAERO = FabricLoader.getInstance().isModLoaded("xaerominimap");

    private static final boolean ABSOLUT_REVIVE = FabricLoader.getInstance().isModLoaded("absolutrevive");

    private static boolean musicHeld;

    private static boolean armed;

    private static boolean cloneHeld;

    private static boolean skipArmed;

    private static Boolean savedHideGui;

    private static SlainAftermath aftermath;

    private SlainFlow() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SlainFlow::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
        HudElementRegistry.addLast(MmsTweaks.id("slain_aftermath"), SlainFlow::render);
    }

    public static boolean musicHeld() {
        return musicHeld;
    }

    // True while the local player lies dead and no respawn has been handed to Cinematic Respawn.
    public static boolean holdsCinematicTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isDeadOrDying() && !armed;
    }

    public static boolean holdsClone() {
        return cloneHeld;
    }

    static void holdMusic(boolean held) {
        musicHeld = held;
    }

    static void onSlain(Minecraft minecraft) {
        armed = false;
        if (CinematicBridge.present()) {
            DeathPan.rise(minecraft);
        }
        if (XAERO) {
            XaeroDeathpoints.begin();
        }
        if (savedHideGui == null) {
            savedHideGui = minecraft.gui.hud.isHidden();
        }
        setHudHidden(minecraft, true);
    }

    static void onRespawnRequested() {
        armed = true;
        cloneHeld = true;
        skipArmed = true;
        CinematicBridge.armRespawn();
    }

    static void releaseClone() {
        if (!cloneHeld) {
            return;
        }
        cloneHeld = false;
        CinematicBridge.wakeUp();
        DeathPan.descend();
    }

    static void onCoordsCopied() {
        if (XAERO) {
            XaeroDeathpoints.keep();
        }
    }

    static void discardDeathpoints() {
        if (XAERO) {
            XaeroDeathpoints.discard();
        }
    }

    static void begin(SlainAftermath next) {
        restoreHideGui(Minecraft.getInstance());
        aftermath = next;
    }

    static void drawBlack(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
        if (alpha > 0.0F) {
            graphics.fill(0, 0, width, height, Math.round(Math.min(1.0F, alpha) * 255.0F) << 24);
        }
    }

    private static void tick(Minecraft minecraft) {
        if (aftermath != null && !aftermath.tick(minecraft)) {
            aftermath = null;
            discardDeathpoints();
        }
        if (!skipArmed) {
            return;
        }
        LocalPlayer current = minecraft.player;
        if (InputConstants.isKeyDown(InputConstants.KEY_SPACE) && current != null && current.isAlive()) {
            if (!cloneHeld) {
                CinematicBridge.skip();
            }
            if (ABSOLUT_REVIVE) {
                DownedCamera.cut();
            }
        }
        LocalPlayer player = minecraft.player;
        if (aftermath == null && !CinematicBridge.isActive() && player != null && player.isAlive()) {
            skipArmed = false;
            armed = false;
            DeathPan.stop();
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (aftermath != null) {
            aftermath.render(graphics, delta.getGameTimeDeltaPartialTick(false));
        }
    }

    private static void restoreHideGui(Minecraft minecraft) {
        if (savedHideGui != null) {
            setHudHidden(minecraft, savedHideGui);
            savedHideGui = null;
        }
    }

    private static void setHudHidden(Minecraft minecraft, boolean hidden) {
        if (minecraft.gui.hud.isHidden() != hidden) {
            minecraft.gui.hud.toggle();
        }
    }

    private static void reset(Minecraft minecraft) {
        SlainScreen.clear();
        if (aftermath != null) {
            aftermath.release(minecraft);
            aftermath = null;
        }
        restoreHideGui(minecraft);
        discardDeathpoints();
        DeathPan.stop();
        musicHeld = false;
        armed = false;
        cloneHeld = false;
        skipArmed = false;
    }
}
