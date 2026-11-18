package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.client.revive.SelfReviveHold;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rewords the downed HUD for the loot rule and adds the self revive prompt under it.
 */
@Mixin(targets = "goetic.mods.absolutrevive.client.StatusEffectLayer")
public class AbsolutReviveStatusLayerMixin {

    @ModifyConstant(method = "onHudRender", constant = @Constant(stringValue = "absolutrevive.gui.waiting_for_rescue"))
    private String mmsTweaks$waitingKey(String original) {
        return SelfReviveHold.lootable() ? "mms_tweaks.downed.waiting_loot" : "mms_tweaks.downed.waiting";
    }

    @ModifyConstant(method = "onHudRender", constant = @Constant(stringValue = "absolutrevive.gui.rescue_help"))
    private String mmsTweaks$helpKey(String original) {
        return SelfReviveHold.lootable() ? "mms_tweaks.downed.help_loot" : "mms_tweaks.downed.help";
    }

    @Inject(method = "onHudRender", at = @At("TAIL"))
    private void mmsTweaks$selfRevivePrompt(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        SelfReviveHold.render(graphics, delta);
    }
}
