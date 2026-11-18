package info.mudbourn.mmstweaks.mixin.ar;

import goetic.mods.absolutrevive.common.EventHandler;
import info.mudbourn.mmstweaks.revive.Downed;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Announces a player going down and gives them the rundown, once per down.
 */
@Mixin(targets = "goetic.mods.absolutrevive.common.EventHandler")
public class AbsolutReviveNotifyMixin {

    private static boolean mmsTweaks$wasUnconscious = false;

    @Inject(method = "handleFatalPlayerDamage", at = @At("HEAD"))
    private static void mmsTweaks$recordPriorState(Player player, CallbackInfoReturnable<Boolean> cir) {
        mmsTweaks$wasUnconscious = EventHandler.isUnconscious(player);
    }

    @Inject(method = "handleFatalPlayerDamage", at = @At("RETURN"))
    private static void mmsTweaks$notifyDowned(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (mmsTweaks$wasUnconscious || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel) || !EventHandler.isUnconscious(player)) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        Component msg = Component.translatable("mms_tweaks.absolutrevive.downed", player.getDisplayName())
            .withStyle(ChatFormatting.RED);
        server.getPlayerList().broadcastSystemMessage(msg, false);
        if (player instanceof ServerPlayer serverPlayer) {
            Downed.onDowned(serverPlayer);
        }
    }
}
