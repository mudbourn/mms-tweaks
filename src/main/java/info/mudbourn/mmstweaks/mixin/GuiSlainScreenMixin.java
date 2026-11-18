package info.mudbourn.mmstweaks.mixin;

import info.mudbourn.mmstweaks.client.slain.DeathScreens;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces any death screen with the game over screen before it is installed.
 */
@Mixin(Gui.class)
public class GuiSlainScreenMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen mmsTweaks$slainScreenArg(Screen screen) {
        return DeathScreens.replace(screen);
    }

    @ModifyVariable(method = "setScreen", at = @At("STORE"), argsOnly = true)
    private Screen mmsTweaks$slainScreenCreated(Screen screen) {
        return DeathScreens.replace(screen);
    }
}
