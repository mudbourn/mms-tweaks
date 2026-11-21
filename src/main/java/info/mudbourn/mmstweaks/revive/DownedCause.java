package info.mudbourn.mmstweaks.revive;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public final class DownedCause {

    private static final Map<UUID, DamageSource> LAST_HIT = new ConcurrentHashMap<>();
    private static final Map<UUID, DamageSource> CAUSES = new ConcurrentHashMap<>();

    private DownedCause() {}

    public static void hit(Player player, DamageSource source) {
        LAST_HIT.put(player.getUUID(), source);
    }

    public static void downed(Player player) {
        DamageSource source = LAST_HIT.get(player.getUUID());
        if (source != null) {
            CAUSES.put(player.getUUID(), source);
        }
    }

    public static DamageSource take(Player player) {
        return CAUSES.remove(player.getUUID());
    }
}
