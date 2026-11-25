package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.command.state.Names;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$nick(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            Component nick = Names.nick(player);
            if (nick != null) {
                cir.setReturnValue(nick);
            }
        }
    }
}
