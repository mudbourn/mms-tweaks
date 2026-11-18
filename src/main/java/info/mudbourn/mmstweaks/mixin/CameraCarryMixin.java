package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.client.slain.DeathPan;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the death camera's spot across the respawn teleport and eases it away through the descent.
 */
@Mixin(value = Camera.class, priority = 1100)
public abstract class CameraCarryMixin {

    @Shadow
    private Vec3 position;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void mmsTweaks$carryAcrossRespawn(float partialTicks, CallbackInfo ci) {
        Vec3 carry = DeathPan.carry(position);
        if (carry != Vec3.ZERO) {
            setPosition(position.add(carry));
        }
    }
}
