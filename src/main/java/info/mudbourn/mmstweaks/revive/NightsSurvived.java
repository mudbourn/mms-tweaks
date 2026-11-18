package info.mudbourn.mmstweaks.revive;

import com.mojang.serialization.Codec;
import info.mudbourn.mmstweaks.MmsTweaks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.player.Player;

/**
 * Nights a player has survived this life, counted on a full sleep or when the overworld clock reaches wake up time.
 */
public final class NightsSurvived {

    private static final long SAME_NIGHT_TICKS = 12000L;

    private static final AttachmentType<Integer> NIGHTS = AttachmentRegistry.create(
        MmsTweaks.id("nights_survived"),
        builder -> builder
            .persistent(Codec.INT)
            .initializer(() -> 0)
            .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
    );

    private static final AttachmentType<Long> COUNTED_AT = AttachmentRegistry.create(
        MmsTweaks.id("night_counted_at"),
        builder -> builder.persistent(Codec.LONG)
    );

    private NightsSurvived() {}

    public static void register() {
        EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            if (entity instanceof ServerPlayer player && player.isSleepingLongEnough()) {
                tally(player);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(NightsSurvived::checkMorning);
    }

    public static int count(Player player) {
        return player.getAttachedOrElse(NIGHTS, 0);
    }

    private static void checkMorning(MinecraftServer server) {
        boolean morning = server.overworld().dimensionType().defaultClock()
            .map(clock -> server.clockManager().isAtTimeMarker(clock, ClockTimeMarkers.WAKE_UP_FROM_SLEEP))
            .orElse(false);
        if (morning) {
            server.getPlayerList().getPlayers().stream().filter(ServerPlayer::isAlive).forEach(NightsSurvived::tally);
        }
    }

    // Counts one night unless this night was already counted.
    private static void tally(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = player.getAttached(COUNTED_AT);
        if (last != null && now - last < SAME_NIGHT_TICKS) {
            return;
        }
        player.setAttached(COUNTED_AT, now);
        player.setAttached(NIGHTS, count(player) + 1);
    }
}
