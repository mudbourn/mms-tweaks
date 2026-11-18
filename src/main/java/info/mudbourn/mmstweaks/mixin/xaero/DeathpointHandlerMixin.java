package info.mudbourn.mmstweaks.mixin.xaero;

import info.mudbourn.mmstweaks.client.xaero.XaeroDeathpoints;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.waypoint.DeathpointHandler;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Hands every deathpoint Xaero's Minimap creates to XaeroDeathpoints.
 */
@Mixin(DeathpointHandler.class)
public class DeathpointHandlerMixin {

    @Inject(
        method = "createDeathpoint(Lnet/minecraft/world/entity/player/Player;Lxaero/hud/minimap/world/MinimapWorld;Z)V",
        at = @At("TAIL")
    )
    private void mmsTweaks$hideDeathpoint(Player player, MinimapWorld world, boolean temporary, CallbackInfo ci) {
        XaeroDeathpoints.captured(world);
    }
}
