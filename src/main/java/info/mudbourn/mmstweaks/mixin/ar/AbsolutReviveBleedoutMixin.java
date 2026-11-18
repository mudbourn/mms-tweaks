package info.mudbourn.mmstweaks.mixin.ar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Shortens the bleedout countdown from 90 to 30 seconds.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveBleedoutMixin {

    @ModifyConstant(method = "handleFatalPlayerDamage", constant = @Constant(intValue = 1800))
    private static int mmsTweaks$shortenBleedout(int original) {
        return 600;
    }
}
