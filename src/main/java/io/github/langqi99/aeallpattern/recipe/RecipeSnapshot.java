package io.github.langqi99.aeallpattern.recipe;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Immutable, concrete single-input recipe exposed to AE2. */
public final class RecipeSnapshot {
    private final ResourceLocation recipeId;
    private final ItemStack input;
    private final ItemStack output;
    private final RecipeFingerprint fingerprint;
    private final int processingTicks;

    public RecipeSnapshot(
            ResourceLocation recipeId,
            ItemStack input,
            ItemStack output,
            RecipeFingerprint fingerprint,
            int processingTicks) {
        this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
        this.input = requireStack(input, "input");
        this.output = requireStack(output, "output");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.processingTicks = Math.max(1, processingTicks);
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    public ItemStack input() {
        return input.copy();
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
