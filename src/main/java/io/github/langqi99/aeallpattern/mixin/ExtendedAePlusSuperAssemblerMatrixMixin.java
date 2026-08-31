package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Publishes aggregate crafting children from ExtendedAEPlus super-matrix inventories. */
@Pseudo
@Mixin(targets = "com.extendedae_plus.content.matrix.supermatrix.SuperAssemblerMatrixCluster", remap = false)
public abstract class ExtendedAePlusSuperAssemblerMatrixMixin {
    @Inject(method = "getAvailablePatterns", at = @At("RETURN"), cancellable = true)
    private void aeallpattern$addAggregatePatterns(
            CallbackInfoReturnable<List<IPatternDetails>> callback) {
        List<IPatternDetails> patterns = new ArrayList<>(callback.getReturnValue());
        var level = aeallpattern$level();
        if (level == null) {
            return;
        }
        for (var inventory : aeallpattern$patternInventories()) {
            for (var stack : inventory) {
                for (IPatternDetails pattern : AggregatePatternExpander.expand(stack, level)) {
                    if (pattern instanceof IMolecularAssemblerSupportedPattern) {
                        patterns.add(pattern);
                    }
                }
            }
        }
        callback.setReturnValue(List.copyOf(patterns));
    }

    @Unique
    private net.minecraft.world.level.Level aeallpattern$level() {
        try {
            var field = getClass().getDeclaredField("core");
            field.setAccessible(true);
            Object value = field.get(this);
            return value instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity
                    ? blockEntity.getLevel()
                    : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Unique
    private appeng.api.inventories.InternalInventory[] aeallpattern$patternInventories() {
        try {
            return (appeng.api.inventories.InternalInventory[]) getClass()
                    .getMethod("getPatternInventories")
                    .invoke(this);
        } catch (ReflectiveOperationException ignored) {
            return new appeng.api.inventories.InternalInventory[0];
        }
    }
}
