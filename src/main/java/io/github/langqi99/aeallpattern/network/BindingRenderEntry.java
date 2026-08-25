package io.github.langqi99.aeallpattern.network;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BindingRenderEntry(UUID bindingId, ResourceKey<Level> dimension, BlockPos pos, byte status) {
}
