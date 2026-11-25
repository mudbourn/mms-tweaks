package info.mudbourn.mmstweaks.command.state;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.Targets;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public final class StateCommands {

    private static final int MAX_NICK = 32;

    private StateCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Targets.withTarget(
            Commands.literal("fly").requires(Tiers.atLeast(Tier.MODERATOR)),
            StateCommands::fly
        ));
        dispatcher.register(Commands.literal("flyspeed")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .then(Targets.withTarget(
                Commands.argument("speed", FloatArgumentType.floatArg(0, 5)),
                StateCommands::flySpeed
            )));
        dispatcher.register(Targets.withTarget(
            Commands.literal("god").requires(Tiers.atLeast(Tier.MODERATOR)),
            StateCommands::god
        ));
        dispatcher.register(Targets.withTarget(
            Commands.literal("heal").requires(Tiers.atLeast(Tier.MODERATOR)),
            StateCommands::heal
        ));
        dispatcher.register(Targets.withTarget(
            Commands.literal("feed").requires(Tiers.atLeast(Tier.MODERATOR)),
            StateCommands::feed
        ));
        dispatcher.register(Targets.withTarget(
            Commands.literal("extinguish").requires(Tiers.atLeast(Tier.MODERATOR)),
            StateCommands::extinguish
        ));
        dispatcher.register(Commands.literal("gm")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .then(Targets.withTarget(Commands.argument("mode", GameModeArgument.gameMode()), StateCommands::gameMode)));
        dispatcher.register(Commands.literal("afk")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(ctx -> {
                Afk.toggle(ctx.getSource().getPlayerOrException());
                return 1;
            }));
        dispatcher.register(Commands.literal("nick")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .then(Targets.withTarget(Commands.literal("clear"), StateCommands::clearNick))
            .then(Targets.withTarget(Commands.argument("name", StringArgumentType.string()), StateCommands::setNick)));
    }

    private static int fly(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        TweaksData data = TweaksData.get();
        String uuid = target.getUUID().toString();
        boolean on = data.fly.add(uuid);
        Abilities abilities = target.getAbilities();
        if (on) {
            abilities.mayfly = true;
        } else {
            data.fly.remove(uuid);
            abilities.mayfly = target.isCreative() || target.isSpectator();
            if (!abilities.mayfly) {
                abilities.flying = false;
            }
        }
        data.save();
        target.onUpdateAbilities();
        Msg.ok(ctx, "Fly " + (on ? "on" : "off") + " for " + target.getName().getString());
        return 1;
    }

    private static int flySpeed(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        float speed = FloatArgumentType.getFloat(ctx, "speed");
        TweaksData data = TweaksData.get();
        data.flySpeeds.put(target.getUUID().toString(), speed);
        data.save();
        target.getAbilities().setFlyingSpeed(speed * PlayerState.BASE_FLY_SPEED);
        target.onUpdateAbilities();
        Msg.ok(ctx, "Fly speed " + speed + " for " + target.getName().getString());
        return 1;
    }

    private static int god(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        TweaksData data = TweaksData.get();
        String uuid = target.getUUID().toString();
        boolean on = data.god.add(uuid);
        if (!on) {
            data.god.remove(uuid);
        }
        data.save();
        Msg.ok(ctx, "God mode " + (on ? "on" : "off") + " for " + target.getName().getString());
        return 1;
    }

    private static int heal(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        Msg.ok(ctx, "Healed " + target.getName().getString());
        return 1;
    }

    private static int feed(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        Msg.ok(ctx, "Fed " + target.getName().getString());
        return 1;
    }

    private static int extinguish(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        target.clearFire();
        Msg.ok(ctx, "Extinguished " + target.getName().getString());
        return 1;
    }

    private static int gameMode(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        GameType mode = GameModeArgument.getGameMode(ctx, "mode");
        target.setGameMode(mode);
        PlayerState.apply(target);
        Msg.ok(ctx, target.getName().getString() + " is now in " + mode.getName());
        return 1;
    }

    private static int setNick(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        if (name.isBlank() || name.length() > MAX_NICK) {
            throw Msg.fail("A nickname is 1 to " + MAX_NICK + " characters");
        }
        TweaksData data = TweaksData.get();
        data.nicks.put(target.getUUID().toString(), name);
        data.save();
        Names.refreshTab(target);
        Msg.ok(ctx, "Nickname set to " + name);
        return 1;
    }

    private static int clearNick(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        TweaksData data = TweaksData.get();
        data.nicks.remove(target.getUUID().toString());
        data.save();
        Names.refreshTab(target);
        Msg.ok(ctx, "Nickname cleared");
        return 1;
    }
}
