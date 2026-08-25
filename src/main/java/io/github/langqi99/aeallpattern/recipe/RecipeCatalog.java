package io.github.langqi99.aeallpattern.recipe;

import java.util.List;

public record RecipeCatalog(long generation, List<RecipeSnapshot> recipes, int filteredCount) {
    public RecipeCatalog {
        recipes = List.copyOf(recipes);
        if (generation < 1 || filteredCount < 0) {
            throw new IllegalArgumentException("invalid catalog metadata");
        }
    }
}
