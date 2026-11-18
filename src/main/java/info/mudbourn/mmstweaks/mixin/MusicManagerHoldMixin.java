package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.client.slain.SlainFlow;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the music manager from fading or starting music while a game over jingle owns the audio.
 */
@Mixin(MusicManager.class)
public class MusicManagerHoldMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$holdForJingle(CallbackInfo ci) {
        if (SlainFlow.musicHeld()) {
            ci.cancel();
        }
    }
}
