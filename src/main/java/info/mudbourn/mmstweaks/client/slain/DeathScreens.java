package info.mudbourn.mmstweaks.client.slain;

import info.mudbourn.mmstweaks.mixin.DeathScreenAccessor;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Swaps the vanilla, You Were Slain and Third Person Death death screens for the game over screen.
 */
public final class DeathScreens {

    private static final String BEDROCK_DEATH_SCREEN = "cintlex.thirdpersondeath.BedrockDeathScreen";

    private DeathScreens() {}

    public static Screen replace(Screen screen) {
        if (screen == null || screen instanceof SlainScreen || Minecraft.getInstance().player == null) {
            return screen;
        }
        if (screen instanceof DeathScreen death) {
            DeathScreenAccessor access = (DeathScreenAccessor) death;
            return SlainScreen.open(access.mmsTweaks$causeOfDeath(), access.mmsTweaks$hardcore());
        }
        if (screen.getClass().getName().equals(BEDROCK_DEATH_SCREEN)) {
            return SlainScreen.open(field(screen, "causeOfDeath", Component.class), Boolean.TRUE.equals(field(screen, "hardcore", Boolean.class)));
        }
        return screen;
    }

    private static <T> T field(Screen screen, String name, Class<T> type) {
        try {
            Field field = screen.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(screen);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
