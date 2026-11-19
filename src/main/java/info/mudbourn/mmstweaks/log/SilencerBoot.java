package info.mudbourn.mmstweaks.log;

import info.mudbourn.mmstweaks.ModMixinGate;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Starts the log silencer as early as a mod can: when Mixin loads this (empty) mixin config,
 * which is before most mixin warnings. The preLaunch entrypoint is a fallback.
 */
public final class SilencerBoot extends ModMixinGate implements PreLaunchEntrypoint {

    public SilencerBoot() {
        super();
    }

    @Override
    public void onLoad(String mixinPackage) {
        LogSilencer.install();
    }

    @Override
    public void onPreLaunch() {
        LogSilencer.install();
    }
}
