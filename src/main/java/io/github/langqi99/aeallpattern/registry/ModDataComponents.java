package io.github.langqi99.aeallpattern.registry;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.AnchorSelection;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternRef;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.mojang.serialization.Codec;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AeAllPattern.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AnchorSelection>> ANCHOR_SELECTION =
            COMPONENTS.registerComponentType("anchor_selection", builder -> builder
                    .persistent(AnchorSelection.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(AnchorSelection.CODEC))
                    .cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> VIRTUAL_PATTERN_ID =
            COMPONENTS.registerComponentType("virtual_pattern_id", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.stringUtf8(160))
                    .cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AggregatePatternRef>> AGGREGATE_PATTERN =
            COMPONENTS.registerComponentType("aggregate_pattern", builder -> builder
                    .persistent(AggregatePatternRef.CODEC)
                    .networkSynchronized(AggregatePatternRef.STREAM_CODEC)
                    .cacheEncoding());

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
