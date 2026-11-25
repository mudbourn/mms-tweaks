package info.mudbourn.mmstweaks.mixin.water;

import info.mudbourn.mmstweaks.water.WaterCurrents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Players swim 15% faster than vanilla in any water.
@Mixin(LivingEntity.class)
public abstract class SwimSpeedMixin {

    @ModifyArg(
        method = "travelInWater",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
        ),
        index = 0
    )
    private float mmsTweaks$easierSwimming(float speed) {
        return (Object) this instanceof Player ? speed * WaterCurrents.SWIM_SPEED_FACTOR : speed;
    }
}
