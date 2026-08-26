package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

public final class AggregatePatternExpander {
    private AggregatePatternExpander() {
    }

    public static List<IPatternDetails> expand(ItemStack aggregateStack, Level level) {
        AggregatePatternRef ref = aggregateStack.get(ModDataComponents.AGGREGATE_PATTERN.get());
        if (!aggregateStack.is(ModItems.AGGREGATE_PATTERN.get()) || ref == null
                || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return List.of();
        }

        List<AggregateRecipe> recipes = AggregatePatternLibrary.get(serverLevel.getServer())
                .recipes(serverLevel.getServer(), ref.libraryId()).orElse(List.of());

        List<IPatternDetails> expanded = new ArrayList<>(recipes.size());
        for (AggregateRecipe recipe : recipes) {
            try {
                ItemStack encoded = encode(recipe, serverLevel);
                if (encoded.isEmpty()) {
                    AeAllPattern.LOGGER.debug(
                            "Cannot encode aggregate {} child {} as {}; skipping instead of changing its pattern type",
                            ref.libraryId(), recipe.recipeId(), recipe.kind());
                    continue;
                }
                IPatternDetails delegate = PatternDetailsHelper.decodePattern(encoded, level);
                if (delegate == null) {
                    AeAllPattern.LOGGER.warn("AE2 rejected aggregate child pattern {}", recipe.recipeId());
                    continue;
                }

                encoded.set(ModDataComponents.VIRTUAL_PATTERN_ID.get(),
                        "aggregate:" + recipe.patternId());
                AEItemKey definition = AEItemKey.of(encoded);
                if (delegate instanceof IMolecularAssemblerSupportedPattern assemblerPattern) {
                    expanded.add(new AggregateAssemblerPatternDetails(
                            recipe.patternId(), definition, assemblerPattern, recipe.processingTicks()));
                } else {
                    expanded.add(new AggregatePatternDetails(
                            recipe.patternId(), definition, delegate, recipe.processingTicks()));
                }
            } catch (RuntimeException error) {
                AeAllPattern.LOGGER.warn(
                        "Failed to expand aggregate child {} as {}", recipe.recipeId(), recipe.kind(), error);
            }
        }
        return List.copyOf(expanded);
    }

    private static ItemStack encode(
            AggregateRecipe recipe, net.minecraft.server.level.ServerLevel level) {
        return switch (recipe.kind()) {
            case PROCESSING -> PatternDetailsHelper.encodeProcessingPattern(recipe.inputs(), recipe.outputs());
            case CRAFTING -> encodeCrafting(recipe, level);
            case STONECUTTING -> encodeStonecutting(recipe, level);
            case SMITHING -> encodeSmithing(recipe, level);
        };
    }

    private static ItemStack encodeCrafting(
            AggregateRecipe aggregate, net.minecraft.server.level.ServerLevel level) {
        ItemStack[] storedGrid = storedCraftingGrid(aggregate.inputs());
        Optional<RecipeHolder<CraftingRecipe>> holder = craftingHolder(aggregate, storedGrid, level);
        if (holder.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CraftingRecipe recipe = holder.orElseThrow().value();
        ItemStack[] grid = recipe.getIngredients().isEmpty() ? storedGrid : craftingGrid(recipe);
        if (Arrays.stream(grid).allMatch(ItemStack::isEmpty)) {
            return ItemStack.EMPTY;
        }
        ItemStack output = itemStack(aggregate.outputs().getFirst());
        if (output.isEmpty()) {
            output = recipe.getResultItem(level.registryAccess()).copy();
        }
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return PatternDetailsHelper.encodeCraftingPattern(holder.orElseThrow(), grid, output, false, false);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> craftingHolder(
            AggregateRecipe aggregate,
            ItemStack[] storedGrid,
            net.minecraft.server.level.ServerLevel level) {
        Optional<RecipeHolder<?>> byId = level.getRecipeManager().byKey(aggregate.recipeId());
        if (byId.isPresent() && byId.orElseThrow().value() instanceof CraftingRecipe) {
            return Optional.of(castHolder(byId.orElseThrow()));
        }
        if (Arrays.stream(storedGrid).allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING,
                CraftingInput.of(3, 3, List.of(storedGrid)),
                level);
    }

    private static ItemStack encodeStonecutting(
            AggregateRecipe aggregate, net.minecraft.server.level.ServerLevel level) {
        Optional<RecipeHolder<?>> rawHolder = level.getRecipeManager().byKey(aggregate.recipeId());
        if (rawHolder.isEmpty() || !(rawHolder.orElseThrow().value() instanceof StonecutterRecipe)) {
            return ItemStack.EMPTY;
        }
        AEItemKey input = itemKey(aggregate.inputs().getFirst());
        AEItemKey output = itemKey(aggregate.outputs().getFirst());
        if (input == null || output == null) {
            return ItemStack.EMPTY;
        }
        return PatternDetailsHelper.encodeStonecuttingPattern(
                castHolder(rawHolder.orElseThrow()), input, output, false);
    }

    private static ItemStack encodeSmithing(
            AggregateRecipe aggregate, net.minecraft.server.level.ServerLevel level) {
        Optional<RecipeHolder<?>> rawHolder = level.getRecipeManager().byKey(aggregate.recipeId());
        if (rawHolder.isEmpty() || !(rawHolder.orElseThrow().value() instanceof SmithingRecipe recipe)) {
            return ItemStack.EMPTY;
        }
        AEItemKey template = findItemKey(aggregate.inputs(), recipe::isTemplateIngredient);
        AEItemKey base = findItemKey(aggregate.inputs(), recipe::isBaseIngredient);
        AEItemKey addition = findItemKey(aggregate.inputs(), recipe::isAdditionIngredient);
        AEItemKey output = itemKey(aggregate.outputs().getFirst());
        if (template == null || base == null || addition == null || output == null) {
            return ItemStack.EMPTY;
        }
        return PatternDetailsHelper.encodeSmithingTablePattern(
                castHolder(rawHolder.orElseThrow()), template, base, addition, output, false);
    }

    private static ItemStack[] craftingGrid(CraftingRecipe recipe) {
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    int ingredientIndex = row * width + column;
                    if (ingredientIndex < ingredients.size()) {
                        grid[row * 3 + column] = chooseItem(ingredients.get(ingredientIndex));
                    }
                }
            }
        } else {
            for (int index = 0; index < Math.min(grid.length, ingredients.size()); index++) {
                grid[index] = chooseItem(ingredients.get(index));
            }
        }
        return grid;
    }

    private static ItemStack[] storedCraftingGrid(List<GenericStack> inputs) {
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        for (int index = 0; index < Math.min(grid.length, inputs.size()); index++) {
            AEItemKey key = itemKey(inputs.get(index));
            if (key == null) {
                Arrays.fill(grid, ItemStack.EMPTY);
                return grid;
            }
            grid[index] = key.toStack(1);
        }
        return grid;
    }

    private static ItemStack chooseItem(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty())
                .min(Comparator.comparing(AggregatePatternExpander::itemIdentity))
                .map(stack -> stack.copyWithCount(1))
                .orElse(ItemStack.EMPTY);
    }

    private static String itemIdentity(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "*" + stack.getComponents();
    }

    private static AEItemKey findItemKey(List<GenericStack> stacks, Predicate<ItemStack> predicate) {
        for (GenericStack stack : stacks) {
            AEItemKey key = itemKey(stack);
            if (key != null && predicate.test(key.getReadOnlyStack())) {
                return key;
            }
        }
        return null;
    }

    private static AEItemKey itemKey(GenericStack stack) {
        return stack.what() instanceof AEItemKey itemKey ? itemKey : null;
    }

    private static ItemStack itemStack(GenericStack stack) {
        AEItemKey key = itemKey(stack);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        return key.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeHolder<T> castHolder(
            RecipeHolder<?> holder) {
        return (RecipeHolder<T>) holder;
    }
}
