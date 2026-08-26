package io.github.langqi99.aeallpattern.client;

import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEBlocks;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternData;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.BlockPos;
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
        scan(runtime.orElseThrow(), event.getPos());
    }

    private static void scan(IJeiRuntime runtime, BlockPos pos) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ItemStack catalyst = minecraft.level.getBlockState(pos).getBlock().asItem().getDefaultInstance();
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
        for (IRecipeCategory<?> category : categories) {
            scanCategory(runtime, category, emptyFocus, recipes, seen);
            if (recipes.size() >= MAX_RECIPES) {
                break;
            }
        }
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
            Set<String> seen) {
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
            List<GenericStack> inputs = new ArrayList<>();
            boolean valid = true;
            for (IRecipeSlotView slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
                Optional<GenericStack> input = chooseStack(slot);
                if (input.isPresent()) {
                    if (kind == AggregatePatternKind.PROCESSING) {
                        merge(inputs, input.orElseThrow());
                    } else {
                        inputs.add(input.orElseThrow());
                    }
                } else if (!slot.isEmpty()) {
                    valid = false;
                    break;
                }
            }
            List<GenericStack> outputs = slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                    .map(ClientJeiAggregateScanner::chooseStack)
                    .flatMap(Optional::stream)
                    .limit(3)
                    .toList();
            if (!valid || inputs.isEmpty() || outputs.isEmpty() || inputs.size() > 9) {
                index++;
                continue;
            }

            String normalizedInputs = inputs.stream().map(ClientJeiAggregateScanner::normalize).sorted()
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
                        inputs,
                        outputs,
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

    private static Optional<GenericStack> chooseStack(IRecipeSlotView slot) {
        return slot.getAllIngredients()
                .map(ClientJeiAggregateScanner::toGenericStack)
                .flatMap(Optional::stream)
                .filter(stack -> stack.what() != null && stack.amount() > 0)
                .sorted(Comparator.comparing(ClientJeiAggregateScanner::normalize))
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

    private static void merge(List<GenericStack> stacks, GenericStack addition) {
        for (int index = 0; index < stacks.size(); index++) {
            GenericStack existing = stacks.get(index);
            if (existing.what().equals(addition.what())) {
                stacks.set(index, new GenericStack(existing.what(), Math.addExact(existing.amount(), addition.amount())));
                return;
            }
        }
        stacks.add(addition);
    }

    private static String normalize(GenericStack stack) {
        return stack.what().getType().getId() + "*" + stack.what().getId()
                + "*" + stack.amount() + "*" + stack.what();
    }

    private static void show(String key) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

}
