package info.mudbourn.mmstweaks.command.tier;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Predicate;

public final class Tiers {

    private Tiers() {
    }

    public static Predicate<CommandSourceStack> atLeast(Tier tier) {
        return source -> effective(source).atLeast(tier);
    }

    public static Tier effective(CommandSourceStack source) {
        if (source.getEntity() == null) {
            return Tier.OWNER;
        }
        Tier op = opTier(source.permissions());
        if (source.getEntity() instanceof ServerPlayer player) {
            return Tier.max(op, DiscordTiers.tier(player.getUUID()));
        }
        return op;
    }

    public static Tier effective(ServerPlayer player) {
        return Tier.max(opTier(player.permissions()), DiscordTiers.tier(player.getUUID()));
    }

    public static Tier opTier(PermissionSet permissions) {
        if (permissions.hasPermission(Permissions.COMMANDS_OWNER)) {
            return Tier.OWNER;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_ADMIN)) {
            return Tier.ADMIN;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return Tier.MODERATOR;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_MODERATOR)) {
            return Tier.TEMP_MOD;
        }
        return Tier.PLAYER;
    }
}
