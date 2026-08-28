package io.github.langqi99.aeallpattern.machine;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Resolves proxy blocks to the machine controller that owns their capabilities. */
public final class MachineTargetResolver {
    private static final String MEKANISM_BOUNDING_TILE = "mekanism.common.tile.TileEntityBoundingBlock";

    private MachineTargetResolver() {
    }

    public static BlockPos resolvePosition(Level level, BlockPos clickedPos) {
        Object blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity == null || !blockEntity.getClass().getName().equals(MEKANISM_BOUNDING_TILE)) {
            return clickedPos;
        }
        try {
            Method getMainPos = blockEntity.getClass().getMethod("getMainPos");
            Object mainPos = getMainPos.invoke(blockEntity);
            return mainPos instanceof BlockPos pos && level.hasChunkAt(pos) ? pos.immutable() : clickedPos;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return clickedPos;
        }
    }
}
