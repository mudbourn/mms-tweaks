package info.mudbourn.mmstweaks.command;

import info.mudbourn.mmstweaks.command.admin.AdminCommands;
import info.mudbourn.mmstweaks.command.home.HomeCommands;
import info.mudbourn.mmstweaks.command.info.InfoCommands;
import info.mudbourn.mmstweaks.command.item.ItemCommands;
import info.mudbourn.mmstweaks.command.state.PlayerState;
import info.mudbourn.mmstweaks.command.state.StateCommands;
import info.mudbourn.mmstweaks.command.utility.UtilityCommands;
import info.mudbourn.mmstweaks.command.world.WorldCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class MmsCommands {

    private MmsCommands() {
    }

    public static void register() {
        PlayerState.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
            HomeCommands.register(dispatcher);
            UtilityCommands.register(dispatcher);
            InfoCommands.register(dispatcher);
            StateCommands.register(dispatcher);
            ItemCommands.register(dispatcher);
            WorldCommands.register(dispatcher);
            AdminCommands.register(dispatcher);
        });
    }
}
