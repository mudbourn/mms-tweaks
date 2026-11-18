package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.Downed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Matches the client revive prompt and progress bar to the 70 tick bare-hand revive.
 */
@Mixin(targets = "goetic.mods.absolutrevive.client.ClientEventHandler")
public class AbsolutReviveRescueTimeClientMixin {

    @ModifyConstant(method = "updateInteractionPromptState", constant = @Constant(intValue = 160))
    private static int mmsTweaks$promptTicks(int original) {
        return Downed.RESCUE_TICKS;
    }

    @ModifyConstant(method = "getInteractionHoldDurationSeconds", constant = @Constant(floatValue = 8.0F))
    private static float mmsTweaks$promptSeconds(float original) {
        return Downed.RESCUE_TICKS / 20.0F;
    }

    @ModifyConstant(
        method = {"getInteractionHoldProgress", "getDisplayedInteractionHoldTicks"},
        constant = @Constant(floatValue = 160.0F)
    )
    private static float mmsTweaks$progressTicks(float original) {
        return Downed.RESCUE_TICKS;
    }
}
