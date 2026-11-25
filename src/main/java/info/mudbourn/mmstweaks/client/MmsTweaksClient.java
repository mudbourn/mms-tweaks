package info.mudbourn.mmstweaks.client;

import info.mudbourn.mmstweaks.client.camera.CameraGlue;
import info.mudbourn.mmstweaks.client.revive.SelfReviveHold;
import info.mudbourn.mmstweaks.client.slain.JingleCommand;
import info.mudbourn.mmstweaks.client.slain.SlainFlow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MmsTweaksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        SlainFlow.register();
        JingleCommand.register();
        WaterDebug.register();
        if (loader.isModLoaded("absolutrevive")) {
            SelfReviveHold.register();
        }
        if (loader.isModLoaded("leawind_third_person")
                && loader.isModLoaded("perspective_api")
                && loader.isModLoaded("cinematic_respawn")) {
            CameraGlue.register();
        }
    }
}
