package io.github.langqi99.aeallpattern.aggregate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Per-item encoding preferences for one aggregate pattern. */
public record AggregatePatternOptions(
        boolean splitSameItems,
        boolean ignoreOutputComponents,
        boolean skipProbabilisticMainOutput,
        boolean ignoreProbabilisticByproducts) {
    public static final AggregatePatternOptions DEFAULT = new AggregatePatternOptions(false, false, true, true);

    /** Compatibility constructor; newly introduced probability safeguards default to enabled. */
    public AggregatePatternOptions(boolean splitSameItems, boolean ignoreOutputComponents) {
        this(splitSameItems, ignoreOutputComponents, true, true);
    }
    public static final Codec<AggregatePatternOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("split_same_items", false)
                    .forGetter(AggregatePatternOptions::splitSameItems),
            Codec.BOOL.optionalFieldOf("ignore_output_components", false)
                    .forGetter(AggregatePatternOptions::ignoreOutputComponents),
            Codec.BOOL.optionalFieldOf("skip_probabilistic_main_output", true)
                    .forGetter(AggregatePatternOptions::skipProbabilisticMainOutput),
            Codec.BOOL.optionalFieldOf("ignore_probabilistic_byproducts", true)
                    .forGetter(AggregatePatternOptions::ignoreProbabilisticByproducts)
    ).apply(instance, AggregatePatternOptions::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregatePatternOptions> STREAM_CODEC = StreamCodec.of(
            (buffer, options) -> buffer.writeByte(options.flags()),
            buffer -> fromFlags(buffer.readUnsignedByte()));

    public int flags() {
        return (splitSameItems ? 1 : 0)
                | (ignoreOutputComponents ? 2 : 0)
                | (skipProbabilisticMainOutput ? 4 : 0)
                | (ignoreProbabilisticByproducts ? 8 : 0);
    }

    public static AggregatePatternOptions fromFlags(int flags) {
        return new AggregatePatternOptions(
                (flags & 1) != 0,
                (flags & 2) != 0,
                (flags & 4) != 0,
                (flags & 8) != 0);
    }
}
