package info.mudbourn.mmstweaks.loops;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Datapack loop functions that mms-tweaks runs in Java instead. A loop is
 * blocked when its datapack calls it, and that call switches its Java
 * replacement on until the server stops.
 */
public final class DatapackLoops {

    public static final String BUBBLELLANEOUS = "bubblellaneous";
    public static final String RANDOM_MOB_SIZES = "random_mob_sizes";
    public static final String SHIELD_UPGRADES = "shield_upgrades";
    public static final String STONECUTTER_DAMAGE = "stonecutter_damage";

    private static final Map<Identifier, String> LOOPS = Map.of(
        Identifier.fromNamespaceAndPath("bubblellaneous", "tick"), BUBBLELLANEOUS,
        Identifier.fromNamespaceAndPath("random_mob_sizes", "1s_loop"), RANDOM_MOB_SIZES,
        Identifier.fromNamespaceAndPath("shield_upgrades", "tick"), SHIELD_UPGRADES,
        Identifier.fromNamespaceAndPath("shield_upgrades", "schedule/5t"), SHIELD_UPGRADES,
        Identifier.fromNamespaceAndPath("stonecutter_damage", "second"), STONECUTTER_DAMAGE
    );

    private static final Set<String> ACTIVE = new HashSet<>();
    private static final Set<String> STARTED = new HashSet<>();

    private DatapackLoops() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE.clear();
            STARTED.clear();
        });
        BubblellaneousTick.register();
        RandomMobSizesScan.register();
        ShieldUpgradesItems.register();
    }

    /** Returns true when {@code id} is a replaced loop, marking its replacement active. */
    public static boolean intercept(Identifier id) {
        String loop = LOOPS.get(id);
        if (loop == null) {
            return false;
        }
        ACTIVE.add(loop);
        return true;
    }

    public static boolean active(String loop) {
        return ACTIVE.contains(loop);
    }

    /** Returns true once per server run, the first time {@code loop} is seen active. */
    public static boolean starting(String loop) {
        return ACTIVE.contains(loop) && STARTED.add(loop);
    }

    // Runs a datapack function as the entity, optionally at its position.
    public static void run(MinecraftServer server, Identifier function, Entity entity, boolean at) {
        server.getFunctions().get(function).ifPresent(f -> {
            CommandSourceStack source = server.getFunctions().getGameLoopSender().withEntity(entity);
            if (at && entity.level() instanceof ServerLevel level) {
                source = source.withLevel(level).withPosition(entity.position()).withRotation(entity.getRotationVector());
            }
            server.getFunctions().execute(f, source);
        });
    }
}
