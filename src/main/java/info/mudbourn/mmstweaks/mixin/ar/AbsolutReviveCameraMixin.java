package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.client.revive.DownedCamera;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blends the camera from the player's own view into a third person orbit while they are downed.
 */
@Mixin(Camera.class)
public abstract class AbsolutReviveCameraMixin {

    private static final float FADE_NEAR = 0.6F;

    private static final float FADE_FAR = 2.2F;

    @Shadow
    private boolean detached;

    @Shadow
    private Vec3 position;

    @Shadow
    private Entity entity;

    @Shadow
    public abstract float xRot();

    @Shadow
    public abstract float yRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Shadow
    private float getMaxZoom(float cameraDist) {
        throw new AssertionError();
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void mmsTweaks$downedOrbit(float partialTicks, CallbackInfo ci) {
        float viewYaw = yRot();
        float blend = DownedCamera.frame(viewYaw);
        if (blend <= 0.0F || entity == null) {
            return;
        }
        Vec3 own = position;
        Vec3 eye = entity.getEyePosition(partialTicks);
        boolean ownDetached = detached;
        DownedCamera.startSmoothing(viewYaw);
        float ownPitch = DownedCamera.smoothPitch(xRot());
        float ownYaw = DownedCamera.smoothYaw(viewYaw);

        setRotation(
            ownYaw + Mth.wrapDegrees(DownedCamera.orbitYaw() - ownYaw) * blend,
            Mth.lerp(blend, ownPitch, DownedCamera.PITCH)
        );
        setPosition(eye);
        move(-getMaxZoom(DownedCamera.DISTANCE * blend), 0.0F, 0.0F);
        setPosition(position.add(own.subtract(eye).scale(1.0F - blend)));
        float distance = (float) position.distanceTo(eye);
        float alpha = ownDetached ? 1.0F : Mth.clamp((distance - FADE_NEAR) / (FADE_FAR - FADE_NEAR), 0.0F, 1.0F);
        DownedCamera.setBodyAlpha(alpha);
        detached = ownDetached || alpha > 0.0F;
    }
}
