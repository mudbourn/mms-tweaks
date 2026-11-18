package info.mudbourn.mmstweaks.revive;

import com.mojang.serialization.Codec;
import info.mudbourn.mmstweaks.MmsTweaks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

/**
 * Self revives a player has spent this life; cleared on death because it is not copied to the respawned player.
 */
public final class SelfRevives {

    public static final int LIMIT = 3;

    private static final AttachmentType<Integer> USED = AttachmentRegistry.create(
        MmsTweaks.id("self_revives_used"),
        builder -> builder.persistent(Codec.INT).initializer(() -> 0)
    );

    private SelfRevives() {}

    public static void register() {}

    public static int used(Player player) {
        return player.getAttachedOrElse(USED, 0);
    }

    public static void set(Player player, int used) {
        player.setAttached(USED, used);
    }
}
