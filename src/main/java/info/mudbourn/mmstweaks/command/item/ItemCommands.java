package info.mudbourn.mmstweaks.command.item;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.Targets;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class ItemCommands {

    private static final int MAX_KILL_RADIUS = 256;
    private static final int DEFAULT_KILL_RADIUS = 64;

    private ItemCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repair")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(ctx -> repair(ctx, ctx.getSource().getPlayerOrException(), false))
            .then(Commands.literal("all")
                .executes(ctx -> repair(ctx, ctx.getSource().getPlayerOrException(), true)))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> repair(ctx, EntityArgument.getPlayer(ctx, "player"), false))
                .then(Commands.literal("all")
                    .executes(ctx -> repair(ctx, EntityArgument.getPlayer(ctx, "player"), true)))));
        dispatcher.register(Commands.literal("smelt")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(ItemCommands::smelt));
        dispatcher.register(Targets.withTarget(
            Commands.literal("disenchant").requires(Tiers.atLeast(Tier.MODERATOR)),
            ItemCommands::disenchant
        ));
        dispatcher.register(Commands.literal("rename")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(ItemCommands::rename)));
        dispatcher.register(Commands.literal("killitems")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(ctx -> killItems(ctx, DEFAULT_KILL_RADIUS))
            .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_KILL_RADIUS))
                .executes(ctx -> killItems(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    private static int repair(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean all) throws CommandSyntaxException {
        int fixed = 0;
        if (all) {
            for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
                fixed += mend(target.getInventory().getItem(i));
            }
        } else {
            fixed = mend(target.getMainHandItem());
        }
        if (fixed == 0) {
            throw Msg.fail("Nothing to repair");
        }
        Msg.ok(ctx, "Repaired " + fixed + " item" + (fixed == 1 ? "" : "s"));
        return fixed;
    }

    private static int mend(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() == 0) {
            return 0;
        }
        stack.setDamageValue(0);
        return 1;
    }

    private static int smelt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            throw Msg.fail("Hold the items to smelt");
        }
        ServerLevel level = player.level();
        SingleRecipeInput input = new SingleRecipeInput(held);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.SMELTING,
            input,
            level
        );
        if (recipe.isEmpty()) {
            throw Msg.fail("That item cannot be smelted");
        }
        ItemStack result = recipe.get().value().assemble(input);
        int total = held.getCount() * result.getCount();
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        while (total > 0) {
            ItemStack part = result.copyWithCount(Math.min(total, result.getMaxStackSize()));
            total -= part.getCount();
            if (!player.getInventory().add(part)) {
                player.drop(part, false, Prediction.SERVER_ONLY);
            }
        }
        Msg.ok(ctx, "Smelted");
        return 1;
    }

    private static int disenchant(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ItemStack held = target.getMainHandItem();
        if (held.isEmpty()) {
            throw Msg.fail("Nothing in the main hand");
        }
        held.remove(DataComponents.ENCHANTMENTS);
        held.remove(DataComponents.STORED_ENCHANTMENTS);
        Msg.ok(ctx, "Enchantments removed");
        return 1;
    }

    private static int rename(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack held = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (held.isEmpty()) {
            throw Msg.fail("Nothing in the main hand");
        }
        held.set(DataComponents.CUSTOM_NAME, Component.literal(StringArgumentType.getString(ctx, "name")));
        Msg.ok(ctx, "Renamed");
        return 1;
    }

    private static int killItems(CommandContext<CommandSourceStack> ctx, int radius) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 origin = ctx.getSource().getPosition();
        List<? extends ItemEntity> items = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            item -> item.position().distanceToSqr(origin) <= (double) radius * radius
        );
        items.forEach(ItemEntity::discard);
        Msg.ok(ctx, "Removed " + items.size() + " item entities");
        return items.size();
    }
}
