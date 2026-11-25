package info.mudbourn.mmstweaks.command.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.Targets;
import info.mudbourn.mmstweaks.command.data.Spot;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import info.mudbourn.mmstweaks.command.move.Teleports;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class HomeCommands {

    private HomeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(HomeCommands::go)
            .then(Commands.literal("set")
                .executes(HomeCommands::set))
            .then(Commands.argument("owner", StringArgumentType.word())
                .requires(Tiers.atLeast(Tier.MODERATOR))
                .suggests(Targets.KNOWN_NAMES)
                .executes(HomeCommands::visit)));
    }

    private static int go(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Spot home = TweaksData.get().homes.get(player.getUUID().toString());
        if (home == null) {
            throw Msg.fail("You have no home. Use /home set");
        }
        Teleports.go(player, home);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TweaksData data = TweaksData.get();
        data.homes.put(player.getUUID().toString(), Spot.of(player));
        data.save();
        Msg.ok(ctx, "Home set");
        return 1;
    }

    private static int visit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String uuid = Targets.knownUuid(ctx, "owner");
        Spot home = TweaksData.get().homes.get(uuid);
        if (home == null) {
            throw Msg.fail("That player has no home");
        }
        Teleports.go(player, home);
        return 1;
    }
}
