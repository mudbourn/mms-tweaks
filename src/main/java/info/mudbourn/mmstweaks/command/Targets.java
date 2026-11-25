package info.mudbourn.mmstweaks.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class Targets {

    public static final SuggestionProvider<CommandSourceStack> KNOWN_NAMES =
        (ctx, builder) -> SharedSuggestionProvider.suggest(TweaksData.get().names.values(), builder);

    @FunctionalInterface
    public interface PlayerAction {
        int run(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException;
    }

    private Targets() {
    }

    // Runs on the sender, or on an optional trailing player argument.
    public static <B extends ArgumentBuilder<CommandSourceStack, B>> B withTarget(B node, PlayerAction action) {
        return node
            .executes(ctx -> action.run(ctx, ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> action.run(ctx, EntityArgument.getPlayer(ctx, "player"))));
    }

    public static String knownUuid(CommandContext<CommandSourceStack> ctx, String argument) throws CommandSyntaxException {
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, argument);
        String uuid = TweaksData.get().uuidOf(name);
        if (uuid == null) {
            throw Msg.fail("Unknown player " + name);
        }
        return uuid;
    }
}
