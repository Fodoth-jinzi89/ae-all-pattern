package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.moakiee.thunderbolt.ae2.crafting.RoutingPatternMetadata;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A single child processing pattern expanded from an aggregate pattern item. */
public final class AggregatePatternDetails implements IPatternDetails, RoutingPatternMetadata {
    private final String patternId;
    private final AEItemKey definition;
    private final IPatternDetails delegate;
    private final int processingTicks;

    public AggregatePatternDetails(
            String patternId, AEItemKey definition, IPatternDetails delegate, int processingTicks) {
        this.patternId = Objects.requireNonNull(patternId, "patternId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.processingTicks = Math.max(1, processingTicks);
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return delegate.getInputs();
    }

    @Override
    public List<GenericStack> getOutputs() {
        return delegate.getOutputs();
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return delegate.supportsPushInputsToExternalInventory();
    }

    @Override
    public void pushInputsToExternalInventory(KeyCounter[] inputHolders, PatternInputSink sink) {
        delegate.pushInputsToExternalInventory(inputHolders, sink);
    }

    @Override
    public PatternDetailsTooltip getTooltip(Level level, TooltipFlag flags) {
        return delegate.getTooltip(level, flags);
    }

    @Override
    public boolean isAggregatePattern() {
        return true;
    }

    @Override
    public int processingTicks() {
        return processingTicks;
    }

    @Override
    public String stableRouteId() {
        return patternId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AggregatePatternDetails pattern
                && definition.equals(pattern.definition)
                && patternId.equals(pattern.patternId);
    }

    @Override
    public int hashCode() {
        return 31 * definition.hashCode() + patternId.hashCode();
    }
}
