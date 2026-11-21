package info.mudbourn.mmstweaks.loops;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/** Sizes each mob once as it loads, instead of the Random Mob Sizes one-second scan of every entity's NBT. */
public final class RandomMobSizesScan {

    private static final Identifier ON_NEW_MOB = Identifier.fromNamespaceAndPath("random_mob_sizes", "on_new_mob");
    private static final List<String> SKIP_TAGS =
        List.of("random_mob_sizes.checked", "smithed.entity", "smithed.strict", "global.ignore");

    private static final Set<Entity> PENDING = new LinkedHashSet<>();

    private RandomMobSizesScan() {}

    static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (candidate(entity)) PENDING.add(entity);
        });
        ServerTickEvents.END_SERVER_TICK.register(RandomMobSizesScan::tick);
    }

    private static boolean candidate(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player || entity instanceof ArmorStand) {
            return false;
        }
        for (String tag : SKIP_TAGS) {
            if (entity.entityTags().contains(tag)) return false;
        }
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (!DatapackLoops.active(DatapackLoops.RANDOM_MOB_SIZES)) {
            return;
        }
        if (DatapackLoops.starting(DatapackLoops.RANDOM_MOB_SIZES)) {
            for (ServerLevel level : server.getAllLevels()) {
                level.getAllEntities().forEach(entity -> {
                    if (candidate(entity)) PENDING.add(entity);
                });
            }
        }
        if (PENDING.isEmpty()) {
            return;
        }
        List<Entity> batch = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Entity entity : batch) {
            if (!entity.isRemoved() && candidate(entity)) {
                DatapackLoops.run(server, ON_NEW_MOB, entity, false);
            }
        }
    }
}
