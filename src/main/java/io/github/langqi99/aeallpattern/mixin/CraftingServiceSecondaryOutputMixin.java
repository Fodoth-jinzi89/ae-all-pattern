package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.storage.AEKeyFilter;
import appeng.me.service.CraftingService;
import appeng.me.service.helpers.NetworkCraftingProviders;
import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.SecondaryOutputPatternSource;
import io.github.langqi99.aeallpattern.tianshu.TianshuRoutingPolicies;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds an opt-in secondary-output index without duplicating the actual provider pattern. */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceSecondaryOutputMixin implements SecondaryOutputPatternSource {
    @Shadow
    @Final
    private IGrid grid;

    @Shadow
    @Final
    private NetworkCraftingProviders craftingProviders;

    @org.spongepowered.asm.mixin.Unique
    private volatile Map<AEKey, List<IPatternDetails>> aeallpattern$secondaryIndex = Map.of();

    @org.spongepowered.asm.mixin.Unique
    private volatile long aeallpattern$secondaryIndexRevision = Long.MIN_VALUE;

    @Override
    public Collection<IPatternDetails> aeallpattern$getSecondaryCraftingFor(AEKey output) {
        if (output == null) {
            return List.of();
        }
        return aeallpattern$getSecondaryIndex().getOrDefault(output, List.of());
    }

    @Override
    public void aeallpattern$secondaryOutputsChanged() {
        aeallpattern$secondaryIndexRevision = Long.MIN_VALUE;
        ((NetworkCraftingProvidersAccessor) craftingProviders).aeallpattern$markModified();
    }

    /**
     * AE's {@code isCraftable} query is backed by {@code getCraftingFor}, while terminal population
     * uses {@code getCraftables}. Keep both views consistent, but only while an online router has
     * explicitly enabled independent byproduct orders.
     */
    @Inject(method = "getCraftingFor", at = @At("RETURN"), cancellable = true)
    private void aeallpattern$includeConfiguredSecondaryPatterns(
            AEKey output, CallbackInfoReturnable<Collection<IPatternDetails>> cir) {
        if (output == null || !TianshuRoutingPolicies.resolve(grid).allowByproductOrders()) {
            return;
        }
        Collection<IPatternDetails> secondary = aeallpattern$getSecondaryCraftingFor(output);
        if (secondary.isEmpty()) {
            return;
        }
        LinkedHashSet<IPatternDetails> result = new LinkedHashSet<>(cir.getReturnValue());
        result.addAll(secondary);
        cir.setReturnValue(List.copyOf(result));
    }

    @Inject(method = "getCraftables", at = @At("RETURN"), cancellable = true)
    private void aeallpattern$includeConfiguredSecondaryKeys(
            AEKeyFilter filter, CallbackInfoReturnable<Set<AEKey>> cir) {
        if (!TianshuRoutingPolicies.resolve(grid).allowByproductOrders()) {
            return;
        }
        Set<AEKey> result = new LinkedHashSet<>(cir.getReturnValue());
        for (AEKey output : aeallpattern$getSecondaryIndex().keySet()) {
            if (filter.matches(output)) {
                result.add(output);
            }
        }
        cir.setReturnValue(Set.copyOf(result));
    }

    @org.spongepowered.asm.mixin.Unique
    private Map<AEKey, List<IPatternDetails>> aeallpattern$getSecondaryIndex() {
        long revision = craftingProviders.getLastModifiedOnTick();
        if (revision == aeallpattern$secondaryIndexRevision) {
            return aeallpattern$secondaryIndex;
        }
        synchronized (this) {
            revision = craftingProviders.getLastModifiedOnTick();
            if (revision == aeallpattern$secondaryIndexRevision) {
                return aeallpattern$secondaryIndex;
            }
            Map<AEKey, List<IPatternDetails>> rebuilt = new HashMap<>();
            Set<IPatternDetails> patterns = ((NetworkCraftingProvidersAccessor) craftingProviders)
                    .aeallpattern$getCraftingMethods().keySet();
            for (IPatternDetails pattern : patterns) {
                AEKey primary = pattern.getPrimaryOutput() == null
                        ? null
                        : pattern.getPrimaryOutput().what();
                Set<AEKey> seen = new LinkedHashSet<>();
                for (var output : pattern.getOutputs()) {
                    AEKey key = output.what();
                    if (key == null || key.equals(primary) || !seen.add(key)) {
                        continue;
                    }
                    rebuilt.computeIfAbsent(key, ignored -> new ArrayList<>()).add(pattern);
                }
            }
            rebuilt.replaceAll((ignored, value) -> List.copyOf(value));
            aeallpattern$secondaryIndex = Map.copyOf(rebuilt);
            aeallpattern$secondaryIndexRevision = revision;
            return aeallpattern$secondaryIndex;
        }
    }
}
