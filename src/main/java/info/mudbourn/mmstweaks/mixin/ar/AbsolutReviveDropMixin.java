package info.mudbourn.mmstweaks.mixin.ar;

import goetic.mods.absolutrevive.common.EventHandler;
import info.mudbourn.mmstweaks.revive.HandGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps hand items on a downed player through the knockout and the per-tick disarm, and syncs the ar_downed tag.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveDropMixin {

    @Inject(method = "handleFatalPlayerDamage", at = @At("HEAD"))
    private static void mmsTweaks$guardFatal(Player player, CallbackInfoReturnable<Boolean> cir) {
        HandGuard.enter();
    }

    @Inject(method = "handleFatalPlayerDamage", at = @At("RETURN"))
    private static void mmsTweaks$releaseFatal(Player player, CallbackInfoReturnable<Boolean> cir) {
        HandGuard.exit();
    }

    @Inject(method = "tickPlayers", at = @At("HEAD"))
    private static void mmsTweaks$guardTick(ServerLevel level, CallbackInfo ci) {
        HandGuard.enter();
    }

    @Inject(method = "tickPlayers", at = @At("TAIL"))
    private static void mmsTweaks$syncArDownedTag(ServerLevel level, CallbackInfo ci) {
        HandGuard.exit();
        for (ServerPlayer player : level.players()) {
            if (EventHandler.isUnconscious(player)) {
                player.addTag("ar_downed");
            } else {
                player.removeTag("ar_downed");
            }
        }
    }
}
