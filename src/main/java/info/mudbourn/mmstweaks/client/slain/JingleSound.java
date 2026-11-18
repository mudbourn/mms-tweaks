package info.mudbourn.mmstweaks.client.slain;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * A non-positional jingle on the master channel that can fade itself out.
 */
final class JingleSound extends AbstractSoundInstance implements TickableSoundInstance {

    private int fadeTicks;

    private int fadeLeft;

    private float fadeFrom;

    private boolean stopped;

    JingleSound(Identifier sound) {
        super(sound, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 1.0F;
    }

    void fadeOut(int ticks) {
        if (stopped || (fadeTicks > 0 && fadeLeft <= ticks)) {
            return;
        }
        if (ticks <= 0) {
            volume = 0.0F;
            stopped = true;
            return;
        }
        fadeTicks = ticks;
        fadeLeft = ticks;
        fadeFrom = volume;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        if (fadeTicks <= 0 || stopped) {
            return;
        }
        fadeLeft--;
        volume = fadeFrom * Math.max(0, fadeLeft) / fadeTicks;
        if (fadeLeft <= 0) {
            stopped = true;
        }
    }
}
