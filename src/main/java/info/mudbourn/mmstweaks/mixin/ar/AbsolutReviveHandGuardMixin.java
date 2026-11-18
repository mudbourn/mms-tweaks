package info.mudbourn.mmstweaks.mixin.ar;

import info.mudbourn.mmstweaks.revive.HandGuard;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses the hand clear and item drop AbsolutRevive makes while HandGuard is active.
 */
@Mixin(LivingEntity.class)
public abstract class AbsolutReviveHandGuardMixin {

    @Inject(method = "setItemInHand", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$keepHand(InteractionHand hand, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty() && HandGuard.blocks((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "createItemStackToDrop", at = @At("HEAD"), cancellable = true)
    private void mmsTweaks$keepDrop(ItemStack stack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        if (HandGuard.blocks((LivingEntity) (Object) this)) {
            cir.setReturnValue(null);
        }
    }
}
