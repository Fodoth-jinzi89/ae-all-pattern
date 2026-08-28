package io.github.langqi99.aeallpattern.compat.mekanism;

import io.github.langqi99.aeallpattern.machine.MachineAdapterRegistry;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;

/** Loaded only when Mekanism is present. */
public final class MekanismAdapters {
    private MekanismAdapters() {
    }

    public static void registerAll() {
        MachineAdapterRegistry.register(new MekanismItemToItemAdapter(
                "smelting", "energized_smelter", "smelting_factory"));
        MachineAdapterRegistry.register(new MekanismItemToItemAdapter(
                "crushing", MekanismRecipeTypes.TYPE_CRUSHING::get,
                "crusher", "crushing_factory"));
        MachineAdapterRegistry.register(new MekanismItemToItemAdapter(
                "enriching", MekanismRecipeTypes.TYPE_ENRICHING::get,
                "enrichment_chamber", "enriching_factory"));
        if (ModList.get().isLoaded("mekmm")) {
            MachineAdapterRegistry.register(new MekanismItemToItemAdapter(
                    "lathing", () -> recipeType("lathing"),
                    "mekmm", "cnc_lathe", "lathing_factory"));
            MachineAdapterRegistry.register(new MekanismItemToItemAdapter(
                    "rolling_mill", () -> recipeType("rolling_mill"),
                    "mekmm", "cnc_rolling_mill", "rolling_mill_factory"));
        }
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private static RecipeType<ItemStackToItemStackRecipe> recipeType(String path) {
        return (RecipeType<ItemStackToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("mekanism", path));
    }
}
