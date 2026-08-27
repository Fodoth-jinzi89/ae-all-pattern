package io.github.langqi99.aeallpattern.compat.emi;

import appeng.api.stacks.GenericStack;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.nolij.toomanyrecipeviewers.impl.ingredient.TMRVStack;
import io.github.langqi99.aeallpattern.aggregate.*;
import io.github.langqi99.aeallpattern.network.GenerateAggregatePayload;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

public final class EmiAggregateScanner {
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private EmiAggregateScanner() {}

    public static boolean scan(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return true;
        Block block = minecraft.level.getBlockState(pos).getBlock();
        ItemStack machine = block.asItem().getDefaultInstance();
        if (machine.isEmpty()) return false;
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        EmiStack machineStack = EmiStack.of(machine);
        List<EmiRecipeCategory> categories = manager.getCategories().stream()
                .filter(category -> isCraftingMachine(machine, category)
                        || manager.getWorkstations(category).stream().flatMap(i -> i.getEmiStacks().stream())
                        .anyMatch(stack -> stack.isEqual(machineStack))).toList();
        if (categories.isEmpty()) return false;
        List<EmiRecipe> candidates = categories.stream().flatMap(c -> manager.getRecipes(c).stream())
                .limit(AggregatePatternData.MAX_RECIPES * 2L).toList();
        if (candidates.isEmpty() || !RUNNING.compareAndSet(false, true)) return false;
        var connection = minecraft.getConnection();
        if (connection == null) { RUNNING.set(false); return true; }
        ResourceLocation catalystId = BuiltInRegistries.BLOCK.getKey(block);
        CompletableFuture.runAsync(() -> buildAndSend(pos, catalystId, block.getDescriptionId(), candidates,
                connection.registryAccess(), connection.getConnectionType()))
                .whenComplete((ignored, error) -> RUNNING.set(false));
        return true;
    }

    private static void buildAndSend(BlockPos pos, ResourceLocation catalystId, String machineName,
            List<EmiRecipe> candidates, RegistryAccess registries, ConnectionType connectionType) {
        List<AggregateRecipe> recipes = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (EmiRecipe recipe : candidates) {
            try { toAggregate(recipe, ids).filter(r -> canEncode(r, registries, connectionType)).ifPresent(recipes::add); }
            catch (RuntimeException ignored) {}
            if (recipes.size() >= AggregatePatternData.MAX_RECIPES) break;
        }
        if (recipes.isEmpty()) return;
        Minecraft.getInstance().execute(() -> {
            UUID uploadId = UUID.randomUUID();
            int pageCount = (recipes.size() + AggregatePatternLibrary.PAGE_SIZE - 1) / AggregatePatternLibrary.PAGE_SIZE;
            for (int page = 0; page < pageCount; page++) {
                int from = page * AggregatePatternLibrary.PAGE_SIZE;
                int to = Math.min(recipes.size(), from + AggregatePatternLibrary.PAGE_SIZE);
                PacketDistributor.sendToServer(new GenerateAggregatePayload(uploadId, pos, catalystId, machineName,
                        page, pageCount, recipes.size(), recipes.subList(from, to)));
            }
        });
    }

    private static boolean canEncode(AggregateRecipe recipe, RegistryAccess registries, ConnectionType type) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, type);
        try { AggregateRecipe.STREAM_CODEC.encode(buffer, recipe); return true; }
        catch (RuntimeException ignored) { return false; }
        finally { buffer.release(); }
    }

    private static boolean isCraftingMachine(ItemStack machine, EmiRecipeCategory category) {
        return category == VanillaEmiRecipeCategories.CRAFTING && (machine.is(Blocks.CRAFTING_TABLE.asItem())
                || BuiltInRegistries.ITEM.getKey(machine.getItem()).toString().equals("ae2:molecular_assembler"));
    }

    private static Optional<AggregateRecipe> toAggregate(EmiRecipe recipe, Set<String> ids) {
        if (recipe.getInputs().isEmpty() || recipe.getInputs().size() > 9 || recipe.getOutputs().isEmpty()) return Optional.empty();
        int limit = Math.max(1, 512 / recipe.getInputs().size());
        List<AggregateInputSlot> inputs = recipe.getInputs().stream().map(i -> input(i, limit)).flatMap(Optional::stream).toList();
        List<GenericStack> outputs = recipe.getOutputs().stream().map(EmiAggregateScanner::stack).flatMap(Optional::stream).limit(3).toList();
        if (inputs.size() != recipe.getInputs().size() || outputs.isEmpty()) return Optional.empty();
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        if (backing != null && backing.value().isSpecial()) return Optional.empty();
        ResourceLocation id = backing == null ? recipe.getId() : backing.id();
        if (id == null) return Optional.empty();
        String patternId = recipe.getCategory().getId() + "/" + id;
        if (!ids.add(patternId)) return Optional.empty();
        return Optional.of(new AggregateRecipe(patternId, id, kind(backing),
                inputs.stream().map(AggregateInputSlot::primary).toList(), inputs, outputs, 1));
    }

    private static Optional<AggregateInputSlot> input(EmiIngredient ingredient, int limit) {
        List<GenericStack> alternatives = ingredient.getEmiStacks().stream()
                .map(s -> stack(s.copy().setAmount(ingredient.getAmount()))).flatMap(Optional::stream).limit(limit).toList();
        return alternatives.isEmpty() ? Optional.empty() : Optional.of(new AggregateInputSlot(alternatives, Optional.empty()));
    }

    private static Optional<GenericStack> stack(EmiStack stack) {
        if (stack.isEmpty() || stack.getAmount() <= 0) return Optional.empty();
        ItemStack item = stack.getItemStack();
        if (!item.isEmpty()) { item.setCount((int) Math.min(Integer.MAX_VALUE, stack.getAmount())); return Optional.ofNullable(GenericStack.fromItemStack(item)); }
        if (stack.getKey() instanceof Fluid fluid) return Optional.ofNullable(GenericStack.fromFluidStack(
                new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, stack.getAmount()))));
        if (stack.getClass().getName().equals("mekanism.client.recipe_viewer.emi.ChemicalEmiStack")) {
            return mekanismChemical(stack);
        }
        return stack instanceof TMRVStack<?> tmrv ? registered(tmrv) : Optional.empty();
    }

    private static Optional<GenericStack> mekanismChemical(EmiStack stack) {
        try {
            Object chemicalStack = stack.getClass().getMethod("getStack").invoke(stack);
            Class<?> chemicalStackType = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object key = Class.forName("me.ramidzkh.mekae2.ae2.MekanismKey")
                    .getMethod("of", chemicalStackType).invoke(null, chemicalStack);
            return Optional.of(new GenericStack((appeng.api.stacks.AEKey) key, stack.getAmount()));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<GenericStack> registered(TMRVStack<?> stack) {
        try {
            Class<?> converters = Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
            Object converter = converters.getMethod("getConverter", mezz.jei.api.ingredients.IIngredientType.class).invoke(null, stack.type);
            if (converter == null) return Optional.empty();
            Method convert = Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter")
                    .getMethod("getStackFromIngredient", Object.class);
            GenericStack result = (GenericStack) convert.invoke(converter, stack.ingredient);
            return result == null ? Optional.empty() : Optional.of(new GenericStack(result.what(), stack.getAmount()));
        } catch (ReflectiveOperationException | RuntimeException ignored) { return Optional.empty(); }
    }

    private static AggregatePatternKind kind(RecipeHolder<?> recipe) {
        if (recipe != null && recipe.value() instanceof CraftingRecipe) return AggregatePatternKind.CRAFTING;
        if (recipe != null && recipe.value() instanceof StonecutterRecipe) return AggregatePatternKind.STONECUTTING;
        if (recipe != null && recipe.value() instanceof SmithingRecipe) return AggregatePatternKind.SMITHING;
        return AggregatePatternKind.PROCESSING;
    }
}
