package info.mudbourn.mmstweaks.command.state;

import info.mudbourn.mmstweaks.command.data.TweaksData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.UUID;

public final class Names {

    private Names() {
    }

    public static Component nick(Player player) {
        String nick = TweaksData.get().nicks.get(player.getUUID().toString());
        return nick == null ? null : Component.literal(nick);
    }

    public static boolean hasNick(UUID uuid) {
        return TweaksData.get().nicks.containsKey(uuid.toString());
    }

    public static Component tab(ServerPlayer player, Component original) {
        boolean afk = Afk.isAfk(player);
        if (!afk && (original != null || !hasNick(player.getUUID()))) {
            return original;
        }
        Component name = original != null ? original : PlayerTeam.formatNameForTeam(player.getTeam(), player.getName());
        MutableComponent prefix = afk ? Component.literal("[AFK] ").withStyle(ChatFormatting.GRAY) : Component.empty();
        return prefix.append(name);
    }

    public static void refreshTab(ServerPlayer player) {
        player.level().getServer().getPlayerList().broadcastAll(
            new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player)
        );
    }
}
