package io.github.langqi99.aeallpattern.recipe;

import java.util.Objects;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Immutable, concrete single-input recipe exposed to AE2. */
public final class RecipeSnapshot {
    private final ResourceLocation recipeId;
    private final List<ItemStack> inputs;
    private final ItemStack output;
    private final RecipeFingerprint fingerprint;
    private final int processingTicks;

    public RecipeSnapshot(
            ResourceLocation recipeId,
            ItemStack input,
            ItemStack output,
            RecipeFingerprint fingerprint,
            int processingTicks) {
        this(recipeId, List.of(input), output, fingerprint, processingTicks);
    }

    public RecipeSnapshot(
            ResourceLocation recipeId,
            List<ItemStack> inputs,
            ItemStack output,
            RecipeFingerprint fingerprint,
            int processingTicks) {
        this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.isEmpty() || inputs.size() > 9) {
            throw new IllegalArgumentException("recipe must have between 1 and 9 inputs");
        }
        this.inputs = inputs.stream().map(stack -> requireStack(stack, "input")).toList();
        this.output = requireStack(output, "output");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.processingTicks = Math.max(1, processingTicks);
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    public ItemStack input() {
        return inputs.getFirst().copy();
    }

    public List<ItemStack> inputs() {
        return inputs.stream().map(ItemStack::copy).toList();
    }

    public ItemStack output() {
        return output.copy();
    }

    public RecipeFingerprint fingerprint() {
        return fingerprint;
    }

    public int processingTicks() {
        return processingTicks;
    }

    private static ItemStack requireStack(ItemStack stack, String name) {
        Objects.requireNonNull(stack, name);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return stack.copy();
    }
}
