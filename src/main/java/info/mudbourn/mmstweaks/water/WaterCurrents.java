package info.mudbourn.mmstweaks.water;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// MMS water currents: a push only along the way you travel, never against it.
public final class WaterCurrents {

    public static final float SWIM_SPEED_FACTOR = 1.15F;

    private static final double SWIM_BOOST = 0.014;
    private static final double SWIM_IDLE_DRIFT = 0.014;
    private static final double BOAT_BOOST = 0.02;
    private static final double BOAT_IDLE_DRIFT = 0.01;
    private static final double BOAT_MOVING_SPEED = 0.01;
    private static final double MIN_FLOW = 1.0E-4;

    private static volatile Reading last = Reading.NONE;

    private WaterCurrents() {
    }

    // One tick of current, as the debug readout shows it.
    public record Reading(Vec3 flow, double along, Vec3 applied, Vec3 otherPush, long tick) {
        static final Reading NONE = new Reading(Vec3.ZERO, 0.0, Vec3.ZERO, Vec3.ZERO, Long.MIN_VALUE);
    }

    public static Reading last() {
        return last;
    }

    public static void swimmer(Player player, Vec3 otherPush) {
        if (player.getAbilities().flying || player.isPassenger() || !player.isInWater()) {
            return;
        }
        Vec3 flow = flowAround(player.level(), player.getBoundingBox());
        Vec3 input = worldInput(player);
        Vec3 applied = push(player, flow, input, SWIM_BOOST, SWIM_IDLE_DRIFT);
        record(player, flow, input, applied, otherPush);
    }

    public static void boat(AbstractBoat boat, Vec3 otherPush) {
        if (!boat.isInWater()) {
            return;
        }
        AABB box = boat.getBoundingBox();
        Vec3 flow = flowAround(boat.level(), box.setMinY(box.minY - 0.2));
        Vec3 velocity = horizontal(boat.getDeltaMovement());
        Vec3 heading = velocity.length() < BOAT_MOVING_SPEED ? Vec3.ZERO : velocity;
        Vec3 applied = push(boat, flow, heading, BOAT_BOOST, BOAT_IDLE_DRIFT);
        if (boat.getControllingPassenger() instanceof Player) {
            record(boat, flow, heading, applied, otherPush);
        }
    }

    // Adds the along-travel share of the current, or a gentle drift when not travelling.
    private static Vec3 push(Entity entity, Vec3 flow, Vec3 heading, double boost, double idleDrift) {
        if (flow.lengthSqr() < MIN_FLOW * MIN_FLOW) {
            return Vec3.ZERO;
        }
        Vec3 applied;
        if (heading.lengthSqr() < MIN_FLOW * MIN_FLOW) {
            applied = flow.scale(idleDrift);
        } else {
            Vec3 direction = heading.normalize();
            double along = flow.dot(direction);
            applied = along > 0.0 ? direction.scale(along * boost) : Vec3.ZERO;
        }
        entity.addDeltaMovement(applied);
        return applied;
    }

    // Averages the horizontal flow of the water blocks the box touches.
    private static Vec3 flowAround(Level level, AABB box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vec3 sum = Vec3.ZERO;
        int count = 0;

        for (int x = Mth.floor(box.minX); x <= Mth.ceil(box.maxX) - 1; x++) {
            for (int y = Mth.floor(box.minY); y <= Mth.ceil(box.maxY) - 1; y++) {
                for (int z = Mth.floor(box.minZ); z <= Mth.ceil(box.maxZ) - 1; z++) {
                    pos.set(x, y, z);
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.is(FluidTags.WATER)) {
                        sum = sum.add(fluid.getFlow(level, pos));
                        count++;
                    }
                }
            }
        }
        return count == 0 ? Vec3.ZERO : horizontal(sum.scale(1.0 / count));
    }

    // The player's movement keys turned into a world-space direction.
    private static Vec3 worldInput(Player player) {
        float sin = Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
        float cos = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
        return new Vec3(player.xxa * cos - player.zza * sin, 0.0, player.zza * cos + player.xxa * sin);
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0, vector.z);
    }

    private static void record(Entity entity, Vec3 flow, Vec3 heading, Vec3 applied, Vec3 otherPush) {
        if (!entity.level().isClientSide()) {
            return;
        }
        double along = heading.lengthSqr() < MIN_FLOW * MIN_FLOW ? 0.0 : flow.dot(heading.normalize());
        last = new Reading(flow, along, applied, otherPush, entity.level().getGameTime());
    }
}
