package info.mudbourn.mmstweaks.client.camera;

import info.mudbourn.mmstweaks.client.CinematicBridge;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;

/**
 * Forces first person through Perspective API's override chain while a Cinematic Respawn sequence runs.
 */
public final class CameraGlue {

    private static final String FIRST_PERSON = "perspective_api.first_person";

    private static final int PRIORITY = 1000;

    private CameraGlue() {}

    // Call only when leawind_third_person, perspective_api and cinematic_respawn are loaded.
    public static void register() {
        PerspectiveAPI.runWhenReady(
            "camera_glue",
            () -> PerspectiveAPI.getOverrideChain().register(PRIORITY, CameraGlue::override)
        );
    }

    private static String override() {
        boolean cinematic = CinematicBridge.isActive() || CinematicBridge.isCameraTransitionActive();
        return cinematic ? FIRST_PERSON : null;
    }
}
