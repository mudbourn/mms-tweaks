package info.mudbourn.mmstweaks.client.slain;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmstweaks.revive.NightsSurvived;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The game over screen that takes the place of every death screen.
 */
public final class SlainScreen extends Screen {

    private static final int AUTO_RESPAWN_TICKS = 300;

    private static final int TEXT_FADE_IN_TICKS = 20;

    private static final int BLACK_FADE_TICKS = 30;

    private static final int NOTICE_TICKS = 60;

    private static final int RESPAWN_RETRY_TICKS = 100;

    private static final int HOLD_SKIP_TICKS = 10;

    private static SlainScreen current;

    private final LocalPlayer owner;

    private final SlainCard card;

    private final SlainAudio audio;

    private int ticks;

    private float black;

    private float lastBlack;

    private boolean blackFading;

    private int spaceHeldTicks;

    private boolean started;

    private int respawnRequestedAt = -1;

    private int noticeTicks;

    private SlainScreen(LocalPlayer owner, Component cause, boolean hardcore) {
        super(Component.translatable(hardcore ? "deathScreen.title.hardcore" : "deathScreen.title"));
        RandomSource random = RandomSource.create();
        Jingles.Jingle jingle = Jingles.pick(random);
        this.owner = owner;
        this.audio = new SlainAudio(jingle);
        this.card = new SlainCard(jingle.headline(random, NightsSurvived.count(owner)), cause, hardcore, owner.blockPosition(), owner.level().dimension().identifier());
    }

    // Returns the live game over screen for this death, making one on the first call.
    public static Screen open(Component cause, boolean hardcore) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (current != null && current.owner == player) {
            return current;
        }
        current = new SlainScreen(player, cause, hardcore);
        return current;
    }

    static void clear() {
        if (current != null) {
            current.audio.release(Minecraft.getInstance());
            current = null;
        }
    }

    @Override
    protected void init() {
        if (started) {
            return;
        }
        started = true;
        SlainFlow.onSlain(minecraft);
        audio.start(minecraft);
    }

    @Override
    public void tick() {
        ticks++;
        audio.tick(minecraft);
        spaceHeldTicks = InputConstants.isKeyDown(InputConstants.KEY_SPACE) ? spaceHeldTicks + 1 : 0;
        if (spaceHeldTicks >= HOLD_SKIP_TICKS) {
            requestRespawn();
        }
        if (noticeTicks > 0) {
            noticeTicks--;
        }

        lastBlack = black;
        if (!blackFading && !DeathPan.active() && audio.quiet(minecraft)) {
            blackFading = true;
        }
        if (blackFading) {
            black = Math.min(1.0F, black + 1.0F / BLACK_FADE_TICKS);
        }

        if (respawnRequestedAt < 0) {
            if (ticks >= AUTO_RESPAWN_TICKS) {
                requestRespawn();
            }
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player != null && player != owner) {
            handOff();
        } else if (ticks - respawnRequestedAt >= RESPAWN_RETRY_TICKS && player != null) {
            respawnRequestedAt = ticks;
            player.respawn();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        float shade = Mth.lerp(partial, lastBlack, black);
        SlainFlow.drawBlack(graphics, width, height, shade * shade * (3.0F - 2.0F * shade));
        DeathTint.render(graphics, width, height, ticks + partial, 1.0F);

        float text = Mth.clamp((ticks + partial) / TEXT_FADE_IN_TICKS, 0.0F, 1.0F);
        float notice = Mth.clamp((noticeTicks - partial) / 15.0F, 0.0F, 1.0F);
        card.draw(
            graphics,
            font,
            width,
            height,
            text,
            timerLine(),
            hintLine(),
            card.overCoords(font, width, height, mouseX, mouseY),
            notice
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            requestRespawn();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (card.overCoords(font, width, height, event.x(), event.y())) {
            minecraft.gui.hud.getChat().addClientSystemMessage(card.chatLine());
            noticeTicks = NOTICE_TICKS;
            SlainFlow.onCoordsCopied();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void requestRespawn() {
        if (respawnRequestedAt >= 0) {
            return;
        }
        respawnRequestedAt = ticks;
        boolean held = InputConstants.isKeyDown(InputConstants.KEY_SPACE);
        audio.fadeOut(held ? 4 : 15);
        SlainFlow.onRespawnRequested();
        owner.respawn();
    }

    private void handOff() {
        current = null;
        SlainFlow.begin(new SlainAftermath(card, audio, black, ticks));
        minecraft.gui.setScreen(null);
    }

    private Component timerLine() {
        if (respawnRequestedAt >= 0) {
            return Component.translatable(card.hardcore ? "mms_tweaks.slain.spectating" : "mms_tweaks.slain.respawning");
        }
        int seconds = Math.max(1, Mth.ceil((AUTO_RESPAWN_TICKS - ticks) / 20.0F));
        return Component.translatable(card.hardcore ? "mms_tweaks.slain.spectate_in" : "mms_tweaks.slain.respawn_in", seconds);
    }

    private Component hintLine() {
        if (respawnRequestedAt >= 0) {
            return null;
        }
        return Component.translatable(card.hardcore ? "mms_tweaks.slain.spectate_hint" : "mms_tweaks.slain.respawn_hint");
    }
}
