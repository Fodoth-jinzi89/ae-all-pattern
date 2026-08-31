package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Registers aggregate patterns as encoded patterns and supplies a marker for expansion. */
public final class AggregatePatternDecoder implements IPatternDetailsDecoder {
    public static void register() {
        PatternDetailsHelper.registerDecoder(new AggregatePatternDecoder());
    }

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return stack.is(ModItems.AGGREGATE_PATTERN.get())
                && stack.has(ModDataComponents.AGGREGATE_PATTERN.get());
    }

    @Override
    public IPatternDetails decodePattern(AEItemKey key, Level level) {
        // AE2 asks every registered decoder to decode empty molecular-assembler
        // pattern slots as well. AEItemKey.of(ItemStack.EMPTY) is null.
        if (key == null) {
            return null;
        }
        ItemStack stack = key.toStack();
        if (!isEncodedPattern(stack)) {
            return null;
        }
        // Decoding happens synchronously while pattern inventories validate and rebuild.
        // Never expand the aggregate here: large aggregates can contain thousands of recipes,
        // and providers expand them at their catalog boundary immediately afterwards.
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                java.util.List.of(appeng.api.stacks.GenericStack.fromItemStack(new ItemStack(Items.COBBLESTONE))),
                java.util.List.of(appeng.api.stacks.GenericStack.fromItemStack(new ItemStack(Items.STONE))));
        IPatternDetails placeholder = PatternDetailsHelper.decodePattern(encoded, level);
        return placeholder == null ? null : new AggregatePatternMarkerDetails(key, placeholder);
    }
}
