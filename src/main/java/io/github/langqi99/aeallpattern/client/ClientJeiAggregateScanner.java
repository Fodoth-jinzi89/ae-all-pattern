package io.github.langqi99.aeallpattern.client;

import appeng.api.stacks.GenericStack;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternData;
import io.github.langqi99.aeallpattern.aggregate.AggregateInputSlot;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternKind;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternLibrary;
import io.github.langqi99.aeallpattern.aggregate.AggregateRecipe;
import io.github.langqi99.aeallpattern.compat.jei.AeAllPatternJeiPlugin;
import io.github.langqi99.aeallpattern.network.GenerateAggregatePayload;
import io.github.langqi99.aeallpattern.recipe.RecipeFingerprint;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModList;
import tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter;
import tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters;

/** Converts any JEI catalyst's visible recipes into concrete AE generic-stack patterns. */
public final class ClientJeiAggregateScanner {
    private static final int MAX_RECIPES = AggregatePatternData.MAX_RECIPES;
    private static final int MAX_EXPLICIT_ALTERNATIVES_PER_SLOT = AggregateInputSlot.MAX_ALTERNATIVES;
    private static long lastScanTick = Long.MIN_VALUE;
    private static BlockPos lastScanPos = BlockPos.ZERO;

    private ClientJeiAggregateScanner() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = player.level();
        if (!level.isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || !player.getItemInHand(event.getHand()).is(ModItems.ALL_PATTERN_GENERATOR.get())) {
            return;
        }
        if (level.getGameTime() == lastScanTick && event.getPos().equals(lastScanPos)) {
            return;
        }
        lastScanTick = level.getGameTime();
        lastScanPos = event.getPos().immutable();

        var runtime = AeAllPatternJeiPlugin.runtime();
        if (runtime.isEmpty()) {
            show("message.aeallpattern.generator.jei_not_ready");
            return;
        }
        scan(runtime.orElseThrow(), ClientRecipeMachineResolver.resolvePosition(level, event.getPos()));
    }

    private static void scan(IJeiRuntime runtime, BlockPos pos) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ItemStack catalyst = ClientRecipeMachineResolver.recipeViewerCatalyst(minecraft.level, pos);
        if (catalyst.isEmpty()) {
            show("message.aeallpattern.generator.no_jei_recipes");
            return;
        }

        var focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> catalystFocus = focusFactory.createFocus(
                RecipeIngredientRole.CATALYST, VanillaTypes.ITEM_STACK, catalyst);
        List<IRecipeCategory<?>> categories;
        if (catalyst.is(Blocks.CRAFTING_TABLE.asItem()) || AEBlocks.MOLECULAR_ASSEMBLER.is(catalyst)) {
            // JEI integrations do not consistently register the molecular assembler as a
            // vanilla crafting catalyst. It executes the same native AE crafting patterns.
            categories = List.of(runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING));
        } else {
            categories = runtime.getRecipeManager()
                    .createRecipeCategoryLookup()
                    .limitFocus(List.of(catalystFocus))
                    .get()
                    .toList();
        }
        if (categories.isEmpty()) {
            show("message.aeallpattern.generator.no_jei_recipes");
            return;
        }

        List<AggregateRecipe> recipes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        IFocusGroup emptyFocus = focusFactory.getEmptyFocusGroup();
        // A compatibility machine may register the clicked block as a catalyst for
        // its own category. A native JEI category is owned by the same namespace as
        // the clicked machine, so never fall back to an unrelated category.
        IRecipeCategory<?> category = findNativeCategory(categories, catalyst);
        if (category == null) {
            show("message.aeallpattern.generator.no_jei_recipes");
            return;
        }
        boolean chemicalOnly = isChemicalInputMachine(catalyst);
        scanCategory(runtime, category, emptyFocus, recipes, seen, chemicalOnly);
        if (recipes.isEmpty()) {
            show("message.aeallpattern.generator.no_item_recipes");
            return;
        }

        var machineBlock = minecraft.level.getBlockState(pos).getBlock();
        String machineKey = machineBlock.getDescriptionId();
        ResourceLocation catalystId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(machineBlock);
        UUID uploadId = UUID.randomUUID();
        int pageCount = (recipes.size() + AggregatePatternLibrary.PAGE_SIZE - 1)
                / AggregatePatternLibrary.PAGE_SIZE;
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int from = pageIndex * AggregatePatternLibrary.PAGE_SIZE;
            int to = Math.min(recipes.size(), from + AggregatePatternLibrary.PAGE_SIZE);
            PacketDistributor.sendToServer(new GenerateAggregatePayload(
                    uploadId, pos, catalystId, machineKey, pageIndex, pageCount,
                    recipes.size(), recipes.subList(from, to)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void scanCategory(
            IJeiRuntime runtime,
            IRecipeCategory category,
            IFocusGroup emptyFocus,
            List<AggregateRecipe> destination,
            Set<String> seen,
            boolean chemicalOnly) {
        IRecipeManager manager = runtime.getRecipeManager();
        ResourceLocation categoryId = category.getRecipeType().getUid();
        AggregatePatternKind kind = patternKind(categoryId);
        List<?> categoryRecipes = manager.createRecipeLookup(category.getRecipeType()).get().toList();
        int index = 0;
        for (Object recipe : categoryRecipes) {
            if (destination.size() >= MAX_RECIPES) {
                return;
            }
            var drawable = manager.createRecipeLayoutDrawable(category, recipe, emptyFocus);
            if (drawable.isEmpty()) {
                index++;
                continue;
            }
            mezz.jei.api.gui.IRecipeLayoutDrawable<?> layout =
                    (mezz.jei.api.gui.IRecipeLayoutDrawable<?>) drawable.orElseThrow();
            var slots = layout.getRecipeSlotsView();
            List<AggregateInputSlot> inputSlots = new ArrayList<>();
            boolean valid = true;
            List<IRecipeSlotView> inputViews = slots.getSlotViews(RecipeIngredientRole.INPUT);
            int alternativesPerSlot = Math.min(
                    MAX_EXPLICIT_ALTERNATIVES_PER_SLOT,
                    AggregateRecipe.MAX_TOTAL_INPUT_ALTERNATIVES / Math.max(1, inputViews.size()));
            for (IRecipeSlotView slot : inputViews) {
                Optional<AggregateInputSlot> input = chooseInputSlot(slot, alternativesPerSlot, chemicalOnly);
                if (input.isPresent()) {
                    inputSlots.add(input.orElseThrow());
                } else if (!chemicalOnly && !slot.isEmpty()) {
                    valid = false;
                    break;
                }
            }
            List<ScannedOutput> scannedOutputs = slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                    .map(ClientJeiAggregateScanner::scanOutput)
                    .flatMap(Optional::stream)
                    .limit(AggregateRecipe.MAX_OUTPUTS)
                    .toList();
            List<GenericStack> outputs = scannedOutputs.stream().map(ScannedOutput::stack).toList();
            if (!valid || inputSlots.isEmpty() || outputs.isEmpty()
                    || inputSlots.size() > AggregateRecipe.MAX_INPUTS) {
                index++;
                continue;
            }

            String normalizedInputs = inputSlots.stream().map(ClientJeiAggregateScanner::normalizeSlot).sorted()
                    .reduce("", (left, right) -> left + "|" + right);
            String normalizedOutputs = outputs.stream().map(ClientJeiAggregateScanner::normalize).sorted()
                    .reduce("", (left, right) -> left + "|" + right);
            ResourceLocation originalId = category.getRegistryName(recipe);
            if (kind != AggregatePatternKind.PROCESSING && originalId == null) {
                index++;
                continue;
            }
            String recipeIdentity = originalId == null ? categoryId + "#" + index : originalId.toString();
            RecipeFingerprint fingerprint = new RecipeFingerprint(
                    "jei:" + kind.serializedName() + ":" + categoryId,
                    recipeIdentity, normalizedInputs, normalizedOutputs, 1);
            String patternId = fingerprint.stableKey();
            if (seen.add(patternId)) {
                destination.add(new AggregateRecipe(
                        patternId,
                        originalId == null
                                ? ResourceLocation.fromNamespaceAndPath("aeallpattern", "jei/" + patternId.substring(0, 32))
                                : originalId,
                        kind,
                        inputSlots.stream().map(AggregateInputSlot::primary).toList(),
                        inputSlots,
                        outputs,
                        probabilisticOutputMask(scannedOutputs),
                        200));
            }
            index++;
        }
    }

    private static AggregatePatternKind patternKind(ResourceLocation categoryId) {
        if (categoryId.equals(RecipeTypes.CRAFTING.getUid())) {
            return AggregatePatternKind.CRAFTING;
        }
        if (categoryId.equals(RecipeTypes.STONECUTTING.getUid())) {
            return AggregatePatternKind.STONECUTTING;
        }
        if (categoryId.equals(RecipeTypes.SMITHING.getUid())) {
            return AggregatePatternKind.SMITHING;
        }
        return AggregatePatternKind.PROCESSING;
    }

    private static IRecipeCategory<?> findNativeCategory(
            List<IRecipeCategory<?>> categories, ItemStack catalyst) {
        ResourceLocation catalystId = BuiltInRegistries.ITEM.getKey(catalyst.getItem());
        return categories.stream()
                .filter(category -> category.getRecipeType().getUid().getNamespace()
                        .equals(catalystId.getNamespace()))
                .findFirst()
                .orElse(null);
    }

    private static Optional<GenericStack> chooseStack(IRecipeSlotView slot) {
        return slot.getAllIngredients()
                .map(ClientJeiAggregateScanner::toGenericStack)
                .flatMap(Optional::stream)
                .filter(stack -> stack.what() != null && stack.amount() > 0)
                .sorted(Comparator.comparing(ClientJeiAggregateScanner::normalize))
                .findFirst();
    }

    private static Optional<ScannedOutput> scanOutput(IRecipeSlotView slot) {
        return chooseStack(slot).map(stack -> new ScannedOutput(stack, isProbabilistic(slot)));
    }

    private static int probabilisticOutputMask(List<ScannedOutput> outputs) {
        int mask = 0;
        for (int index = 0; index < outputs.size(); index++) {
            if (outputs.get(index).probabilistic()) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    /**
     * JEI has no dedicated probability field, but recipe integrations expose chance information
     * through the output slot name or its tooltip callback. Inspect those semantic labels while
     * deliberately skipping the first tooltip line (the ingredient name) to avoid treating an item
     * whose own name contains "chance" as a probabilistic output.
     */
    private static boolean isProbabilistic(IRecipeSlotView slot) {
        if (slot.getSlotName().map(ClientJeiAggregateScanner::containsProbabilityMarker).orElse(false)) {
            return true;
        }
        if (!(slot instanceof IRecipeSlotDrawable drawable)) {
            return false;
        }
        try {
            List<Component> tooltip = drawable.getTooltip();
            for (int index = 1; index < tooltip.size(); index++) {
                Component line = tooltip.get(index);
                if (containsProbabilityMarker(line.getString())
                        || containsProbabilityMarker(line.getContents().toString())) {
                    return true;
                }
            }
        } catch (RuntimeException error) {
            io.github.langqi99.aeallpattern.AeAllPattern.LOGGER.debug(
                    "JEI output tooltip rejected probability inspection", error);
        }
        return false;
    }

    private static boolean containsProbabilityMarker(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("chance")
                || normalized.contains("probab")
                || normalized.contains("random output")
                || normalized.contains("概率")
                || normalized.contains("几率")
                || normalized.contains("機率")
                || normalized.contains("確率")
                || normalized.contains("확률");
    }

    private static Optional<AggregateInputSlot> chooseInputSlot(
            IRecipeSlotView slot, int alternativeLimit, boolean preferChemical) {
        LinkedHashMap<String, GenericStack> unique = new LinkedHashMap<>();
        List<GenericStack> converted = slot.getAllIngredients()
                .map(ClientJeiAggregateScanner::toGenericStack)
                .flatMap(Optional::stream)
                .filter(stack -> stack.what() != null && stack.amount() > 0)
                .sorted(Comparator.comparing(ClientJeiAggregateScanner::normalize))
                .limit(AggregateInputSlot.MAX_ALTERNATIVES)
                .toList();
        boolean slotHasChemical = preferChemical && converted.stream().anyMatch(ClientJeiAggregateScanner::isChemical);
        converted.stream()
                .filter(stack -> !slotHasChemical || isChemical(stack))
                .forEach(stack -> unique.putIfAbsent(normalize(stack), stack));
        if (unique.isEmpty()) {
            return Optional.empty();
        }
        List<GenericStack> candidates = List.copyOf(unique.values());
        Optional<ResourceLocation> itemTag = exactItemTag(candidates);
        if (itemTag.isPresent()) {
            // The tag will be resolved from the server's current datapack. Keep
            // only one concrete fallback so large tags never inflate packets.
            return Optional.of(new AggregateInputSlot(
                    List.of(candidates.getFirst()), itemTag));
        }
        return Optional.of(new AggregateInputSlot(
                candidates.stream().limit(alternativeLimit).toList(),
                Optional.empty()));
    }

    /**
     * Mekanism exposes these recipes through JEI as an item-or-chemical input
     * slot. AE patterns must select the chemical side for automated execution;
     * the item side is not a valid alternative for the machine's input handler.
     */
    private static boolean isChemicalInputMachine(ItemStack catalyst) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(catalyst.getItem());
        return id.getNamespace().equals("mekanism")
                && (id.getPath().equals("metallurgic_infuser")
                        || id.getPath().endsWith("_infusing_factory")
                        || id.getPath().equals("osmium_compressor")
                        || id.getPath().endsWith("_compressing_factory"));
    }

    private static boolean isChemical(GenericStack stack) {
        return stack.what().getType().getId().equals(
                ResourceLocation.fromNamespaceAndPath("appmek", "chemical"));
    }

    private static Optional<ResourceLocation> exactItemTag(List<GenericStack> candidates) {
        if (candidates.isEmpty()
                || candidates.stream().anyMatch(stack -> !(stack.what() instanceof AEItemKey))
                || candidates.stream().mapToLong(GenericStack::amount).distinct().count() != 1) {
            return Optional.empty();
        }
        Set<net.minecraft.world.item.Item> candidateItems = candidates.stream()
                .map(GenericStack::what)
                .map(AEItemKey.class::cast)
                .map(AEItemKey::getItem)
                .collect(java.util.stream.Collectors.toSet());
        return BuiltInRegistries.ITEM.getTagNames()
                .filter(tag -> BuiltInRegistries.ITEM.getTag(tag)
                        .map(named -> named.size() == candidateItems.size()
                                && named.stream().allMatch(holder -> candidateItems.contains(holder.value())))
                        .orElse(false))
                .map(net.minecraft.tags.TagKey::location)
                .sorted(Comparator.comparingInt((ResourceLocation id) -> id.toString().length())
                        .thenComparing(ResourceLocation::toString))
                .findFirst();
    }

    private static Optional<GenericStack> toGenericStack(ITypedIngredient<?> typed) {
        Object ingredient = typed.getIngredient();
        if (ingredient instanceof ItemStack item && !item.isEmpty()) {
            return Optional.of(GenericStack.fromItemStack(item.copy()));
        }
        if (ingredient instanceof FluidStack fluid && !fluid.isEmpty()) {
            return Optional.of(GenericStack.fromFluidStack(fluid.copy()));
        }
        return convertRegistered(typed);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<GenericStack> convertRegistered(ITypedIngredient typed) {
        if (!ModList.get().isLoaded("ae2jeiintegration")) {
            return Optional.empty();
        }
        try {
            IngredientConverter converter = IngredientConverters.getConverter(typed.getType());
            return converter == null
                    ? Optional.empty()
                    : Optional.ofNullable(converter.getStackFromIngredient(typed.getIngredient()));
        } catch (RuntimeException error) {
            io.github.langqi99.aeallpattern.AeAllPattern.LOGGER.debug(
                    "AE JEI converter rejected ingredient type {}", typed.getType(), error);
            return Optional.empty();
        }
    }

    private static String normalize(GenericStack stack) {
        return stack.what().getType().getId() + "*" + stack.what().getId()
                + "*" + stack.amount() + "*" + stack.what();
    }

    private static String normalizeSlot(AggregateInputSlot slot) {
        String tag = slot.itemTag().map(ResourceLocation::toString).orElse("-");
        return tag + slot.alternatives().stream()
                .map(ClientJeiAggregateScanner::normalize)
                .sorted()
                .reduce("", (left, right) -> left + "+" + right);
    }

    private static void show(String key) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private record ScannedOutput(GenericStack stack, boolean probabilistic) {
    }

}
