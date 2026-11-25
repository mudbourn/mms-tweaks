package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.command.state.PlayerState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "setGameMode", at = @At("RETURN"))
    private void mmsTweaks$reapply(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            PlayerState.apply((ServerPlayer) (Object) this);
        }
    }
}
