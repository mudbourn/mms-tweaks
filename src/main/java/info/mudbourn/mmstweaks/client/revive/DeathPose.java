package info.mudbourn.mmstweaks.client.revive;

import goetic.mods.absolutrevive.client.ClientEventHandler;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Remembers which players were downed so they stay in the downed pose while they die.
 */
public final class DeathPose {

    private static final long RECENT_TICKS = 20L;

    private static final Int2LongMap LAST_DOWNED = new Int2LongOpenHashMap();

    private static final IntSet KEEP = new IntOpenHashSet();

    private DeathPose() {}

    // True when this entity died while downed, or just after, and should keep lying down.
    public static boolean keepsDownedPose(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int id = player.getId();
        long now = player.level().getGameTime();
        if (player.isAlive() && ClientEventHandler.isUnconscious(player)) {
            LAST_DOWNED.put(id, now);
            KEEP.remove(id);
            return false;
        }
        if (player.isDeadOrDying()) {
            if (KEEP.contains(id)) {
                return true;
            }
            if (LAST_DOWNED.containsKey(id) && now - LAST_DOWNED.get(id) <= RECENT_TICKS) {
                KEEP.add(id);
                return true;
            }
            return false;
        }
        KEEP.remove(id);
        if (LAST_DOWNED.containsKey(id) && now - LAST_DOWNED.get(id) > RECENT_TICKS) {
            LAST_DOWNED.remove(id);
        }
        return false;
    }
}
