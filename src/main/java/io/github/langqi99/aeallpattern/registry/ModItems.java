package io.github.langqi99.aeallpattern.registry;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.PatternBinderItem;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternItem;
import io.github.langqi99.aeallpattern.aggregate.AllPatternGeneratorItem;
import io.github.langqi99.aeallpattern.tianshu.TianshuPatternSelectorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AeAllPattern.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeAllPattern.MOD_ID);

    public static final DeferredItem<PatternBinderItem> PATTERN_BINDER = ITEMS.registerItem(
            "pattern_binder", PatternBinderItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> PATTERN_LINKER = ITEMS.registerSimpleBlockItem(
            "pattern_linker", ModBlocks.PATTERN_LINKER, new Item.Properties());
    public static final DeferredItem<TianshuPatternSelectorItem> TIANSHU_PATTERN_SELECTOR = ITEMS.registerItem(
            "tianshu_pattern_selector",
            properties -> new TianshuPatternSelectorItem(ModBlocks.TIANSHU_PATTERN_SELECTOR.get(), properties),
            new Item.Properties());
    public static final DeferredItem<AllPatternGeneratorItem> ALL_PATTERN_GENERATOR = ITEMS.registerItem(
            "all_pattern_generator", AllPatternGeneratorItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<AggregatePatternItem> AGGREGATE_PATTERN = ITEMS.registerItem(
            "aggregate_pattern", AggregatePatternItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aeallpattern"))
                    .icon(() -> PATTERN_BINDER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(PATTERN_BINDER.get());
                        output.accept(PATTERN_LINKER.get());
                        output.accept(TIANSHU_PATTERN_SELECTOR.get());
                        output.accept(ALL_PATTERN_GENERATOR.get());
                    })
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
