package io.github.langqi99.aeallpattern.registry;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlock;
import io.github.langqi99.aeallpattern.tianshu.TianshuPatternSelectorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AeAllPattern.MOD_ID);

    public static final DeferredBlock<PatternLinkerBlock> PATTERN_LINKER = BLOCKS.registerBlock(
            "pattern_linker",
            PatternLinkerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.METAL)
                    .strength(2.2F, 10.0F)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<TianshuPatternSelectorBlock> TIANSHU_PATTERN_SELECTOR = BLOCKS.registerBlock(
            "tianshu_pattern_selector",
            TianshuPatternSelectorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.METAL)
                    .strength(5.0F, 12.0F)
                    .lightLevel(state -> state.getValue(TianshuPatternSelectorBlock.ACTIVE) ? 7 : 1)
                    .requiresCorrectToolForDrops());

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
