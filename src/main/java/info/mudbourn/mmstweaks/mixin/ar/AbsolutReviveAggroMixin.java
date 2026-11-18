package info.mudbourn.mmstweaks.mixin.ar;

import goetic.mods.absolutrevive.common.EventHandler;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes mobs near a downed player drop goal targets, brain targets and persistent anger aimed at them.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveAggroMixin {

    private static final TagKey<EntityType<?>> IGNORES_DOWNED = TagKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("mms_tweaks", "revive_ignores_downed")
    );

    private static final double RADIUS = 32.0;

    @Inject(method = "tickPlayers", at = @At("TAIL"))
    private static void mmsTweaks$suppressAggroAroundDowned(ServerLevel level, CallbackInfo ci) {
        for (ServerPlayer downed : level.players()) {
            if (!EventHandler.isUnconscious(downed)) {
                continue;
            }
            UUID downedId = downed.getUUID();
            AABB box = downed.getBoundingBox().inflate(RADIUS);

            for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
                if (mob.typeHolder().is(IGNORES_DOWNED)) {
                    continue;
                }

                if (mob.getTarget() == downed) {
                    mob.setTarget(null);
                }

                Brain<?> brain = mob.getBrain();
                if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                        && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == downed) {
                    brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
                if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)
                        && downedId.equals(brain.getMemory(MemoryModuleType.ANGRY_AT).orElse(null))) {
                    brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                }

                if (mob instanceof NeutralMob neutral && downedId.equals(neutral.getPersistentAngerTarget())) {
                    neutral.stopBeingAngry();
                }
            }
        }
    }
}
