package info.mudbourn.mmstweaks.command.utility;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Map;

public final class CosmeticSlots {

    private CosmeticSlots() {
    }

    // The Cosmetic Armor head slot, a Trinkets slot named cosmetic in the head group.
    public static Container head(ServerPlayer player) throws CommandSyntaxException {
        if (!FabricLoader.getInstance().isModLoaded("cosmeticarmor") || !FabricLoader.getInstance().isModLoaded("trinkets")) {
            throw Msg.fail("Cosmetic Armor is not loaded");
        }
        try {
            Object attachment = Class.forName("eu.pb4.trinkets.api.TrinketsApi")
                .getMethod("getAttachment", LivingEntity.class)
                .invoke(null, player);
            Method inventories = Class.forName("eu.pb4.trinkets.api.TrinketAttachment").getMethod("getInventory");
            Map<?, ?> groups = (Map<?, ?>) inventories.invoke(attachment);
            Map<?, ?> head = (Map<?, ?>) groups.get("head");
            Object slot = head == null ? null : head.get("cosmetic");
            if (slot instanceof Container container && container.getContainerSize() > 0) {
                return container;
            }
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException e) {
            throw Msg.fail("Cosmetic Armor slots are unavailable");
        }
        throw Msg.fail("No cosmetic head slot found");
    }
}
