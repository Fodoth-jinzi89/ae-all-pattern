package io.github.langqi99.aeallpattern.machine;

import appeng.api.stacks.AEItemKey;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.BindingRecord;
import io.github.langqi99.aeallpattern.recipe.RecipeCatalog;
import io.github.langqi99.aeallpattern.recipe.RecipeFingerprint;
import io.github.langqi99.aeallpattern.recipe.RecipeSnapshot;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Optional, dependency-free bridge to PackagedAuto's atomic crafting-machine protocol. */
final class PackagedCraftingAdapter implements MachineAdapter {
    private static final ResourceLocation ID = id("aeallpattern", "packaged_crafting");
    private static final int MAX_PATTERNS = 4096;
    private static final Map<ResourceLocation, Spec> MACHINES = machineSpecs();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int schemaVersion() {
        return 1;
    }

    @Override
    public boolean supports(ServerLevel level, BlockEntity target) {
        return MACHINES.containsKey(BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()));
    }

    @Override
    public RecipeCatalog discoverRecipes(ServerLevel level, BlockEntity target, long generation) {
        Spec spec = spec(target);
        if (spec == null) {
            return new RecipeCatalog(generation, List.of(), 0);
        }
        RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.getOptional(spec.recipeType).orElse(null);
        if (recipeType == null) {
            return new RecipeCatalog(generation, List.of(), 0);
        }
        List<RecipeSnapshot> snapshots = new ArrayList<>();
        int filtered = 0;
        List<RecipeHolder<?>> holders = recipes(level, recipeType);
        for (RecipeHolder<?> holder : holders.stream()
                .sorted(Comparator.comparing(candidate -> candidate.id().toString())).toList()) {
            if (snapshots.size() >= MAX_PATTERNS) {
                filtered++;
                continue;
            }
            try {
                Recipe<?> recipe = holder.value();
                if (spec.tier > 0 && invokeInt(recipe, "getTier") != spec.tier) {
                    continue;
                }
                RecipeLayout layout = layout(spec, recipe);
                Object info = createRecipeInfo(spec, holder.id(), layout, layout.primaryInputs());
                if (!invokeBoolean(info, "isValid")) {
                    filtered++;
                    continue;
                }
                ItemStack output = firstOutput(info);
                if (output.isEmpty()) {
                    filtered++;
                    continue;
                }
                String normalizedInput = layout.inputAlternatives.stream()
                        .map(alternatives -> alternatives.stream().map(PackagedCraftingAdapter::normalize).toList().toString())
                        .toList().toString();
                RecipeFingerprint fingerprint = new RecipeFingerprint(
                        id().toString(), holder.id().toString(), normalizedInput, normalize(output), schemaVersion());
                snapshots.add(RecipeSnapshot.withAlternatives(
                        holder.id(), layout.inputAlternatives, output, fingerprint, processingTicks(info)));
            } catch (ReflectiveOperationException | RuntimeException error) {
                filtered++;
                AeAllPattern.LOGGER.debug("Skipping unsupported packaged recipe {}", holder.id(), error);
            }
        }
        snapshots.sort(Comparator.comparing(snapshot -> snapshot.fingerprint().stableKey()));
        return new RecipeCatalog(generation, snapshots, filtered);
    }

    @Override
    public boolean insert(ServerLevel level, BindingRecord binding, ItemStack stack) {
        return false;
    }

    @Override
    public boolean insertRecipe(
            ServerLevel level, BindingRecord binding, RecipeSnapshot snapshot, List<ItemStack> inputs) {
        BlockEntity target = level.getBlockEntity(binding.target().pos());
        Spec spec = target == null ? null : spec(target);
        if (spec == null) {
            return false;
        }
        try {
            RecipeHolder<?> holder = level.getRecipeManager().byKey(snapshot.recipeId()).orElse(null);
            if (holder == null || (spec.tier > 0 && invokeInt(holder.value(), "getTier") != spec.tier)) {
                return false;
            }
            RecipeLayout layout = layout(spec, holder.value());
            if (inputs.size() != layout.inputAlternatives.size()) {
                return false;
            }
            Object info = createRecipeInfo(spec, holder.id(), layout, inputs);
            ItemStack actualOutput = firstOutput(info);
            ItemStack expectedOutput = snapshot.output();
            if (!invokeBoolean(info, "isValid")
                    || !ItemStack.isSameItemSameComponents(actualOutput, expectedOutput)
                    || actualOutput.getCount() != expectedOutput.getCount()) {
                return false;
            }
            Method accept = java.util.Arrays.stream(target.getClass().getMethods())
                    .filter(method -> method.getName().equals("acceptPackage") && method.getParameterCount() == 3)
                    .findFirst().orElse(null);
            if (accept == null) {
                return false;
            }
            @SuppressWarnings("unchecked")
            List<ItemStack> condensed = (List<ItemStack>) info.getClass().getMethod("getInputs").invoke(info);
            return Boolean.TRUE.equals(accept.invoke(target, info, condensed, binding.clickedSide()));
        } catch (ReflectiveOperationException | RuntimeException error) {
            AeAllPattern.LOGGER.debug("Packaged machine rejected recipe {}", snapshot.recipeId(), error);
            return false;
        }
    }

    @Override
    public ItemStack extractAnyOutput(ServerLevel level, BindingRecord binding, boolean simulate) {
        for (Direction side : preferredFirst(binding.clickedSide())) {
            ItemStack extracted = ItemHandlerTransfer.extractAny(level.getCapability(
                    Capabilities.ItemHandler.BLOCK, binding.target().pos(), side), simulate);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemHandlerTransfer.extractAny(level.getCapability(
                Capabilities.ItemHandler.BLOCK, binding.target().pos(), null), simulate);
    }

    private static RecipeLayout layout(Spec spec, Recipe<?> recipe) throws ReflectiveOperationException {
        List<Ingredient> ingredients = new ArrayList<>();
        boolean combination = spec.kind == Kind.COMBINATION;
        if (combination) {
            ingredients.add((Ingredient) recipe.getClass().getMethod("getInput").invoke(recipe));
        }
        ingredients.addAll(recipe.getIngredients());

        List<List<ItemStack>> alternatives = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        int offset = combination ? 1 : 0;
        for (int index = 0; index < ingredients.size(); index++) {
            Ingredient ingredient = ingredients.get(index);
            ItemStack[] variants = ingredient.getItems();
            if (variants.length == 0) {
                continue;
            }
            if (variants.length > 32) {
                throw new IllegalArgumentException("ingredient has more than 32 alternatives");
            }
            List<ItemStack> choices = java.util.Arrays.stream(variants)
                    .map(stack -> stack.copyWithCount(Math.max(1, stack.getCount())))
                    .sorted(Comparator.comparing(PackagedCraftingAdapter::normalize))
                    .toList();
            alternatives.add(choices);
            positions.add(index - offset);
        }
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("recipe has no item inputs");
        }
        int width = combination ? 0 : invokeIntOr(recipe, "getWidth", spec.gridSize);
        int height = combination ? 0 : invokeIntOr(recipe, "getHeight", spec.gridSize);
        return new RecipeLayout(List.copyOf(alternatives), List.copyOf(positions), width, height);
    }

    private static Object createRecipeInfo(
            Spec spec, ResourceLocation recipeId, RecipeLayout layout, List<ItemStack> supplied)
            throws ReflectiveOperationException {
        Class<?> infoClass = Class.forName(spec.recipeInfoClass);
        if (spec.kind == Kind.COMBINATION) {
            Constructor<?> constructor = infoClass.getConstructor(ResourceLocation.class, ItemStack.class, List.class);
            return constructor.newInstance(recipeId, supplied.getFirst().copy(), copy(supplied.subList(1, supplied.size())));
        }
        int size = Math.multiplyExact(layout.width, layout.height);
        List<ItemStack> matrix = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
        for (int index = 0; index < supplied.size(); index++) {
            int position = layout.positions.get(index);
            if (position < 0 || position >= matrix.size()) {
                throw new IllegalArgumentException("recipe input lies outside its crafting grid");
            }
            matrix.set(position, supplied.get(index).copy());
        }
        Constructor<?> constructor = infoClass.getConstructor(
                ResourceLocation.class, int.class, int.class, List.class);
        return constructor.newInstance(recipeId, layout.width, layout.height, matrix);
    }

    private static ItemStack firstOutput(Object info) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        List<ItemStack> outputs = (List<ItemStack>) info.getClass().getMethod("getOutputs").invoke(info);
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    private static int processingTicks(Object info) {
        try {
            return Math.max(1, invokeInt(info, "getTimeRequired"));
        } catch (ReflectiveOperationException ignored) {
            return 200;
        }
    }

    private static int invokeInt(Object target, String method) throws ReflectiveOperationException {
        return ((Number) target.getClass().getMethod(method).invoke(target)).intValue();
    }

    private static int invokeIntOr(Object target, String method, int fallback) {
        try {
            return invokeInt(target, method);
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static boolean invokeBoolean(Object target, String method) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(target.getClass().getMethod(method).invoke(target));
    }

    private static Spec spec(BlockEntity target) {
        return MACHINES.get(BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<RecipeHolder<?>> recipes(ServerLevel level, RecipeType<?> type) {
        return (List) level.getRecipeManager().getAllRecipesFor((RecipeType) type);
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<Direction> preferredFirst(Direction preferred) {
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.remove(preferred);
        sides.addFirst(preferred);
        return sides;
    }

    private static String normalize(ItemStack stack) {
        return AEItemKey.of(stack) + "*" + stack.getCount();
    }

    private static Map<ResourceLocation, Spec> machineSpecs() {
        Map<ResourceLocation, Spec> specs = new LinkedHashMap<>();
        addGrid(specs, "packagedexcrafting", "basic_crafter", "extendedcrafting:table",
                "thelm.packagedexcrafting.recipe.BasicPackageRecipeInfo", 1, 3);
        addGrid(specs, "packagedexcrafting", "advanced_crafter", "extendedcrafting:table",
                "thelm.packagedexcrafting.recipe.AdvancedPackageRecipeInfo", 2, 5);
        addGrid(specs, "packagedexcrafting", "elite_crafter", "extendedcrafting:table",
                "thelm.packagedexcrafting.recipe.ElitePackageRecipeInfo", 3, 7);
        addGrid(specs, "packagedexcrafting", "ultimate_crafter", "extendedcrafting:table",
                "thelm.packagedexcrafting.recipe.UltimatePackageRecipeInfo", 4, 9);
        addGrid(specs, "packagedexcrafting", "ender_crafter", "extendedcrafting:ender_crafter",
                "thelm.packagedexcrafting.recipe.EnderPackageRecipeInfo", 0, 3);
        addGrid(specs, "packagedexcrafting", "flux_crafter", "extendedcrafting:flux_crafter",
                "thelm.packagedexcrafting.recipe.FluxPackageRecipeInfo", 0, 3);
        specs.put(id("packagedexcrafting", "combination_crafter"), new Spec(
                id("extendedcrafting", "combination"),
                "thelm.packagedexcrafting.recipe.CombinationPackageRecipeInfo", 0, 0, Kind.COMBINATION));
        addGrid(specs, "packagedavaritia", "sculk_crafter", "avaritia:crafting_table_recipe",
                "thelm.packagedavaritia.recipe.SculkPackageRecipeInfo", 1, 3);
        addGrid(specs, "packagedavaritia", "nether_crafter", "avaritia:crafting_table_recipe",
                "thelm.packagedavaritia.recipe.NetherPackageRecipeInfo", 2, 5);
        addGrid(specs, "packagedavaritia", "end_crafter", "avaritia:crafting_table_recipe",
                "thelm.packagedavaritia.recipe.EndPackageRecipeInfo", 3, 7);
        addGrid(specs, "packagedavaritia", "extreme_crafter", "avaritia:crafting_table_recipe",
                "thelm.packagedavaritia.recipe.ExtremePackageRecipeInfo", 4, 9);
        return Map.copyOf(specs);
    }

    private static void addGrid(
            Map<ResourceLocation, Spec> specs,
            String namespace,
            String path,
            String recipeType,
            String recipeInfoClass,
            int tier,
            int gridSize) {
        specs.put(id(namespace, path), new Spec(
                ResourceLocation.parse(recipeType), recipeInfoClass, tier, gridSize, Kind.GRID));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private enum Kind {
        GRID,
        COMBINATION
    }

    private record Spec(
            ResourceLocation recipeType, String recipeInfoClass, int tier, int gridSize, Kind kind) {
    }

    private record RecipeLayout(
            List<List<ItemStack>> inputAlternatives, List<Integer> positions, int width, int height) {
        private List<ItemStack> primaryInputs() {
            return inputAlternatives.stream().map(List::getFirst).map(ItemStack::copy).toList();
        }
    }
}
