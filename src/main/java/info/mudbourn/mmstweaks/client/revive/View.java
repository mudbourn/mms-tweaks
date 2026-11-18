package info.mudbourn.mmstweaks.client.revive;

/**
 * Frame-rate independent follow rate for camera angles that must never snap.
 */
public final class View {

    private static final float FOLLOW_PER_SECOND = 9.0F;

    private View() {}

    public static float follow(float seconds) {
        return 1.0F - (float) Math.exp(-FOLLOW_PER_SECOND * seconds);
    }
}
