package info.mudbourn.mmstweaks.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class Msg {

    private Msg() {
    }

    public static CommandSyntaxException fail(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    public static void ok(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
