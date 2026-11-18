package info.mudbourn.mmstweaks.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/**
 * Reflective handle on Cinematic Respawn's controller, which ships against an older game version.
 */
public final class CinematicBridge {

    private static final String CONTROLLER = "net.gamev.cinematic_respawn.client.RespawnCinematicController";

    private static final Method IS_ACTIVE;

    private static final Method IS_TRANSITION;

    private static final Method CAN_SKIP;

    private static final Method SKIP;

    private static final Method ON_CLONE;

    private static final Method TRANSITION_TO;

    private static final Field RESPAWN_REQUESTED;

    private static final Object RESPAWNING;

    private static final Field POSITION_SYNCED;

    private static final Field STATE;

    private static final Field STATE_TICKS;

    static {
        Method isActive = null;
        Method isTransition = null;
        Method canSkip = null;
        Method skip = null;
        Method onClone = null;
        Method transitionTo = null;
        Field respawnRequested = null;
        Object respawning = null;
        Field positionSynced = null;
        Field stateField = null;
        Field stateTicks = null;
        if (FabricLoader.getInstance().isModLoaded("cinematic_respawn")) {
            try {
                Class<?> controller = Class.forName(CONTROLLER);
                Class<?> state = Class.forName(CONTROLLER + "$State");
                isActive = controller.getMethod("isActive");
                isTransition = controller.getMethod("isCameraTransitionActive");
                canSkip = controller.getMethod("canSkip");
                skip = controller.getMethod("skip");
                onClone = controller.getMethod("onPlayerClone");
                transitionTo = controller.getDeclaredMethod("transitionTo", state);
                transitionTo.setAccessible(true);
                respawnRequested = controller.getDeclaredField("respawnRequested");
                respawnRequested.setAccessible(true);
                for (Object constant : state.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals("RESPAWNING")) {
                        respawning = constant;
                    }
                }
                if (respawning == null) {
                    throw new NoSuchFieldException("RESPAWNING");
                }
                positionSynced = optionalField(controller, "wakeUpPositionSynchronized");
                stateField = optionalField(controller, "state");
                stateTicks = optionalField(controller, "stateTicks");
            } catch (ReflectiveOperationException | RuntimeException e) {
                LoggerFactory.getLogger("mms_tweaks").error("Cinematic Respawn bridge disabled", e);
                isActive = null;
            }
        }
        IS_ACTIVE = isActive;
        IS_TRANSITION = isTransition;
        CAN_SKIP = canSkip;
        SKIP = skip;
        ON_CLONE = onClone;
        TRANSITION_TO = transitionTo;
        RESPAWN_REQUESTED = respawnRequested;
        RESPAWNING = respawning;
        POSITION_SYNCED = positionSynced;
        STATE = stateField;
        STATE_TICKS = stateTicks;
    }

    // The controller's state name, or an empty string when it cannot be read.
    public static String state() {
        try {
            return present() && STATE != null ? ((Enum<?>) STATE.get(null)).name() : "";
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    public static int stateTicks() {
        try {
            return present() && STATE_TICKS != null ? STATE_TICKS.getInt(null) : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private CinematicBridge() {}

    public static boolean present() {
        return IS_ACTIVE != null;
    }

    public static boolean isActive() {
        return present() && (Boolean) call(IS_ACTIVE);
    }

    public static boolean isCameraTransitionActive() {
        return present() && (Boolean) call(IS_TRANSITION);
    }

    // Puts the controller in its respawning state so the next player clone starts the wake-up shot.
    public static void armRespawn() {
        if (!present()) {
            return;
        }
        try {
            RESPAWN_REQUESTED.setBoolean(null, true);
            TRANSITION_TO.invoke(null, RESPAWNING);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // Starts the wake-up shot with the respawn position marked as synced.
    public static void wakeUp() {
        if (!present()) {
            return;
        }
        call(ON_CLONE);
        if (POSITION_SYNCED != null) {
            try {
                POSITION_SYNCED.setBoolean(null, true);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void skip() {
        if (present() && (Boolean) call(CAN_SKIP)) {
            call(SKIP);
        }
    }

    private static Field optionalField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Object call(Method method) {
        try {
            return method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
