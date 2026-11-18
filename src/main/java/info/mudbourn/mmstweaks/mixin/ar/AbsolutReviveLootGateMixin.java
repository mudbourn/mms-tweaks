package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.Downed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops anyone searching a downed player while keepInventory is off.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveLootGateMixin {

    @Inject(method = "openLootMenu", at = @At("HEAD"), cancellable = true)
    private static void mmsTweaks$gateLoot(ServerPlayer rescuer, Player target, CallbackInfo ci) {
        if (!Downed.lootable(target)) {
            ci.cancel();
        }
    }
}
