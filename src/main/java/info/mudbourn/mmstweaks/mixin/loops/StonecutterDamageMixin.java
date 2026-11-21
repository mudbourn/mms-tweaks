package info.mudbourn.mmstweaks.mixin.loops;

import info.mudbourn.mmstweaks.loops.DatapackLoops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Stonecutter Damage's once-a-second hurt, applied by the stonecutter to what stands on it.
@Mixin(StonecutterBlock.class)
public abstract class StonecutterDamageMixin extends Block {

    @Unique
    private static final ResourceKey<DamageType> MMS_TWEAKS$CUT = ResourceKey.create(
        Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("stonecutter_damage", "stonecutter"));

    @Unique
    private static final TagKey<EntityType<?>> MMS_TWEAKS$SPARED = TagKey.create(
        Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("stonecutter_damage", "blacklist"));

    private StonecutterDamageMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level instanceof ServerLevel server
            && level.getGameTime() % 20 == 0
            && DatapackLoops.active(DatapackLoops.STONECUTTER_DAMAGE)
            && !entity.is(MMS_TWEAKS$SPARED)) {
            server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(MMS_TWEAKS$CUT).ifPresent(type ->
                entity.hurtServer(server, new DamageSource(type, Vec3.atBottomCenterOf(pos)), 1.0F));
        }
    }
}
