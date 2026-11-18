package info.mudbourn.mmstweaks.client.slain;

import info.mudbourn.mmstweaks.mixin.MusicManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

/**
 * Ducks the game music quickly, then plays the jingle and tracks how long it has played.
 */
final class SlainAudio {

    private static final int DUCK_TICKS = 8;

    private final Jingles.Jingle jingle;

    private JingleSound sound;

    private int duckTick = -1;

    private float duckFrom;

    private int playedTicks = -1;

    private boolean released;

    SlainAudio(Jingles.Jingle jingle) {
        this.jingle = jingle;
    }

    void start(Minecraft minecraft) {
        SlainFlow.holdMusic(true);
        MusicManagerAccessor music = (MusicManagerAccessor) minecraft.getMusicManager();
        SoundInstance current = music.mmsTweaks$currentMusic();
        if (current != null && minecraft.getSoundManager().isActive(current)) {
            duckTick = 0;
            duckFrom = music.mmsTweaks$currentGain();
        } else {
            play(minecraft);
        }
    }

    void tick(Minecraft minecraft) {
        if (released) {
            return;
        }
        if (duckTick >= 0) {
            duckTick++;
            float gain = duckFrom * Math.max(0.0F, 1.0F - duckTick / (float) DUCK_TICKS);
            minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, gain);
            if (duckTick >= DUCK_TICKS) {
                duckTick = -1;
                minecraft.getMusicManager().stopPlaying();
                play(minecraft);
            }
            return;
        }
        if (playedTicks >= 0) {
            playedTicks++;
        }
    }

    // True once the jingle's own tail has gone quiet or the jingle was cut.
    boolean quiet(Minecraft minecraft) {
        if (playedTicks < 0) {
            return false;
        }
        boolean done = sound.isStopped() || !minecraft.getSoundManager().isActive(sound);
        return done && playedTicks > 2 || playedTicks / 20.0F >= jingle.tailSeconds();
    }

    void fadeOut(int ticks) {
        duckTick = -1;
        if (sound != null) {
            sound.fadeOut(ticks);
        }
    }

    void release(Minecraft minecraft) {
        if (released) {
            return;
        }
        released = true;
        SoundManager sounds = minecraft.getSoundManager();
        if (sound != null && !sound.isStopped()) {
            sounds.stop(sound);
        }
        MusicManager music = minecraft.getMusicManager();
        sounds.updateCategoryVolume(SoundSource.MUSIC, ((MusicManagerAccessor) music).mmsTweaks$currentGain());
        SlainFlow.holdMusic(false);
    }

    private void play(Minecraft minecraft) {
        sound = new JingleSound(jingle.sound());
        minecraft.getSoundManager().play(sound);
        playedTicks = 0;
    }
}
