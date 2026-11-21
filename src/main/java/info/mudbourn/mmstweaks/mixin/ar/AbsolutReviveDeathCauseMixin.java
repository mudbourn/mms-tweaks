package info.mudbourn.mmstweaks.mixin.ar;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmstweaks.revive.DownedCause;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Gives a downed player's death the damage source that downed them.
@Mixin(targets = "goetic.mods.absolutrevive.common.damagesystem.PlayerDamageModel")
public class AbsolutReviveDeathCauseMixin {

    @WrapOperation(
        method = { "giveUp", "tick" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;die(Lnet/minecraft/world/damagesource/DamageSource;)V")
    )
    private void mmsTweaks$dieFromCause(Player player, DamageSource fallback, Operation<Void> original) {
        DamageSource cause = DownedCause.take(player);
        if (cause != null) {
            player.getCombatTracker().recordDamage(cause, 0.0F);
        }
        original.call(player, cause != null ? cause : fallback);
    }
}
