package info.mudbourn.mmstweaks.command.move;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.data.Spot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public final class Teleports {

    private Teleports() {
    }

    public static void go(ServerPlayer player, Spot destination) throws CommandSyntaxException {
        ServerLevel level = destination.level(player.level().getServer());
        if (level == null) {
            throw Msg.fail("That dimension is not loaded");
        }
        player.teleportTo(level, destination.x(), destination.y(), destination.z(), Set.of(), destination.yaw(), destination.pitch(), true);
    }
}
