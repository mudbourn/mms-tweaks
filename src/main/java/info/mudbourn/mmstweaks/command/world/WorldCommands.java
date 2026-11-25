package info.mudbourn.mmstweaks.command.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmstweaks.command.Msg;
import info.mudbourn.mmstweaks.command.Targets;
import info.mudbourn.mmstweaks.command.config.CommandConfig;
import info.mudbourn.mmstweaks.command.data.Spot;
import info.mudbourn.mmstweaks.command.move.Surface;
import info.mudbourn.mmstweaks.command.move.Teleports;
import info.mudbourn.mmstweaks.command.tier.Tier;
import info.mudbourn.mmstweaks.command.tier.Tiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class WorldCommands {

    private static final int RTP_ATTEMPTS = 30;
    private static final Map<UUID, Integer> RTP_READY = new HashMap<>();

    private WorldCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtp")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(WorldCommands::rtp));
        dispatcher.register(Commands.literal("dimension")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .then(Targets.withTarget(Commands.argument("dimension", DimensionArgument.dimension()), WorldCommands::dimension)));
        dispatcher.register(Commands.literal("day")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(ctx -> time(ctx, "day")));
        dispatcher.register(Commands.literal("night")
            .requires(Tiers.atLeast(Tier.MODERATOR))
            .executes(ctx -> time(ctx, "night")));
    }

    private static int time(CommandContext<CommandSourceStack> ctx, String preset) {
        CommandSourceStack source = ctx.getSource().withPermission(LevelBasedPermissionSet.GAMEMASTER);
        source.getServer().getCommands().performPrefixedCommand(source, "time set " + preset);
        return 1;
    }

    private static int rtp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD) {
            throw Msg.fail("Random teleport only works in the overworld");
        }
        int now = level.getServer().getTickCount();
        int ready = RTP_READY.getOrDefault(player.getUUID(), 0);
        if (now < ready) {
            throw Msg.fail("Random teleport again in " + (ready - now + 19) / 20 + " seconds");
        }
        CommandConfig config = CommandConfig.get();
        int min = Math.min(config.rtp_min, config.rtp_radius);
        int max = config.rtp_radius;
        BlockPos center = level.getRespawnData().pos();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < RTP_ATTEMPTS; attempt++) {
            int far = (random.nextBoolean() ? 1 : -1) * random.nextInt(min, max + 1);
            int along = random.nextInt(-max, max + 1);
            int x = center.getX() + (random.nextBoolean() ? far : along);
            int z = center.getZ() + (x == center.getX() + far ? along : far);
            if (!level.getWorldBorder().isWithinBounds(x, z)) {
                continue;
            }
            Integer y = Surface.standingY(level, x, z);
            if (y == null) {
                continue;
            }
            Teleports.go(player, new Spot(
                level.dimension().identifier().toString(),
                x + 0.5,
                y,
                z + 0.5,
                player.getYRot(),
                player.getXRot()
            ));
            RTP_READY.put(player.getUUID(), now + config.rtp_cooldown_seconds * 20);
            Msg.ok(ctx, "Teleported to " + x + ", " + y + ", " + z);
            return 1;
        }
        throw Msg.fail("No safe spot found, try again");
    }

    private static int dimension(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ServerLevel to = DimensionArgument.getDimension(ctx, "dimension");
        ServerLevel from = target.level();
        if (to == from) {
            throw Msg.fail("Already in that dimension");
        }
        double scale = from.dimensionType().coordinateScale() / to.dimensionType().coordinateScale();
        double x = target.getX() * scale;
        double z = target.getZ() * scale;
        Integer found = Surface.standingY(to, (int) Math.floor(x), (int) Math.floor(z));
        double y = found != null ? found : Math.clamp(target.getY(), to.getMinY() + 1, to.getMaxY() - 1);
        Teleports.go(target, new Spot(to.dimension().identifier().toString(), x, y, z, target.getYRot(), target.getXRot()));
        Msg.ok(ctx, target.getName().getString() + " moved to " + to.dimension().identifier());
        return 1;
    }
}
