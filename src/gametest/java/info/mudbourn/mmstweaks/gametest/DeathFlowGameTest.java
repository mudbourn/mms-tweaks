package info.mudbourn.mmstweaks.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import goetic.mods.absolutrevive.common.damagesystem.PlayerDamageModel;
import goetic.mods.absolutrevive.common.util.CommonUtils;
import info.mudbourn.mmstweaks.client.revive.DeathPose;
import info.mudbourn.mmstweaks.revive.Downed;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goes down, self revives, then dies in a fresh world, logging and screenshotting each phase.
 */
public class DeathFlowGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks_gametest");

    private static final String CONTROLLER = "net.gamev.cinematic_respawn.client.RespawnCinematicController";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(60);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(
                player -> player.hurtServer(player.level(), player.damageSources().generic(), 1000.0F)
            ));
            for (int tick = 0; tick <= 60; tick++) {
                if (tick == 30) {
                    world.getServer().runCommand("tp @a ~6 ~ ~ 180 0");
                }
                if (tick >= 25) {
                    LOGGER.info("PROBE down t={} {}", tick, context.computeOnClient(DeathFlowGameTest::describe));
                }
                if (tick == 20) {
                    context.takeScreenshot("down-orbit");
                }
                context.waitTick();
            }
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(Downed::selfRevive));
            for (int tick = 0; tick <= 30; tick++) {
                LOGGER.info("PROBE up t={} {}", tick, context.computeOnClient(DeathFlowGameTest::describe));
                if (tick == 8 || tick == 14 || tick == 18) {
                    context.takeScreenshot("up-" + tick);
                }
                context.waitTick();
            }
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().forEach(
                player -> player.hurtServer(player.level(), player.damageSources().generic(), 1000.0F)
            ));
            for (int wait = 0; wait < 700; wait++) {
                boolean dead = context.computeOnClient(minecraft -> minecraft.player != null && minecraft.player.isDeadOrDying());
                if (dead) {
                    break;
                }
                if (wait >= 0) {
                    LOGGER.info("PROBE bleed w={} {} ar={}", wait, context.computeOnClient(DeathFlowGameTest::describe), context.computeOnClient(DeathFlowGameTest::arTicks));
                }
                context.waitTick();
            }
            for (int tick = 0; tick <= 340; tick++) {
                if (tick % 5 == 0 || tick >= 285 && tick <= 320) {
                    LOGGER.info("PROBE t={} {}", tick, context.computeOnClient(DeathFlowGameTest::describe));
                }
                if (tick % 40 == 0) {
                    context.takeScreenshot("death-" + tick);
                }
                context.waitTick();
            }
        }
    }

    private static String describe(Minecraft minecraft) {
        String screen = minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getSimpleName();
        String player = minecraft.player == null ? "null" : (minecraft.player.isDeadOrDying() ? "dead" : "alive") + "@" + System.identityHashCode(minecraft.player);
        var camera = minecraft.gameRenderer.mainCamera();
        String view = String.format("cam=%.1f/%.1f@%.1f,%.1f,%.1f", camera.yRot(), camera.xRot(), camera.position().x, camera.position().y, camera.position().z);
        String pose = minecraft.player == null ? "-" : String.valueOf(DeathPose.keepsDownedPose(minecraft.player));
        return "screen=" + screen + " player=" + player + " pose=" + pose + " " + view + " cr[" + crState() + "]";
    }

    private static String arTicks(Minecraft minecraft) {
        return CommonUtils.getExistingDamageModel(minecraft.player) instanceof PlayerDamageModel model
            ? model.getUnconsciousTicks() + (model.isUnconscious() ? "u" : "c")
            : "-";
    }

    private static String crState() {
        try {
            Class<?> controller = Class.forName(CONTROLLER);
            Class<?> screens = Class.forName("net.gamev.cinematic_respawn.client.RespawnScreens");
            Method fade = controller.getMethod("fadeAlpha", float.class);
            Method loading = screens.getMethod("isLoading");
            return "state=" + field(controller, "state")
                + " ticks=" + field(controller, "stateTicks")
                + " pending=" + field(controller, "wakeUpInitializationPending")
                + " synced=" + field(controller, "wakeUpPositionSynchronized")
                + " loading=" + loading.invoke(null)
                + " fade=" + fade.invoke(null, 0.0F)
                + " y=" + controller.getMethod("cameraYOffset", float.class).invoke(null, 0.0F);
        } catch (ReflectiveOperationException e) {
            return e.toString();
        }
    }

    private static Object field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
