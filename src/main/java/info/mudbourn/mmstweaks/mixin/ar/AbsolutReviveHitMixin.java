package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.DownedCause;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Remembers the latest damage source that hit a player.
@Mixin(ServerPlayer.class)
public class AbsolutReviveHitMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void mmsTweaks$rememberHit(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        DownedCause.hit((ServerPlayer) (Object) this, source);
    }
}
