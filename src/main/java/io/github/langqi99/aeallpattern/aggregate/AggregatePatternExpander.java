package io.github.langqi99.aeallpattern.aggregate;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
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
    private static final ResourceLocation CHEMICAL_KEY_TYPE =
            ResourceLocation.fromNamespaceAndPath("appmek", "chemical");
    static final TagKey<Item> PROCESSING_CATALYSTS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("aeallpattern", "processing_catalysts"));

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
        AggregatePatternOptions savedOptions =
                aggregateStack.get(ModDataComponents.AGGREGATE_PATTERN_OPTIONS.get());
        AggregatePatternOptions options = savedOptions == null ? AggregatePatternOptions.DEFAULT : savedOptions;

        List<IPatternDetails> expanded = new ArrayList<>(recipes.size());
        for (AggregateRecipe recipe : recipes) {
            try {
                IPatternDetails details = expandRecipe(
                        recipe, options, serverLevel, "aggregate:" + recipe.patternId());
                if (details != null) {
                    expanded.add(details);
                }
            } catch (RuntimeException error) {
                AeAllPattern.LOGGER.debug(
                        "Failed to expand aggregate child {} as {}", recipe.recipeId(), recipe.kind(), error);
            }
        }
        return List.copyOf(expanded);
    }

    /** Shared child-pattern path used by aggregate items and live linker providers. */
    public static IPatternDetails expandRecipe(
            AggregateRecipe recipe,
            AggregatePatternOptions options,
            net.minecraft.server.level.ServerLevel level,
            String virtualIdPrefix) {
        if (options.skipProbabilisticMainOutput() && recipe.isProbabilisticOutput(0)) {
            return null;
        }
        if (recipe.kind() == AggregatePatternKind.PROCESSING
                && recipe.outputs().stream().allMatch(stack -> removeOutput(stack.what(), options))) {
            return null;
        }
        AggregateRecipe configuredRecipe = configureOutputs(recipe, options);
        List<AggregateInputSlot> configuredInputs = configuredProcessingInputs(
                configuredRecipe, options, level);
        if (filtersProcessingInputs(options)
                && configuredRecipe.kind() == AggregatePatternKind.PROCESSING
                && configuredInputs.isEmpty()) {
            return null;
        }
        ItemStack encoded = encode(configuredRecipe, level, options);
        if (encoded.isEmpty()) {
            return null;
        }
        IPatternDetails delegate = PatternDetailsHelper.decodePattern(encoded, level);
        if (delegate == null) {
            return null;
        }

        encoded.set(ModDataComponents.VIRTUAL_PATTERN_ID.get(),
                virtualPatternId(virtualIdPrefix, options, configuredInputs));
        AEItemKey definition = AEItemKey.of(encoded);
        if (delegate instanceof IMolecularAssemblerSupportedPattern assemblerPattern) {
            return new AggregateAssemblerPatternDetails(
                    recipe.patternId(), definition, assemblerPattern, recipe.processingTicks());
        }
        return new AggregatePatternDetails(
                recipe.patternId(), definition, delegate, recipe.processingTicks(), configuredInputs);
    }

    private static AggregateRecipe configureOutputs(
            AggregateRecipe recipe, AggregatePatternOptions options) {
        if (!options.ignoreOutputComponents()
                && !options.ignoreProbabilisticByproducts()
                && !options.removeOutputFluids()
                && !options.removeOutputChemicals()) {
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
            if (recipe.kind() == AggregatePatternKind.PROCESSING && removeOutput(stack.what(), options)) {
                continue;
            }
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

        boolean filtersInputs = filtersProcessingInputs(options);
        List<AggregateInputSlot> slots = filtersInputs
                ? recipe.inputSlots().stream()
                        .filter(slot -> !removeInput(slot, options, level))
                        .toList()
                : recipe.inputSlots();

        if (options.splitSameItems()) {
            List<AggregateInputSlot> result = new ArrayList<>();
            for (AggregateInputSlot slot : slots) {
                result.addAll(slot.splitUnits(level));
                if (result.size() > MAX_SPLIT_ITEM_INPUTS) {
                    throw new IllegalArgumentException(
                            "split item input count exceeds safety limit " + MAX_SPLIT_ITEM_INPUTS);
                }
            }
            return List.copyOf(result);
        }

        if (!filtersInputs
                && slots.stream().noneMatch(AggregateInputSlot::hasAlternatives)) {
            return List.of();
        }
        return slots.stream()
                .map(slot -> new AggregateInputSlot(slot.resolve(level), Optional.empty()))
                .toList();
    }

    private static boolean filtersProcessingInputs(AggregatePatternOptions options) {
        return options.removeProcessingCatalysts()
                || options.removeInputFluids()
                || options.removeInputChemicals();
    }

    private static boolean removeInput(
            AggregateInputSlot slot, AggregatePatternOptions options, Level level) {
        return slot.resolve(level).stream().allMatch(stack -> {
            AEKey key = stack.what();
            return options.removeInputFluids() && key instanceof AEFluidKey
                    || options.removeInputChemicals() && isChemical(key)
                    || options.removeProcessingCatalysts()
                            && key instanceof AEItemKey itemKey
                            && itemKey.toStack().is(PROCESSING_CATALYSTS);
        });
    }

    private static boolean removeOutput(AEKey key, AggregatePatternOptions options) {
        return options.removeOutputFluids() && key instanceof AEFluidKey
                || options.removeOutputChemicals() && isChemical(key);
    }

    private static boolean isChemical(AEKey key) {
        return key.getType().getId().equals(CHEMICAL_KEY_TYPE);
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
            String prefix,
            AggregatePatternOptions options,
            List<AggregateInputSlot> configuredInputs) {
        String value = prefix + ":options=" + options.flags();
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
            AggregateRecipe recipe,
            net.minecraft.server.level.ServerLevel level,
            AggregatePatternOptions options) {
        return switch (recipe.kind()) {
            case PROCESSING -> PatternDetailsHelper.encodeProcessingPattern(recipe.inputs(), recipe.outputs());
            case CRAFTING -> encodeCrafting(recipe, level, options);
            case STONECUTTING -> encodeStonecutting(recipe, level, options);
            case SMITHING -> encodeSmithing(recipe, level, options);
        };
    }

    private static ItemStack encodeCrafting(
            AggregateRecipe aggregate,
            net.minecraft.server.level.ServerLevel level,
            AggregatePatternOptions options) {
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
        return PatternDetailsHelper.encodeCraftingPattern(
                holder.orElseThrow(), grid, output,
                options.allowItemSubstitutions(), options.allowFluidSubstitutions());
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
            AggregateRecipe aggregate,
            net.minecraft.server.level.ServerLevel level,
            AggregatePatternOptions options) {
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
                castHolder(rawHolder.orElseThrow()), input, output, options.allowItemSubstitutions());
    }

    private static ItemStack encodeSmithing(
            AggregateRecipe aggregate,
            net.minecraft.server.level.ServerLevel level,
            AggregatePatternOptions options) {
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
                castHolder(rawHolder.orElseThrow()), template, base, addition, output,
                options.allowItemSubstitutions());
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
