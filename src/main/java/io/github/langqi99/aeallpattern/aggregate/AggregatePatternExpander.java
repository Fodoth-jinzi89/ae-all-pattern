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
    static final int MAX_SPLIT_ITEM_INPUTS = 4096;

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
        AggregatePatternOptions options = aggregateStack.get(ModDataComponents.AGGREGATE_PATTERN_OPTIONS.get());
        if (options == null) {
            options = AggregatePatternOptions.DEFAULT;
        }

        List<IPatternDetails> expanded = new ArrayList<>(recipes.size());
        for (AggregateRecipe recipe : recipes) {
            try {
                if (options.skipProbabilisticMainOutput() && recipe.isProbabilisticOutput(0)) {
                    continue;
                }
                AggregateRecipe configuredRecipe = configureOutputs(recipe, options);
                List<AggregateInputSlot> configuredInputs = configuredProcessingInputs(
                        configuredRecipe, options, serverLevel);
                ItemStack encoded = encode(configuredRecipe, serverLevel);
                if (encoded.isEmpty()) {
                    AeAllPattern.LOGGER.debug(
                            "Cannot encode aggregate {} child {} as {}; skipping instead of changing its pattern type",
                            ref.libraryId(), recipe.recipeId(), recipe.kind());
                    continue;
                }
                IPatternDetails delegate = PatternDetailsHelper.decodePattern(encoded, level);
                if (delegate == null) {
                    // Third-party recipes can be visible to the scanner but not encodable by AE2.
                    // Skip them quietly; one rejected child must not flood the server log.
                    continue;
                }

                encoded.set(ModDataComponents.VIRTUAL_PATTERN_ID.get(),
                        virtualPatternId(recipe, options, configuredInputs));
                AEItemKey definition = AEItemKey.of(encoded);
                if (delegate instanceof IMolecularAssemblerSupportedPattern assemblerPattern) {
                    expanded.add(new AggregateAssemblerPatternDetails(
                            recipe.patternId(), definition, assemblerPattern, recipe.processingTicks()));
                } else {
                    expanded.add(new AggregatePatternDetails(
                            recipe.patternId(), definition, delegate, recipe.processingTicks(), configuredInputs));
                }
            } catch (RuntimeException error) {
                AeAllPattern.LOGGER.debug(
                        "Failed to expand aggregate child {} as {}", recipe.recipeId(), recipe.kind(), error);
            }
        }
        return List.copyOf(expanded);
    }

    private static AggregateRecipe configureOutputs(
            AggregateRecipe recipe, AggregatePatternOptions options) {
        if (!options.ignoreOutputComponents() && !options.ignoreProbabilisticByproducts()) {
            return recipe;
        }

        List<GenericStack> outputs = new ArrayList<>(recipe.outputs().size());
        int probabilisticOutputMask = 0;
        for (int sourceIndex = 0; sourceIndex < recipe.outputs().size(); sourceIndex++) {
            boolean probabilistic = recipe.isProbabilisticOutput(sourceIndex);
            if (sourceIndex > 0 && probabilistic && options.ignoreProbabilisticByproducts()) {
                continue;
            }
            GenericStack stack = recipe.outputs().get(sourceIndex);
            if (options.ignoreOutputComponents()
                    && stack.what() instanceof AEItemKey itemKey
                    && itemKey.hasComponents()) {
                stack = new GenericStack(AEItemKey.of(itemKey.getItem()), stack.amount());
            }
            if (probabilistic) {
                probabilisticOutputMask |= 1 << outputs.size();
            }
            outputs.add(stack);
        }
        return new AggregateRecipe(
                recipe.patternId(), recipe.recipeId(), recipe.kind(),
                recipe.inputs(), recipe.inputSlots(), outputs,
                probabilisticOutputMask, recipe.processingTicks());
    }

    /**
     * Resolves processing-slot alternatives at expansion time so tags follow the current datapack.
     * A custom input view is only needed for alternatives or unit splitting; exact legacy inputs keep
     * delegating to AE2 so their normal container-item semantics remain untouched.
     */
    private static List<AggregateInputSlot> configuredProcessingInputs(
            AggregateRecipe recipe,
            AggregatePatternOptions options,
            net.minecraft.server.level.ServerLevel level) {
        if (recipe.kind() != AggregatePatternKind.PROCESSING) {
            return List.of();
        }

        if (options.splitSameItems()) {
            List<AggregateInputSlot> result = new ArrayList<>();
            for (AggregateInputSlot slot : recipe.inputSlots()) {
                result.addAll(slot.splitUnits(level));
                if (result.size() > MAX_SPLIT_ITEM_INPUTS) {
                    throw new IllegalArgumentException(
                            "split item input count exceeds safety limit " + MAX_SPLIT_ITEM_INPUTS);
                }
            }
            return List.copyOf(result);
        }

        if (recipe.inputSlots().stream().noneMatch(AggregateInputSlot::hasAlternatives)) {
            return List.of();
        }
        return recipe.inputSlots().stream()
                .map(slot -> new AggregateInputSlot(slot.resolve(level), Optional.empty()))
                .toList();
    }

    /** Expands every item count n into exactly n independent unit inputs. */
    static List<GenericStack> splitItemInputs(List<GenericStack> inputs) {
        long expandedSize = 0;
        for (GenericStack input : inputs) {
            expandedSize += input.what() instanceof AEItemKey ? input.amount() : 1;
            if (expandedSize > MAX_SPLIT_ITEM_INPUTS) {
                throw new IllegalArgumentException(
                        "split item input count exceeds safety limit " + MAX_SPLIT_ITEM_INPUTS);
            }
        }

        List<GenericStack> expanded = new ArrayList<>((int) expandedSize);
        for (GenericStack input : inputs) {
            if (input.what() instanceof AEItemKey) {
                for (long index = 0; index < input.amount(); index++) {
                    expanded.add(new GenericStack(input.what(), 1));
                }
            } else {
                expanded.add(input);
            }
        }
        return List.copyOf(expanded);
    }

    private static String virtualPatternId(
            AggregateRecipe recipe,
            AggregatePatternOptions options,
            List<AggregateInputSlot> configuredInputs) {
        String value = "aggregate:" + options.flags() + ":" + recipe.patternId();
        if (!configuredInputs.isEmpty()) {
            // Makes refreshed providers observe datapack/tag membership changes without regenerating the item.
            value += ":inputs=" + Integer.toUnsignedString(configuredInputs.hashCode(), 16);
        }
        if (value.length() <= 160) {
            return value;
        }
        String suffix = Integer.toUnsignedString(value.hashCode(), 16);
        return value.substring(0, 159 - suffix.length()) + ":" + suffix;
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
        return PatternDetailsHelper.encodeCraftingPattern(holder.orElseThrow(), grid, output, true, false);
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
                castHolder(rawHolder.orElseThrow()), input, output, true);
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
                castHolder(rawHolder.orElseThrow()), template, base, addition, output, true);
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
