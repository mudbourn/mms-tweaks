package info.mudbourn.mmstweaks.command.state;

import info.mudbourn.mmstweaks.command.config.CommandConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Afk {

    private record Seen(double x, double y, double z, float yaw, float pitch) {
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();
    private static final Set<UUID> AFK = new HashSet<>();

    private Afk() {
    }

    public static boolean isAfk(ServerPlayer player) {
        return AFK.contains(player.getUUID());
    }

    public static void toggle(ServerPlayer player) {
        if (isAfk(player)) {
            active(player);
        } else {
            set(player);
        }
    }

    public static void active(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), player.level().getServer().getTickCount());
        if (AFK.remove(player.getUUID())) {
            announce(player, " is no longer AFK");
        }
    }

    public static void forget(UUID uuid) {
        SEEN.remove(uuid);
        ACTIVE.remove(uuid);
        AFK.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        int limit = CommandConfig.get().afk_minutes * 1200;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Seen current = new Seen(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            Seen previous = SEEN.put(player.getUUID(), current);
            if (previous == null || !previous.equals(current)) {
                active(player);
            } else if (!isAfk(player) && now - ACTIVE.getOrDefault(player.getUUID(), now) >= limit) {
                set(player);
            }
        }
    }

    private static void set(ServerPlayer player) {
        AFK.add(player.getUUID());
        announce(player, " is now AFK");
    }

    private static void announce(ServerPlayer player, String text) {
        Names.refreshTab(player);
        player.level().getServer().getPlayerList().broadcastSystemMessage(
            Component.empty().append(player.getName()).append(text).withStyle(ChatFormatting.GRAY),
            false
        );
    }
}
