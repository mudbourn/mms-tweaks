package info.mudbourn.mmstweaks.mixin.cr;

import info.mudbourn.mmstweaks.client.CinematicBridge;
import info.mudbourn.mmstweaks.client.slain.DeathPan;
import info.mudbourn.mmstweaks.client.slain.SlainFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets Cinematic Respawn rise and wait behind the game over screen, drives its height from DeathPan and drops its fades.
 */
@Pseudo
@Mixin(targets = "net.gamev.cinematic_respawn.client.RespawnCinematicController", remap = false)
public class CinematicRespawnHoldMixin {

    private static final int RISE_HOLD_TICKS = 89;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$holdWhileSlain(CallbackInfo ci) {
        if (!SlainFlow.holdsCinematicTick()) {
            return;
        }
        String state = CinematicBridge.state();
        boolean rising = state.equals("CAMERA_RISE") && CinematicBridge.stateTicks() < RISE_HOLD_TICKS;
        if (!state.equals("IDLE") && !rising) {
            ci.cancel();
        }
    }

    @Inject(method = "cameraYOffset", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$panHeight(float partialTick, CallbackInfoReturnable<Double> cir) {
        if (DeathPan.active() && CinematicBridge.isActive()) {
            cir.setReturnValue(DeathPan.offset());
        }
    }

    @Inject(method = {"shouldHideHand", "isCameraTransitionActive"}, at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$hideWhileRespawning(CallbackInfoReturnable<Boolean> cir) {
        if (DeathPan.active() && CinematicBridge.state().equals("RESPAWNING")) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "cameraYaw", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$panYaw(float currentYaw, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (DeathPan.active() && CinematicBridge.isActive()) {
            cir.setReturnValue(DeathPan.yaw(currentYaw));
        }
    }

    @Inject(method = "cameraPitch", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$panPitch(float currentPitch, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (DeathPan.active() && CinematicBridge.isActive()) {
            Float pitch = DeathPan.pitch(currentPitch);
            if (pitch != null) {
                cir.setReturnValue(pitch);
            }
        }
    }

    @Inject(method = "fadeAlpha", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$noFade(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (DeathPan.active()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "shouldSuppressDeathScreen", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$keepGameOverScreen(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "onDeathScreenOpening", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$noDeathShot(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onPlayerClone", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$holdWakeUp(CallbackInfo ci) {
        if (SlainFlow.holdsClone()) {
            ci.cancel();
        }
    }
}
