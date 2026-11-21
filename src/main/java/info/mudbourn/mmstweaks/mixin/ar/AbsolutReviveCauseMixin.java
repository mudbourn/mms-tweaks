package info.mudbourn.mmstweaks.mixin.ar;

import goetic.mods.absolutrevive.common.EventHandler;
import info.mudbourn.mmstweaks.revive.DownedCause;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Pins the damage source that downed a player.
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveCauseMixin {

    private static boolean mmsTweaks$wasDowned = false;

    @Inject(method = "handleFatalPlayerDamage", at = @At("HEAD"))
    private static void mmsTweaks$recordPriorState(Player player, CallbackInfoReturnable<Boolean> cir) {
        mmsTweaks$wasDowned = EventHandler.isUnconscious(player);
    }

    @Inject(method = "handleFatalPlayerDamage", at = @At("RETURN"))
    private static void mmsTweaks$pinCause(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!mmsTweaks$wasDowned && Boolean.TRUE.equals(cir.getReturnValue()) && EventHandler.isUnconscious(player)) {
            DownedCause.downed(player);
        }
    }
}
