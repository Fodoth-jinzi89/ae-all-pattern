package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.util.inv.AppEngInternalInventory;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes aggregate crafting children from ExtendedAE assembler-matrix pattern cores. */
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern",
        remap = false)
public abstract class ExtendedAeAssemblerMatrixPatternMixin {
    @Shadow @Final private AppEngInternalInventory patternInventory;
    @Shadow @Final private List<IPatternDetails> patterns;

    @Inject(method = "updatePatterns", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;requestUpdate(Lappeng/api/networking/IManagedGridNode;)V",
            shift = At.Shift.BEFORE))
    private void aeallpattern$addAggregatePatterns(CallbackInfo callback) {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return;
        }
        for (var stack : patternInventory) {
            for (IPatternDetails pattern : AggregatePatternExpander.expand(stack, level)) {
                if (pattern instanceof IMolecularAssemblerSupportedPattern) {
                    patterns.add(pattern);
                }
            }
        }
    }
}
