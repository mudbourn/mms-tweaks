package info.mudbourn.mmstweaks.client.revive;

import goetic.mods.absolutrevive.client.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Drives the slow third person orbit around a downed player and the ease back into their own view.
 */
public final class DownedCamera {

    public static final float DISTANCE = 4.5F;

    public static final float PITCH = 28.0F;

    private static final float EASE_IN_SECONDS = 1.6F;

    private static final float EASE_OUT_SECONDS = 1.1F;

    private static final float DEGREES_PER_SECOND = 7.0F;

    private static LocalPlayer owner;

    private static float weight;

    private static float orbitYaw;

    private static long lastFrame = -1L;

    private static float frameSeconds;

    private static float bodyAlpha = 1.0F;

    private static float smoothYaw;

    private static float smoothPitch;

    private static boolean smoothing;

    private DownedCamera() {}

    // Advances the orbit for this frame and returns the eased blend toward it, 0 when inactive.
    public static float frame(float viewYaw) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        long now = Util.getMillis();
        float seconds = lastFrame < 0L ? 0.0F : Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        frameSeconds = seconds;
        if (player == null || player != owner) {
            boolean respawned = owner != null && player != null && !owner.isAlive();
            owner = player;
            if (!respawned) {
                weight = 0.0F;
            }
        }
        if (player == null) {
            return 0.0F;
        }

        boolean downed = player.isAlive() && ClientEventHandler.isUnconscious(player);
        if (downed && weight <= 0.0F) {
            orbitYaw = viewYaw;
        }
        if (downed || !player.isAlive()) {
            weight = Math.min(1.0F, weight + seconds / EASE_IN_SECONDS);
        } else {
            weight = Math.max(0.0F, weight - seconds / EASE_OUT_SECONDS);
        }
        if (weight > 0.0F) {
            orbitYaw = Mth.wrapDegrees(orbitYaw + DEGREES_PER_SECOND * seconds);
        } else {
            smoothing = false;
            bodyAlpha = 1.0F;
        }
        return weight * weight * (3.0F - 2.0F * weight);
    }

    // Drops the orbit at once so the next frame is the player's own view.
    public static void cut() {
        weight = 0.0F;
    }

    // Eases the player's own view toward where it really points, so a teleport turns the camera instead of snapping it.
    public static float smoothYaw(float yaw) {
        if (!smoothing) {
            return yaw;
        }
        smoothYaw += Mth.wrapDegrees(yaw - smoothYaw) * View.follow(frameSeconds);
        return smoothYaw;
    }

    public static float smoothPitch(float pitch) {
        if (!smoothing) {
            smoothing = true;
            smoothPitch = pitch;
            return pitch;
        }
        smoothPitch += (pitch - smoothPitch) * View.follow(frameSeconds);
        return smoothPitch;
    }

    public static void startSmoothing(float yaw) {
        if (!smoothing) {
            smoothYaw = yaw;
        }
    }

    public static boolean orbiting() {
        return weight > 0.0F;
    }

    public static void setBodyAlpha(float alpha) {
        bodyAlpha = alpha;
    }

    // The opacity the local player's body renders at, 1 when no orbit is running.
    public static float bodyAlpha() {
        return weight > 0.0F ? bodyAlpha : 1.0F;
    }

    public static float orbitYaw() {
        return orbitYaw;
    }
}
