package info.mudbourn.mmstweaks.mixin.water;

import info.mudbourn.mmstweaks.water.WaterCurrents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Adds the MMS current to players and boats after every other fluid push has run.
@Mixin(Entity.class)
public abstract class EntityCurrentMixin {

    @Unique
    private Vec3 mmsTweaks$beforeFluid = Vec3.ZERO;

    @Inject(method = "updateFluidInteraction", at = @At("HEAD"))
    private void mmsTweaks$rememberMovement(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player || self instanceof AbstractBoat) {
            mmsTweaks$beforeFluid = self.getDeltaMovement();
        }
    }

    @Inject(method = "updateFluidInteraction", at = @At("RETURN"))
    private void mmsTweaks$applyCurrent(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player) {
            WaterCurrents.swimmer(player, player.getDeltaMovement().subtract(mmsTweaks$beforeFluid));
        } else if (self instanceof AbstractBoat boat) {
            WaterCurrents.boat(boat, boat.getDeltaMovement().subtract(mmsTweaks$beforeFluid));
        }
    }
}
