package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.inv.AppEngInternalInventory;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternMarkerDetails;
import io.github.langqi99.aeallpattern.compat.TechStartPatternCompat;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Expands one aggregate item into every child recipe published by the provider. */
@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {
    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Shadow
    @Final
    private AppEngInternalInventory patternInventory;

    @Shadow
    @Final
    private List<IPatternDetails> patterns;

    @Shadow
    @Final
    private Set<AEKey> patternInputs;

    @Shadow @Final private IManagedGridNode mainNode;

    @Inject(method = "updatePatterns", at = @At("HEAD"), cancellable = true)
    private void aeallpattern$expandAggregatePatterns(CallbackInfo callback) {
        patterns.clear();
        patternInputs.clear();
        patterns.removeIf(AggregatePatternMarkerDetails.class::isInstance);
        var level = host.getBlockEntity().getLevel();
        if (level != null) {
            for (var stack : patternInventory) {
                var expanded = AggregatePatternExpander.expand(stack, level);
                if (expanded.isEmpty()) expanded = TechStartPatternCompat.expand(stack, level);
                if (expanded.isEmpty()) {
                    var decoded = PatternDetailsHelper.decodePattern(stack, level);
                    if (decoded != null) patterns.add(decoded);
                } else {
                    patterns.addAll(expanded);
                }
            }
        }

        patternInputs.clear();
        for (IPatternDetails pattern : patterns) {
            for (IPatternDetails.IInput input : pattern.getInputs()) {
                for (var possibleInput : input.getPossibleInputs()) {
                    patternInputs.add(possibleInput.what().dropSecondary());
                }
            }
        }
        ICraftingProvider.requestUpdate(mainNode);
        callback.cancel();
    }
}
