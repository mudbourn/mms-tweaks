package info.mudbourn.mmstweaks.command.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.config.CommandConfig;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

public final class UtilityCommands {

    private UtilityCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[] {"enderchest", "ec", "echest"}) {
            dispatcher.register(enderChest(name));
        }
        dispatcher.register(menu("workbench", UtilityCommands::workbench));
        dispatcher.register(menu("stonecutter", UtilityCommands::stonecutter));
        dispatcher.register(menu("grindstone", UtilityCommands::grindstone));
        dispatcher.register(menu("trash", UtilityCommands::trash));
        dispatcher.register(Commands.literal("rules")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(UtilityCommands::rules)
            .then(Commands.literal("reload")
                .requires(Tiers.atLeast(Tier.ADMIN))
                .executes(UtilityCommands::reloadRules)));
        dispatcher.register(Commands.literal("suicide")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(UtilityCommands::suicide));
        dispatcher.register(Commands.literal("gametime")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(UtilityCommands::gameTime));
        dispatcher.register(Commands.literal("hat")
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(UtilityCommands::hat));
        dispatcher.register(Commands.literal("invsee")
            .requires(Tiers.atLeast(Tier.TEMP_MOD))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(UtilityCommands::invsee)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> enderChest(String name) {
        return Commands.literal(name)
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(ctx -> openEnderChest(ctx, ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(Tiers.atLeast(Tier.MODERATOR))
                .executes(ctx -> openEnderChest(ctx, EntityArgument.getPlayer(ctx, "player"))));
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> ctx, ServerPlayer owner) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        viewer.openMenu(new SimpleMenuProvider(
            (id, inventory, player) -> enderMenu(id, inventory, owner, viewer),
            Component.translatable("container.enderchest")
        ));
        return 1;
    }

    private static ChestMenu enderMenu(int id, Inventory inventory, ServerPlayer owner, ServerPlayer viewer) {
        if (owner == viewer) {
            return ChestMenu.threeRows(id, inventory, owner.getEnderChestInventory());
        }
        return new ChestMenu(MenuType.GENERIC_9x3, id, inventory, owner.getEnderChestInventory(), 3) {
            @Override
            public boolean stillValid(Player user) {
                return !owner.hasDisconnected() && owner.isAlive() && super.stillValid(user);
            }
        };
    }

    private interface MenuFactory {
        MenuProvider create(ServerPlayer player);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> menu(String name, MenuFactory factory) {
        return Commands.literal(name)
            .requires(Tiers.atLeast(Tier.PLAYER))
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                player.openMenu(factory.create(player));
                return 1;
            });
    }

    private static ContainerLevelAccess access(Player player) {
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }

    private static MenuProvider workbench(ServerPlayer owner) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new CraftingMenu(id, inventory, access(player)) {
                @Override
                public boolean stillValid(Player user) {
                    return true;
                }
            },
            Component.translatable("container.crafting")
        );
    }

    private static MenuProvider stonecutter(ServerPlayer owner) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new StonecutterMenu(id, inventory, access(player)) {
                @Override
                public boolean stillValid(Player user) {
                    return true;
                }
            },
            Component.translatable("container.stonecutter")
        );
    }

    private static MenuProvider grindstone(ServerPlayer owner) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new GrindstoneMenu(id, inventory, access(player)) {
                @Override
                public boolean stillValid(Player user) {
                    return true;
                }
            },
            Component.translatable("container.grindstone_title")
        );
    }

    private static MenuProvider trash(ServerPlayer owner) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> ChestMenu.threeRows(id, inventory, new SimpleContainer(27)),
            Component.literal("Trash")
        );
    }

    private static int rules(CommandContext<CommandSourceStack> ctx) {
        for (String line : CommandConfig.get().rules) {
            Msg.ok(ctx, line);
        }
        return 1;
    }

    private static int reloadRules(CommandContext<CommandSourceStack> ctx) {
        CommandConfig.load(ctx.getSource().getServer());
        Msg.ok(ctx, "Rules reloaded");
        return 1;
    }

    private static int suicide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.kill(player.level());
        return 1;
    }

    private static int gameTime(CommandContext<CommandSourceStack> ctx) {
        long time = ctx.getSource().getServer().overworld().getOverworldClockTime();
        long day = time / 24000 + 1;
        long ofDay = time % 24000;
        long hours = (ofDay / 1000 + 6) % 24;
        long minutes = (ofDay % 1000) * 60 / 1000;
        Msg.ok(ctx, String.format("Day %d, %02d:%02d", day, hours, minutes));
        return 1;
    }

    private static int hat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Container slot = CosmeticSlots.head(player);
        ItemStack held = player.getMainHandItem().copy();
        ItemStack worn = slot.getItem(0).copy();
        slot.setItem(0, held);
        player.setItemSlot(EquipmentSlot.MAINHAND, worn);
        return 1;
    }

    private static int invsee(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        SimpleContainer snapshot = new SimpleContainer(54);
        for (int i = 0; i < Math.min(54, target.getInventory().getContainerSize()); i++) {
            snapshot.setItem(i, target.getInventory().getItem(i).copy());
        }
        viewer.openMenu(new SimpleMenuProvider(
            (id, inventory, player) -> new ViewMenu(id, inventory, snapshot),
            target.getName()
        ));
        return 1;
    }
}
