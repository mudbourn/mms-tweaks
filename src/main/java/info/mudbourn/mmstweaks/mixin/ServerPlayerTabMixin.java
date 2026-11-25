package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.command.state.Names;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerTabMixin {

    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void mmsTweaks$tab(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Names.tab((ServerPlayer) (Object) this, cir.getReturnValue()));
    }
}
