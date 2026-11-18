package info.mudbourn.mmstweaks.client.slain;

import net.minecraft.client.Minecraft;
import info.mudbourn.mmstweaks.client.revive.View;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * The camera height over a death: an eased rise past render distance, then an eased descent after the respawn.
 */
public final class DeathPan {

    private static final float RISE_SECONDS = 9.0F;

    private static final float DESCENT_SECONDS = 4.0F;

    private static final double MARGIN = 48.0;

    private static final float LOOK_DOWN = 82.0F;

    private static boolean active;

    private static double height;

    private static long riseStart;

    private static long descentStart = -1L;

    private static float heldYaw;

    private static float shownYaw;

    private static float shownPitch;

    private static long lastYawFrame = -1L;

    private static long lastPitchFrame = -1L;

    private static Vec3 lastDeadSpot;

    private static Vec3 carried = Vec3.ZERO;

    private DeathPan() {}

    public static boolean active() {
        return active;
    }

    static void rise(Minecraft minecraft) {
        active = true;
        height = minecraft.options.getEffectiveRenderDistance() * 16.0 + MARGIN;
        riseStart = Util.getMillis();
        descentStart = -1L;
        lastYawFrame = -1L;
        lastPitchFrame = -1L;
    }

    static void descend() {
        if (active && descentStart < 0L) {
            descentStart = Util.getMillis();
        }
    }

    static boolean landed() {
        return descentStart >= 0L && (Util.getMillis() - descentStart) / 1000.0F >= DESCENT_SECONDS;
    }

    private static float descentEase() {
        float t = Mth.clamp((Util.getMillis() - descentStart) / 1000.0F / DESCENT_SECONDS, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    static void stop() {
        active = false;
        lastDeadSpot = null;
        carried = Vec3.ZERO;
        descentStart = -1L;
        lastYawFrame = -1L;
        lastPitchFrame = -1L;
    }

    // Holds the camera yaw from the moment of respawn and eases it into the player's own through the descent.
    public static float yaw(float currentYaw) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isDeadOrDying()) {
            heldYaw = currentYaw;
            return currentYaw;
        }
        float target = descentStart < 0L ? heldYaw : heldYaw + Mth.wrapDegrees(currentYaw - heldYaw) * descentEase();
        if (lastYawFrame < 0L) {
            shownYaw = heldYaw;
        }
        shownYaw += Mth.wrapDegrees(target - shownYaw) * View.follow(secondsSince(lastYawFrame));
        lastYawFrame = Util.getMillis();
        return shownYaw;
    }

    // Looks straight down until the descent, then eases into the player's own pitch; empty while they are dead.
    public static Float pitch(float currentPitch) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isDeadOrDying()) {
            return null;
        }
        float eased = descentStart < 0L ? 0.0F : descentEase();
        float target = LOOK_DOWN + (currentPitch - LOOK_DOWN) * eased;
        if (lastPitchFrame < 0L) {
            shownPitch = LOOK_DOWN;
        }
        shownPitch += (target - shownPitch) * View.follow(secondsSince(lastPitchFrame));
        lastPitchFrame = Util.getMillis();
        return shownPitch;
    }

    // The offset that keeps the camera where it was before the respawn teleport, fading out as the camera lands.
    public static Vec3 carry(Vec3 spot) {
        if (!active) {
            return Vec3.ZERO;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isDeadOrDying()) {
            lastDeadSpot = spot;
            carried = Vec3.ZERO;
            return Vec3.ZERO;
        }
        if (lastDeadSpot != null) {
            carried = lastDeadSpot.subtract(spot);
            lastDeadSpot = null;
        }
        if (carried == Vec3.ZERO) {
            return Vec3.ZERO;
        }
        float keep = descentStart < 0L ? 1.0F : 1.0F - descentEase();
        return carried.scale(keep);
    }

    private static float secondsSince(long frame) {
        return frame < 0L ? 0.0F : Math.min(0.1F, (Util.getMillis() - frame) / 1000.0F);
    }

    public static double offset() {
        if (descentStart >= 0L) {
            return height * (1.0F - descentEase());
        }
        float t = Mth.clamp((Util.getMillis() - riseStart) / 1000.0F / RISE_SECONDS, 0.0F, 1.0F);
        return height * t * t * (3.0F - 2.0F * t);
    }
}
