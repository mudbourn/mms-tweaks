package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.client.revive.DownedCamera;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the first person hand away while any of the player's body is still showing in the downed orbit.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class AbsolutReviveHandHideMixin {

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$hideHand(CallbackInfo ci) {
        if (DownedCamera.orbiting() && DownedCamera.bodyAlpha() > 0.0F) {
            ci.cancel();
        }
    }
}
