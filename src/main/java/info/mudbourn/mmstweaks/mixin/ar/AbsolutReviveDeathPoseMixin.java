package info.mudbourn.mmstweaks.mixin.ar;

import goetic.mods.absolutrevive.client.RenderStateExtensions;
import info.mudbourn.mmstweaks.client.revive.DeathPose;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a player who dies while downed in AbsolutRevive's lying pose instead of standing for the death fall.
 */
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class AbsolutReviveDeathPoseMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void mmsTweaks$stayDown(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof FabricRenderState && DeathPose.keepsDownedPose(entity)) {
            state.setData(RenderStateExtensions.UNCONSCIOUS, true);
            state.setData(RenderStateExtensions.COLLAPSE_PROGRESS, 1.0F);
        }
    }
}
