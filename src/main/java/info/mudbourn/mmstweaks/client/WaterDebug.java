package info.mudbourn.mmstweaks.client;

import info.mudbourn.mmstweaks.water.WaterCurrents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

// /mmswater toggles an action bar readout of the water current under the player.
public final class WaterDebug {

    private static boolean shown;

    private WaterDebug() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
            ClientCommands.literal("mmswater").executes(command -> {
                shown = !shown;
                command.getSource().sendFeedback(Component.literal("Water current readout " + (shown ? "on" : "off")));
                return 1;
            })
        ));
        ClientTickEvents.END_CLIENT_TICK.register(WaterDebug::tick);
    }

    private static void tick(Minecraft client) {
        if (!shown || client.player == null || client.level == null) {
            return;
        }
        WaterCurrents.Reading reading = WaterCurrents.last();
        String text = client.level.getGameTime() - reading.tick() > 2
            ? "not in water"
            : String.format(
                Locale.ROOT,
                "flow %s  along %+.2f  ours %s  other %s  speed %.3f",
                vector(reading.flow()),
                reading.along(),
                vector(reading.applied()),
                vector(reading.otherPush()),
                client.player.getVehicle() != null
                    ? client.player.getVehicle().getDeltaMovement().horizontalDistance()
                    : client.player.getDeltaMovement().horizontalDistance()
            );
        client.player.sendOverlayMessage(Component.literal(text));
    }

    private static String vector(Vec3 vector) {
        return String.format(Locale.ROOT, "(%.3f, %.3f)", vector.x, vector.z);
    }
}
