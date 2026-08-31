package io.github.langqi99.aeallpattern.mixin;

import appeng.api.inventories.InternalInventory;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows aggregate patterns into ExtendedAE assembler-matrix pattern inventories. */
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern$Filter",
        remap = false)
public abstract class ExtendedAeAssemblerMatrixFilterMixin {
    @Inject(method = "allowInsert", at = @At("HEAD"), cancellable = true)
    private void aeallpattern$allowAggregate(
            InternalInventory inventory,
            int slot,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback) {
        if (stack.has(ModDataComponents.AGGREGATE_PATTERN.get())) {
            callback.setReturnValue(true);
        }
    }
}
