package info.mudbourn.mmstweaks.revive;

import goetic.mods.absolutrevive.common.EventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Marks the stretch where AbsolutRevive disarms downed players, so their hand items stay put.
 */
public final class HandGuard {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private HandGuard() {}

    public static void enter() {
        ACTIVE.set(true);
    }

    public static void exit() {
        ACTIVE.set(false);
    }

    public static boolean blocks(LivingEntity entity) {
        return ACTIVE.get() && entity instanceof Player player && EventHandler.isUnconscious(player);
    }
}
