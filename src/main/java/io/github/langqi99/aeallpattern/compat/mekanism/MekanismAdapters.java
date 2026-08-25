package io.github.langqi99.aeallpattern.compat.mekanism;

import io.github.langqi99.aeallpattern.machine.MachineAdapterRegistry;
import mekanism.api.recipes.MekanismRecipeTypes;

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
    }
}
