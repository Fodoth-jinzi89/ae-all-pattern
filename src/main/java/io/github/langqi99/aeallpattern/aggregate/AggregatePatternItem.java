package io.github.langqi99.aeallpattern.aggregate;

import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** One physical AE pattern item that publishes every captured child recipe. */
public final class AggregatePatternItem extends Item {
    public AggregatePatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        AggregatePatternRef ref = stack.get(ModDataComponents.AGGREGATE_PATTERN.get());
        if (ref == null) {
            return super.getName(stack);
        }
        String machineKey = AggregateMetadataView.find(ref.libraryId())
                .map(AggregateMetadataView.Entry::machineTranslationKey)
                .orElseGet(() -> BuiltInRegistries.BLOCK.get(ref.catalystId()).getDescriptionId());
        return Component.translatable(
                "item.aeallpattern.aggregate_pattern.named",
                Component.translatable(machineKey));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        AggregatePatternRef ref = stack.get(ModDataComponents.AGGREGATE_PATTERN.get());
        if (ref == null) {
            tooltip.add(Component.translatable("tooltip.aeallpattern.aggregate_pattern.empty")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        var metadata = AggregateMetadataView.find(ref.libraryId());
        if (metadata.isPresent()) {
            tooltip.add(Component.translatable(
                    "tooltip.aeallpattern.aggregate_pattern.count", metadata.orElseThrow().recipeCount())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.aeallpattern.aggregate_pattern.syncing")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.aeallpattern.aggregate_pattern.provider")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.AGGREGATE_PATTERN.get()) || super.isFoil(stack);
    }
}
