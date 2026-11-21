package info.mudbourn.mmstweaks.loops;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Applies Shield Upgrades' dropped-shield effects as items load, instead of three item scans a tick. */
public final class ShieldUpgradesItems {

    private static final String PREVENT_LAVA = "shield_upgrades.item.shield.prevent.lava";
    private static final int LAVA_INTERVAL = 5;

    private static final Set<ItemEntity> PENDING = new LinkedHashSet<>();
    private static final Set<ItemEntity> LAVA_PROOF = new LinkedHashSet<>();

    private ShieldUpgradesItems() {}

    static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) PENDING.add(item);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) {
                PENDING.remove(item);
                LAVA_PROOF.remove(item);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(ShieldUpgradesItems::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!DatapackLoops.active(DatapackLoops.SHIELD_UPGRADES)) {
            return;
        }
        if (DatapackLoops.starting(DatapackLoops.SHIELD_UPGRADES)) {
            for (ServerLevel level : server.getAllLevels()) {
                level.getAllEntities().forEach(entity -> {
                    if (entity instanceof ItemEntity item) PENDING.add(item);
                });
            }
        }
        if (!PENDING.isEmpty()) {
            List<ItemEntity> batch = new ArrayList<>(PENDING);
            PENDING.clear();
            for (ItemEntity item : batch) {
                if (!item.isRemoved()) upgrade(item);
            }
        }
        if (server.getTickCount() % LAVA_INTERVAL == 0) {
            for (ItemEntity item : List.copyOf(LAVA_PROOF)) {
                liftOutOfLava(item);
            }
        }
    }

    private static void upgrade(ItemEntity item) {
        CustomData data = item.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        Set<String> tags = item.entityTags();
        if (has(data, "shield_upgrades.ender") && !tags.contains("shield_upgrades.ender")
            && item.level().dimension() == Level.END) {
            item.addTag("shield_upgrades.ender");
            item.setPermanentlyInvulnerable(true);
            item.setNoGravity(true);
            item.setDeltaMovement(Vec3.ZERO);
        }
        if (has(data, "shield_upgrades.fire") && !tags.contains("shield_upgrades.fire")) {
            item.addTag("shield_upgrades.fire");
            item.addTag(PREVENT_LAVA);
            item.setPermanentlyInvulnerable(true);
        }
        if (has(data, "shield_upgrades.netherite") && !tags.contains("shield_upgrades.netherite")) {
            item.addTag("shield_upgrades.netherite");
            item.addTag(PREVENT_LAVA);
            item.setPermanentlyInvulnerable(true);
        }
        if (item.entityTags().contains(PREVENT_LAVA)) {
            LAVA_PROOF.add(item);
        }
    }

    private static boolean has(CustomData data, String key) {
        CompoundTag match = new CompoundTag();
        match.putBoolean(key, true);
        return data.matchedBy(match);
    }

    private static void liftOutOfLava(ItemEntity item) {
        if (item.isRemoved()) {
            LAVA_PROOF.remove(item);
            return;
        }
        BlockPos below = BlockPos.containing(item.getX(), item.getY() - 0.1, item.getZ());
        if (item.level().getBlockState(below).is(Blocks.LAVA)) {
            item.teleportTo(item.getX(), item.getY() + 0.2, item.getZ());
        }
    }
}
