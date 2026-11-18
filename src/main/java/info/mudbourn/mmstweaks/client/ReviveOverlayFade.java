package info.mudbourn.mmstweaks.client;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Fades AbsolutRevive's downed HUD out once a Cinematic Respawn sequence starts.
 */
public final class ReviveOverlayFade {

    private static final long FADE_MILLIS = 400L;

    private static long startedAt = -1L;

    private static float alpha = 1.0F;

    private ReviveOverlayFade() {}

    public static void update() {
        if (!CinematicBridge.isActive() && !CinematicBridge.isCameraTransitionActive()) {
            startedAt = -1L;
            alpha = 1.0F;
            return;
        }
        long now = Util.getMillis();
        if (startedAt < 0L) {
            startedAt = now;
        }
        alpha = 1.0F - Mth.clamp((now - startedAt) / (float) FADE_MILLIS, 0.0F, 1.0F);
    }

    public static boolean hidden() {
        return alpha <= 0.0F;
    }

    public static int apply(int argb) {
        if (alpha >= 1.0F) {
            return argb;
        }
        int a = Math.round((argb >>> 24) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
