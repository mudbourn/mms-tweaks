package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.HeldTotem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a held or worn totem save the player before AbsolutRevive downs them.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveTotemMixin {

    @Inject(method = "handleFatalPlayerDamage", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$deferToTotem(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (HeldTotem.has(player)) {
            cir.setReturnValue(false);
        }
    }
}
