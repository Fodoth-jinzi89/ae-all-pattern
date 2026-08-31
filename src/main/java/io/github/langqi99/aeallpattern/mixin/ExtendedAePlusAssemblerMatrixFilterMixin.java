package io.github.langqi99.aeallpattern.mixin;

import appeng.api.inventories.InternalInventory;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows aggregate patterns into ExtendedAEPlus assembler-matrix pattern inventories. */
@Pseudo
@Mixin(targets = "com.extendedae_plus.content.matrix.PatternCorePlusBlockEntity$Filter", remap = false)
public abstract class ExtendedAePlusAssemblerMatrixFilterMixin {
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
