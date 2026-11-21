package info.mudbourn.mmstweaks.log;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

/**
 * Fabric creates language adapters while it sets up mods, before Mixin reads any mixin config.
 * Creating this one installs the log silencer early enough to catch refmap warnings and failed
 * companion plugins. It is never used to create entrypoints.
 */
public final class SilencerLanguageAdapter implements LanguageAdapter {

    public SilencerLanguageAdapter() {
        LogSilencer.install();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        throw new LanguageAdapterException("mms_tweaks_silencer is not an entrypoint adapter");
    }
}
