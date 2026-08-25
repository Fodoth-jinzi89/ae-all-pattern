package io.github.langqi99.aeallpattern.binding;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockEntityFingerprint {
    private BlockEntityFingerprint() {
    }

    public static String of(BlockEntity blockEntity) {
        return BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock())
                + "|"
                + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
    }
}
