package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.langqi99.aeallpattern.recipe.RecipeSnapshot;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** One concrete native AE2 recipe stored inside an aggregate pattern. */
public record AggregateRecipe(
        String patternId,
        ResourceLocation recipeId,
        AggregatePatternKind kind,
        List<GenericStack> inputs,
        List<AggregateInputSlot> inputSlots,
        List<GenericStack> outputs,
        int probabilisticOutputMask,
        int processingTicks) {
    private static final Codec<List<GenericStack>> INPUTS_CODEC = GenericStack.CODEC.listOf()
            .validate(inputs -> validateStacks(inputs, 9, "inputs"));
    private static final Codec<List<GenericStack>> OUTPUTS_CODEC = GenericStack.CODEC.listOf()
            .validate(outputs -> validateStacks(outputs, 3, "outputs"));
    private static final Codec<List<AggregateInputSlot>> INPUT_SLOTS_CODEC = AggregateInputSlot.CODEC.listOf()
            .validate(AggregateRecipe::validateInputSlots);

    public static final Codec<AggregateRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pattern_id").forGetter(AggregateRecipe::patternId),
            ResourceLocation.CODEC.fieldOf("recipe_id").forGetter(AggregateRecipe::recipeId),
            AggregatePatternKind.CODEC.optionalFieldOf("kind", AggregatePatternKind.PROCESSING)
                    .forGetter(AggregateRecipe::kind),
            INPUTS_CODEC.fieldOf("inputs").forGetter(AggregateRecipe::inputs),
            INPUT_SLOTS_CODEC.optionalFieldOf("input_slots", List.of()).forGetter(AggregateRecipe::inputSlots),
            OUTPUTS_CODEC.fieldOf("outputs").forGetter(AggregateRecipe::outputs),
            Codec.INT.optionalFieldOf("probabilistic_output_mask", 0)
                    .forGetter(AggregateRecipe::probabilisticOutputMask),
            Codec.INT.optionalFieldOf("processing_ticks", 1).forGetter(AggregateRecipe::processingTicks)
    ).apply(instance, AggregateRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateRecipe> STREAM_CODEC = StreamCodec.of(
            AggregateRecipe::encode, AggregateRecipe::decode);

    public AggregateRecipe {
        if (patternId == null || patternId.isBlank() || patternId.length() > 160) {
            throw new IllegalArgumentException("invalid aggregate pattern id");
        }
        inputs = copyAndValidate(inputs, 9, "inputs");
        inputSlots = inputSlots == null || inputSlots.isEmpty()
                ? inputs.stream().map(AggregateInputSlot::exact).toList()
                : copyAndValidateSlots(inputSlots);
        inputs = inputSlots.stream().map(AggregateInputSlot::primary).toList();
        outputs = copyAndValidate(outputs, 3, "outputs");
        probabilisticOutputMask &= (1 << outputs.size()) - 1;
        processingTicks = Math.max(1, processingTicks);
    }

    /** Compatibility constructor for callers that do not provide probability metadata. */
    public AggregateRecipe(
            String patternId,
            ResourceLocation recipeId,
            AggregatePatternKind kind,
            List<GenericStack> inputs,
            List<AggregateInputSlot> inputSlots,
            List<GenericStack> outputs,
            int processingTicks) {
        this(patternId, recipeId, kind, inputs, inputSlots, outputs, 0, processingTicks);
    }

    /** Compatibility constructor for existing processing-only callers and saved data. */
    public AggregateRecipe(
            String patternId,
            ResourceLocation recipeId,
            AggregatePatternKind kind,
            List<GenericStack> inputs,
            List<GenericStack> outputs,
            int processingTicks) {
        this(patternId, recipeId, kind, inputs, List.of(), outputs, 0, processingTicks);
    }

    /** Compatibility constructor for existing processing-only callers and saved data. */
    public AggregateRecipe(
            String patternId,
            ResourceLocation recipeId,
            List<GenericStack> inputs,
            List<GenericStack> outputs,
            int processingTicks) {
        this(patternId, recipeId, AggregatePatternKind.PROCESSING, inputs, List.of(), outputs, 0, processingTicks);
    }

    public static AggregateRecipe from(RecipeSnapshot snapshot) {
        return new AggregateRecipe(
                snapshot.fingerprint().stableKey(),
                snapshot.recipeId(),
                snapshot.inputs().stream().map(GenericStack::fromItemStack).toList(),
                List.of(GenericStack.fromItemStack(snapshot.output())),
                snapshot.processingTicks());
    }

    public boolean isProbabilisticOutput(int index) {
        return index >= 0 && index < outputs.size() && (probabilisticOutputMask & (1 << index)) != 0;
    }

    private static DataResult<List<GenericStack>> validateStacks(
            List<GenericStack> stacks, int maximum, String name) {
        if (stacks.isEmpty() || stacks.size() > maximum) {
            return DataResult.error(() -> "aggregate recipe must have 1-" + maximum + " " + name);
        }
        if (stacks.stream().anyMatch(stack -> stack == null || stack.what() == null || stack.amount() <= 0)) {
            return DataResult.error(() -> "aggregate recipe " + name + " must be non-empty");
        }
        return DataResult.success(stacks);
    }

    private static DataResult<List<AggregateInputSlot>> validateInputSlots(List<AggregateInputSlot> slots) {
        try {
            return DataResult.success(copyAndValidateSlots(slots));
        } catch (IllegalArgumentException error) {
            return DataResult.error(error::getMessage);
        }
    }

    private static List<AggregateInputSlot> copyAndValidateSlots(List<AggregateInputSlot> slots) {
        if (slots == null || slots.isEmpty() || slots.size() > 9) {
            throw new IllegalArgumentException("aggregate recipe must have 1-9 input slots");
        }
        List<AggregateInputSlot> result = List.copyOf(slots);
        if (result.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("aggregate recipe input slots must be non-empty");
        }
        int totalAlternatives = result.stream().mapToInt(slot -> slot.alternatives().size()).sum();
        if (totalAlternatives > 512) {
            throw new IllegalArgumentException("aggregate recipe has too many explicit input alternatives");
        }
        return result;
    }

    private static List<GenericStack> copyAndValidate(
            List<GenericStack> stacks, int maximum, String name) {
        if (stacks == null || stacks.isEmpty() || stacks.size() > maximum) {
            throw new IllegalArgumentException("aggregate recipe must have 1-" + maximum + " " + name);
        }
        List<GenericStack> result = List.copyOf(stacks);
        if (result.stream().anyMatch(stack -> stack == null || stack.what() == null || stack.amount() <= 0)) {
            throw new IllegalArgumentException("aggregate recipe " + name + " must be non-empty");
        }
        return result;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AggregateRecipe recipe) {
        buffer.writeUtf(recipe.patternId(), 160);
        buffer.writeResourceLocation(recipe.recipeId());
        buffer.writeEnum(recipe.kind());
        buffer.writeVarInt(recipe.inputSlots.size());
        recipe.inputSlots.forEach(slot -> AggregateInputSlot.STREAM_CODEC.encode(buffer, slot));
        buffer.writeVarInt(recipe.outputs.size());
        recipe.outputs.forEach(stack -> GenericStack.STREAM_CODEC.encode(buffer, stack));
        buffer.writeByte(recipe.probabilisticOutputMask);
        buffer.writeVarInt(recipe.processingTicks());
    }

    private static AggregateRecipe decode(RegistryFriendlyByteBuf buffer) {
        String patternId = buffer.readUtf(160);
        ResourceLocation recipeId = buffer.readResourceLocation();
        AggregatePatternKind kind = buffer.readEnum(AggregatePatternKind.class);
        int inputCount = checkedCount(buffer.readVarInt(), 9, "input");
        List<AggregateInputSlot> inputSlots = java.util.stream.IntStream.range(0, inputCount)
                .mapToObj(index -> AggregateInputSlot.STREAM_CODEC.decode(buffer)).toList();
        int outputCount = checkedCount(buffer.readVarInt(), 3, "output");
        List<GenericStack> outputs = java.util.stream.IntStream.range(0, outputCount)
                .mapToObj(index -> GenericStack.STREAM_CODEC.decode(buffer)).toList();
        int probabilisticOutputMask = buffer.readUnsignedByte();
        return new AggregateRecipe(
                patternId, recipeId, kind,
                inputSlots.stream().map(AggregateInputSlot::primary).toList(),
                inputSlots, outputs, probabilisticOutputMask, buffer.readVarInt());
    }

    private static int checkedCount(int count, int maximum, String name) {
        if (count < 1 || count > maximum) {
            throw new IllegalArgumentException("invalid aggregate " + name + " count: " + count);
        }
        return count;
    }
}
