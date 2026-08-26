package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Decoder marker that lets AE2 accept the custom item in encoded-pattern slots. */
public final class AggregatePatternMarkerDetails implements IPatternDetails {
    private final AEItemKey definition;
    private final IPatternDetails delegate;

    public AggregatePatternMarkerDetails(AEItemKey definition, IPatternDetails delegate) {
        this.definition = definition;
        this.delegate = delegate;
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
}
