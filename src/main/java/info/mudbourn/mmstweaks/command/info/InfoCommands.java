package info.mudbourn.mmstweaks.command.info;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.Targets;
import info.mudbourn.mmstweaks.command.data.Spot;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import info.mudbourn.mmstweaks.command.move.Surface;
import info.mudbourn.mmstweaks.command.move.Teleports;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class InfoCommands {

    private static final int MAX_NEAR = 200;
    private static final int DEFAULT_NEAR = 100;

    private InfoCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deathpos")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(ctx -> showDeath(ctx, ctx.getSource().getPlayerOrException().getUUID().toString()))
            .then(Commands.literal("tp")
                .requires(Tiers.atLeast(Tier.MODERATOR))
                .executes(ctx -> teleportToDeath(ctx, ctx.getSource().getPlayerOrException().getUUID().toString()))
                .then(Commands.argument("owner", StringArgumentType.word())
                    .suggests(Targets.KNOWN_NAMES)
                    .executes(ctx -> teleportToDeath(ctx, Targets.knownUuid(ctx, "owner")))))
            .then(Commands.argument("owner", StringArgumentType.word())
                .requires(Tiers.atLeast(Tier.TEMP_MOD))
                .suggests(Targets.KNOWN_NAMES)
                .executes(ctx -> showDeath(ctx, Targets.knownUuid(ctx, "owner")))));
        dispatcher.register(Commands.literal("coords")
            .requires(Tiers.atLeast(Tier.TEMP_MOD))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> showCoords(ctx, EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("lastpos")
            .requires(Tiers.atLeast(Tier.TEMP_MOD))
            .then(Commands.argument("owner", StringArgumentType.word())
                .suggests(Targets.KNOWN_NAMES)
                .executes(InfoCommands::lastPos)));
        dispatcher.register(Commands.literal("near")
            .requires(Tiers.atLeast(Tier.TEMP_MOD))
            .executes(ctx -> near(ctx, DEFAULT_NEAR))
            .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_NEAR))
                .executes(ctx -> near(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
        dispatcher.register(Commands.literal("top")
            .requires(Tiers.atLeast(Tier.TEMP_MOD))
            .executes(InfoCommands::top));
    }

    private static int showDeath(CommandContext<CommandSourceStack> ctx, String uuid) throws CommandSyntaxException {
        Spot spot = TweaksData.get().deaths.get(uuid);
        if (spot == null) {
            throw Msg.fail("No death position recorded");
        }
        Msg.ok(ctx, "Last death: " + spot.describe());
        return 1;
    }

    private static int teleportToDeath(CommandContext<CommandSourceStack> ctx, String uuid) throws CommandSyntaxException {
        Spot spot = TweaksData.get().deaths.get(uuid);
        if (spot == null) {
            throw Msg.fail("No death position recorded");
        }
        Teleports.go(ctx.getSource().getPlayerOrException(), spot);
        return 1;
    }

    private static int showCoords(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        Msg.ok(ctx, target.getName().getString() + ": " + Spot.of(target).describe());
        return 1;
    }

    private static int lastPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Spot spot = TweaksData.get().logouts.get(Targets.knownUuid(ctx, "owner"));
        if (spot == null) {
            throw Msg.fail("No logout position recorded");
        }
        Msg.ok(ctx, "Last seen: " + spot.describe());
        return 1;
    }

    private static int near(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        List<String> found = new ArrayList<>();
        for (ServerPlayer other : self.level().players()) {
            double distance = other.distanceTo(self);
            if (other != self && distance <= radius) {
                found.add(other.getName().getString() + " (" + Math.round(distance) + " m)");
            }
        }
        Msg.ok(ctx, found.isEmpty() ? "No players within " + radius + " blocks" : "Nearby: " + String.join(", ", found));
        return found.size();
    }

    private static int top(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Integer y = Surface.standingY(player.level(), player.getBlockX(), player.getBlockZ());
        if (y == null) {
            throw Msg.fail("No safe surface here");
        }
        Teleports.go(player, new Spot(
            player.level().dimension().identifier().toString(),
            player.getX(),
            y,
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        ));
        return 1;
    }
}
