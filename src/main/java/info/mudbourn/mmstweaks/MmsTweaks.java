package info.mudbourn.mmstweaks;

import info.mudbourn.mmstweaks.command.MmsCommands;
import info.mudbourn.mmstweaks.loops.DatapackLoops;
import info.mudbourn.mmstweaks.net.DownedStatus;
import info.mudbourn.mmstweaks.net.SelfReviveRequest;
import info.mudbourn.mmstweaks.revive.Downed;
import info.mudbourn.mmstweaks.revive.NightsSurvived;
import info.mudbourn.mmstweaks.revive.SelfRevives;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class MmsTweaks implements ModInitializer {

    public static final String MOD_ID = "mms_tweaks";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(SelfReviveRequest.TYPE, SelfReviveRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DownedStatus.TYPE, DownedStatus.CODEC);
        SelfRevives.register();
        NightsSurvived.register();
        DatapackLoops.register();
        info.mudbourn.mmstweaks.command.TestingCommands.register();
        MmsCommands.register();
        if (FabricLoader.getInstance().isModLoaded("absolutrevive")) {
            ServerPlayNetworking.registerGlobalReceiver(
                SelfReviveRequest.TYPE,
                (payload, context) -> Downed.selfRevive(context.player())
            );
        }
    }
}
