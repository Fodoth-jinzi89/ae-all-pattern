package io.github.langqi99.aeallpattern.binding;

import io.github.langqi99.aeallpattern.recipe.RecipeFingerprint;
import java.util.Objects;
import java.util.UUID;

/** Identifies which binding/provider publishes a globally fingerprinted recipe. */
public record BindingPatternKey(UUID bindingId, RecipeFingerprint recipe) {
    public BindingPatternKey {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(recipe, "recipe");
    }
}
