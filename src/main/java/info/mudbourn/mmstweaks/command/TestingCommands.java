package info.mudbourn.mmstweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// Testing shortcuts layered onto vanilla /tp and /summon. Brigadier merges these nodes
// into the vanilla ones, so every vanilla form still works.
//
//   /tp <targets> <many>   a selector matching several entities (/tp @s @e[type=zombie])
//                          sends each target to the nearest living match, not itself.
//   /summon <entity> <amount> [geared] [despawn] [pos] [nbt]
//                          geared: full iron armour and an iron sword (mobs only)
//                          despawn: false makes the mob persistent
public final class TestingCommands {

    private static final int MAX_AMOUNT = 500;
    private static final SimpleCommandExceptionType NO_DESTINATION =
        new SimpleCommandExceptionType(Component.literal("No valid entity to teleport to"));

    private TestingCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
            registerTeleport(dispatcher, "teleport");
            registerTeleport(dispatcher, "tp");
            registerSummon(dispatcher, context);
        });
    }

    private static void registerTeleport(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("nearest", EntityArgument.entities())
                    .executes(TestingCommands::teleportToNearest))));
    }

    private static int teleportToNearest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        Collection<? extends Entity> candidates = EntityArgument.getOptionalEntities(ctx, "nearest");
        int moved = 0;
        Entity last = null;
        for (Entity target : targets) {
            Entity destination = nearest(target, candidates);
            if (destination == null) {
                continue;
            }
            target.teleportTo((ServerLevel) target.level(),
                destination.getX(), destination.getY(), destination.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
            last = destination;
            moved++;
        }
        if (moved == 0) {
            throw NO_DESTINATION.create();
        }
        Entity shown = last;
        int count = moved;
        ctx.getSource().sendSuccess(() -> Component.literal("Teleported " + count + " entit" + (count == 1 ? "y" : "ies") + " to ")
            .append(shown.getDisplayName()), true);
        return moved;
    }

    // Nearest living candidate in the target's dimension, other than the target itself.
    private static Entity nearest(Entity target, Collection<? extends Entity> candidates) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            if (candidate == target || !candidate.isAlive() || candidate.level() != target.level()
                || candidate instanceof ServerPlayer player && player.isSpectator()) {
                continue;
            }
            double distance = candidate.distanceToSqr(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    // Every extra field is optional and keeps its order: amount, geared, despawn, then vanilla's
    // pos and nbt. Any of amount/geared can be left out, so /summon zombie 5 ~ ~ ~ and
    // /summon zombie true ~ ~ ~ both work. despawn only follows geared, since two bare
    // booleans in a row could not be told apart. The entity node's own pos child is
    // vanilla's and is left alone, so plain /summon is untouched.
    private static void registerSummon(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("summon")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, MAX_AMOUNT))
                    .executes(TestingCommands::summon)
                    .then(geared())
                    .then(position()))
                .then(geared())));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> geared() {
        return Commands.argument("geared", BoolArgumentType.bool())
            .executes(TestingCommands::summon)
            .then(Commands.argument("despawn", BoolArgumentType.bool())
                .executes(TestingCommands::summon)
                .then(position()))
            .then(position());
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> position() {
        return Commands.argument("pos", Vec3Argument.vec3())
            .executes(TestingCommands::summon)
            .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                .executes(TestingCommands::summon));
    }

    // The value of an argument, or null when this command line left it out.
    private static <T> T optional(CommandContext<CommandSourceStack> ctx, String name, Class<T> type) {
        try {
            return ctx.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int summon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Integer amountArg = optional(ctx, "amount", Integer.class);
        Boolean gearedArg = optional(ctx, "geared", Boolean.class);
        Boolean despawnArg = optional(ctx, "despawn", Boolean.class);
        boolean hasPos = optional(ctx, "pos", net.minecraft.commands.arguments.coordinates.Coordinates.class) != null;
        return summon(ctx, amountArg == null ? 1 : amountArg, gearedArg != null && gearedArg,
            despawnArg == null || despawnArg,
            hasPos ? Vec3Argument.getVec3(ctx, "pos") : null,
            optional(ctx, "nbt", CompoundTag.class));
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, int amount, boolean geared, boolean despawn,
                              Vec3 pos, CompoundTag nbt) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> type = ResourceArgument.getSummonableEntityType(ctx, "entity");
        Vec3 at = pos != null ? pos : ctx.getSource().getPosition();
        CompoundTag tag = nbt != null ? nbt : new CompoundTag();
        boolean randomize = nbt == null;
        int summoned = 0;
        for (int i = 0; i < amount; i++) {
            Entity entity = SummonCommand.createEntity(ctx.getSource(), type, at, tag.copy(), randomize);
            if (entity instanceof Mob mob) {
                if (geared) {
                    gear(mob);
                }
                if (!despawn) {
                    mob.setPersistenceRequired();
                }
            }
            summoned++;
        }
        int count = summoned;
        ctx.getSource().sendSuccess(() -> Component.literal("Summoned " + count + " ")
            .append(type.value().getDescription()), true);
        return summoned;
    }

    private static void gear(Mob mob) {
        List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND);
        List<ItemStack> items = List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.IRON_SWORD));
        for (int i = 0; i < slots.size(); i++) {
            mob.setItemSlot(slots.get(i), items.get(i));
        }
    }
}
