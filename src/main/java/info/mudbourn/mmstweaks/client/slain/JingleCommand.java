package info.mudbourn.mmstweaks.client.slain;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Client command /mmsjingle that forces the next death's jingle and quip.
 */
public final class JingleCommand {

    private JingleCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("mmsjingle")
                .then(ClientCommands.argument("jingle", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(Jingles.ids(), builder))
                    .executes(context -> force(context, -1))
                    .then(ClientCommands.argument("quip", IntegerArgumentType.integer(0, Jingles.MESSAGES - 1))
                        .executes(context -> force(context, IntegerArgumentType.getInteger(context, "quip")))))
        ));
    }

    private static int force(CommandContext<FabricClientCommandSource> context, int quip) {
        String id = StringArgumentType.getString(context, "jingle");
        if (!Jingles.force(id, quip)) {
            context.getSource().sendError(Component.literal("Unknown jingle: " + id));
            return 0;
        }
        String which = quip < 0 ? "a random quip" : "quip " + quip;
        context.getSource().sendFeedback(Component.literal("Next death plays " + id + " with " + which + "."));
        return 1;
    }
}
