package io.github.langqi99.aeallpattern.registry;

import appeng.api.AECapabilities;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlockEntity;
import io.github.langqi99.aeallpattern.tianshu.TianshuPatternSelectorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AeAllPattern.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternLinkerBlockEntity>> PATTERN_LINKER =
            BLOCK_ENTITIES.register("pattern_linker", () -> BlockEntityType.Builder.of(
                    PatternLinkerBlockEntity::new, ModBlocks.PATTERN_LINKER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TianshuPatternSelectorBlockEntity>>
            TIANSHU_PATTERN_SELECTOR = BLOCK_ENTITIES.register(
                    "tianshu_pattern_selector",
                    () -> BlockEntityType.Builder.of(
                            TianshuPatternSelectorBlockEntity::new,
                            ModBlocks.TIANSHU_PATTERN_SELECTOR.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                PATTERN_LINKER.get(),
                (linker, ignored) -> linker);
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                TIANSHU_PATTERN_SELECTOR.get(),
                (selector, ignored) -> selector);
    }
}
