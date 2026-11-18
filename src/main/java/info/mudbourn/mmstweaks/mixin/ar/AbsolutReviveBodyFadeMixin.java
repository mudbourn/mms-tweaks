package info.mudbourn.mmstweaks.mixin.ar;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmstweaks.client.revive.DownedCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fades the local player's body out as the downed orbit camera closes in on it.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class AbsolutReviveBodyFadeMixin {

    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    private static float mmsTweaks$alpha = 1.0F;

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void mmsTweaks$pickAlpha(
        LivingEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean self = state instanceof AvatarRenderState avatar && minecraft.player != null && avatar.id == minecraft.player.getId();
        mmsTweaks$alpha = self ? DownedCamera.bodyAlpha() : 1.0F;
    }

    @ModifyVariable(method = SUBMIT, at = @At("STORE"), ordinal = 1)
    private boolean mmsTweaks$translucent(boolean forceTransparent) {
        return forceTransparent || mmsTweaks$alpha < 1.0F;
    }

    @ModifyArg(
        method = SUBMIT,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"
        ),
        index = 6
    )
    private int mmsTweaks$fadeColor(int color) {
        if (mmsTweaks$alpha >= 1.0F) {
            return color;
        }
        return ARGB.color(Math.round(255.0F * mmsTweaks$alpha), color & 0xFFFFFF);
    }

    @ModifyExpressionValue(
        method = SUBMIT,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z"
        )
    )
    private boolean mmsTweaks$hideLayersWhenFaint(boolean original) {
        return original && mmsTweaks$alpha > 0.5F;
    }
}
