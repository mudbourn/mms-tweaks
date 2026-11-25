package info.mudbourn.mmstweaks.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.config.CommandConfig;
import info.mudbourn.mmstweaks.command.tier.DiscordTiers;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class AdminCommands {

    private AdminCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mmstier")
            .requires(Tiers.atLeast(Tier.ADMIN))
            .executes(ctx -> show(ctx, ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("reload")
                .executes(AdminCommands::reload))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> show(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int show(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        DiscordTiers.Reading reading = DiscordTiers.reading(target.getUUID());
        Tier op = Tiers.opTier(target.permissions());
        Msg.ok(ctx, target.getName().getString() + ": " + Tiers.effective(target)
            + " (op " + op + ", discord " + reading.tier() + " from " + reading.source() + ")");
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandConfig.load(ctx.getSource().getServer());
        DiscordTiers.refreshAll(ctx.getSource().getServer());
        Msg.ok(ctx, "Config reloaded, tiers refreshing");
        return 1;
    }
}
