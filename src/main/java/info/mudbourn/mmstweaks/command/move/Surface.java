package info.mudbourn.mmstweaks.command.move;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Surface {

    private Surface() {
    }

    // Standing y for a column, or null when nothing safe is there.
    public static Integer standingY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        if (level.dimensionType().hasCeiling()) {
            int top = level.getMinY() + level.dimensionType().logicalHeight() - 2;
            for (int y = top; y > level.getMinY() + 1; y--) {
                if (safeAt(level, new BlockPos(x, y, z))) {
                    return y;
                }
            }
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return safeAt(level, new BlockPos(x, y, z)) ? y : null;
    }

    private static boolean safeAt(ServerLevel level, BlockPos feet) {
        BlockState below = level.getBlockState(feet.below());
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        return below.isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
            && below.getFluidState().isEmpty()
            && !below.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
            && !below.is(net.minecraft.world.level.block.Blocks.CACTUS)
            && body.getCollisionShape(level, feet).isEmpty()
            && body.getFluidState().isEmpty()
            && head.getCollisionShape(level, feet.above()).isEmpty()
            && head.getFluidState().isEmpty();
    }
}
