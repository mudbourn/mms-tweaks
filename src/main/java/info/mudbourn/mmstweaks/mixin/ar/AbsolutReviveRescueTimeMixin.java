package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.Downed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Shortens the bare-hand revive from 160 to 70 ticks; the defibrillator keeps its 60.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveRescueTimeMixin {

    @ModifyConstant(method = "tickRescueProgress", constant = @Constant(intValue = 160))
    private static int mmsTweaks$shortenRescue(int original) {
        return Downed.RESCUE_TICKS;
    }
}
