package info.mudbourn.mmstweaks.command.utility;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

final class ViewMenu extends ChestMenu {

    ViewMenu(int id, Inventory inventory, Container container) {
        super(MenuType.GENERIC_9x6, id, inventory, container, 6);
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
