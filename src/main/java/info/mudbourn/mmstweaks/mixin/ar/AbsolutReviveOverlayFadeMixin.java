package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.client.ReviveOverlayFade;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fades the downed HUD out while a Cinematic Respawn death sequence plays.
 */
@Mixin(targets = "goetic.mods.absolutrevive.client.StatusEffectLayer")
public class AbsolutReviveOverlayFadeMixin {

    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$updateFade(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        ReviveOverlayFade.update();
        if (ReviveOverlayFade.hidden()) {
            ci.cancel();
        }
    }

    @ModifyArg(
        method = {
            "onHudRender",
            "renderVignette",
            "fillEdge",
            "renderDeathDangerOverlay",
            "renderGiveUpProgress",
            "renderRescuePrompt"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
        ),
        index = 4,
        require = 0
    )
    private static int mmsTweaks$fadeFill(int color) {
        return ReviveOverlayFade.apply(color);
    }

    @ModifyArg(
        method = {
            "onHudRender",
            "renderGiveUpProgress",
            "renderRescuePrompt"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
        ),
        index = 4,
        require = 0
    )
    private static int mmsTweaks$fadeText(int color) {
        return ReviveOverlayFade.apply(color);
    }
}
