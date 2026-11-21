package info.mudbourn.mmstweaks.mixin.loops;

import info.mudbourn.mmstweaks.loops.DatapackLoops;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerFunctionManager.class)
public abstract class DatapackLoopBlockMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$blockReplacedLoop(CommandFunction<CommandSourceStack> function, CommandSourceStack source, CallbackInfo ci) {
        if (DatapackLoops.intercept(function.id())) {
            ci.cancel();
        }
    }
}
