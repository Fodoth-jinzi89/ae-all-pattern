package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

/** Keeps AE2's molecular-assembler behavior while giving an aggregate child a unique definition. */
public final class AggregateAssemblerPatternDetails implements IMolecularAssemblerSupportedPattern {
    private final String patternId;
    private final AEItemKey definition;
    private final IMolecularAssemblerSupportedPattern delegate;

    public AggregateAssemblerPatternDetails(
            String patternId,
            AEItemKey definition,
            IMolecularAssemblerSupportedPattern delegate) {
        this.patternId = Objects.requireNonNull(patternId, "patternId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IPatternDetails.IInput[] getInputs() {
        return delegate.getInputs();
    }

    @Override
    public List<GenericStack> getOutputs() {
        return delegate.getOutputs();
    }

    @Override
    public ItemStack assemble(CraftingInput input, Level level) {
        return delegate.assemble(input, level);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return delegate.getRemainingItems(input);
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey item, Level level) {
        return delegate.isItemValid(slot, item, level);
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return delegate.isSlotEnabled(slot);
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] inputHolders, CraftingGridAccessor grid) {
        delegate.fillCraftingGrid(inputHolders, grid);
    }

    @Override
    public PatternDetailsTooltip getTooltip(Level level, TooltipFlag flags) {
        return delegate.getTooltip(level, flags);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AggregateAssemblerPatternDetails pattern
                && definition.equals(pattern.definition)
                && patternId.equals(pattern.patternId);
    }

    @Override
    public int hashCode() {
        return 31 * definition.hashCode() + patternId.hashCode();
    }
}
