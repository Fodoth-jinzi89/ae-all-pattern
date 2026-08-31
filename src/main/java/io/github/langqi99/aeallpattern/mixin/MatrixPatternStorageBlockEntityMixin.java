package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows aggregate crafting patterns in AE2LT's matter-warping matrix storage. */
@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.blockentity.MatrixPatternStorageBlockEntity", remap = false)
public abstract class MatrixPatternStorageBlockEntityMixin {
    @Shadow @Final private NonNullList<ItemStack> items;
    @Shadow @Final private List<IPatternDetails> cachedPatterns;

    @Inject(method = "isValidPatternStack", at = @At("HEAD"), cancellable = true)
    private void aeallpattern$allowAggregate(
            ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (stack.has(ModDataComponents.AGGREGATE_PATTERN.get())) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "rebuildPatternCache", at = @At("TAIL"))
    private void aeallpattern$addAggregatePatterns(CallbackInfo callback) {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return;
        }
        for (ItemStack stack : items) {
            for (IPatternDetails pattern : AggregatePatternExpander.expand(stack, level)) {
                if (pattern instanceof IMolecularAssemblerSupportedPattern) {
                    cachedPatterns.add(pattern);
                }
            }
        }
    }
}
