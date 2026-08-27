package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.util.inv.AppEngInternalInventory;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternMarkerDetails;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes aggregate children through AE2 Lightning Tech's overloaded provider catalog.
 *
 * <p>The overloaded provider overrides AE2's normal pattern refresh method, so the generic
 * {@link PatternProviderLogicMixin} hook is intentionally bypassed. Expanding at the catalog
 * boundary also lets every virtual child retain the physical aggregate slot it came from.</p>
 */
@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.logic.OverloadedProviderPatternCatalog", remap = false)
public abstract class OverloadedProviderPatternCatalogMixin {
    @Shadow
    abstract void register(IPatternDetails pattern, int slot);

    @Inject(method = "rebuild", at = @At("TAIL"))
    private void aeallpattern$expandAggregatePatterns(
            AppEngInternalInventory patternInventory,
            Level level,
            List<IPatternDetails> patterns,
            Set<AEKey> patternInputs,
            CallbackInfo callback) {
        patterns.removeIf(AggregatePatternMarkerDetails.class::isInstance);

        for (int slot = 0; slot < patternInventory.size(); slot++) {
            var expanded = AggregatePatternExpander.expand(patternInventory.getStackInSlot(slot), level);
            for (IPatternDetails pattern : expanded) {
                patterns.add(pattern);
                register(pattern, slot);

                for (IPatternDetails.IInput input : pattern.getInputs()) {
                    for (var possibleInput : input.getPossibleInputs()) {
                        patternInputs.add(possibleInput.what().dropSecondary());
                    }
                }
            }
        }
    }
}
