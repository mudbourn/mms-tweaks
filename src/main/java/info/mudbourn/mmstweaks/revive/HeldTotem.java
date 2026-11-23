package info.mudbourn.mmstweaks.revive;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Finds a death-protection item a player could still use, in either hand or any Trinkets slot.
 */
public final class HeldTotem {

    private static final Method GET_COMPONENT = trinketsLookup();

    private HeldTotem() {}

    // True when the vanilla totem check would save this player.
    public static boolean has(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (protects(player.getItemInHand(hand))) {
                return true;
            }
        }
        return GET_COMPONENT != null && inTrinkets(player);
    }

    private static boolean inTrinkets(Player player) {
        try {
            Object trinkets = GET_COMPONENT.invoke(null, player);
            if (trinkets == null) {
                return false;
            }
            Method isEquipped = Class.forName("eu.pb4.trinkets.api.TrinketAttachment").getMethod("isEquipped", Predicate.class);
            Predicate<ItemStack> test = HeldTotem::protects;
            return (Boolean) isEquipped.invoke(trinkets, test);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return false;
        }
    }

    private static Method trinketsLookup() {
        if (!FabricLoader.getInstance().isModLoaded("trinkets")) {
            return null;
        }
        try {
            // Trinkets Updated (provides "trinkets"); the old dev.emi API is not present on 26.3.
            return Class.forName("eu.pb4.trinkets.api.TrinketsApi").getMethod("getAttachment", LivingEntity.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean protects(ItemStack stack) {
        return stack.has(DataComponents.DEATH_PROTECTION);
    }
}
