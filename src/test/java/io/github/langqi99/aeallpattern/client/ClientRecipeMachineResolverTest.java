package io.github.langqi99.aeallpattern.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ClientRecipeMachineResolverTest {
    @Test
    void mapsEveryPackagedExCrafterToItsExtendedCraftingWorkstation() {
        assertAlias("packagedexcrafting", "basic_crafter", "extendedcrafting", "basic_table");
        assertAlias("packagedexcrafting", "advanced_crafter", "extendedcrafting", "advanced_table");
        assertAlias("packagedexcrafting", "elite_crafter", "extendedcrafting", "elite_table");
        assertAlias("packagedexcrafting", "ultimate_crafter", "extendedcrafting", "ultimate_table");
        assertAlias("packagedexcrafting", "ender_crafter", "extendedcrafting", "ender_crafter");
        assertAlias("packagedexcrafting", "flux_crafter", "extendedcrafting", "flux_crafter");
        assertAlias("packagedexcrafting", "combination_crafter", "extendedcrafting", "crafting_core");
        assertAlias("packagedexcrafting", "marked_pedestal", "extendedcrafting", "pedestal");
    }

    @Test
    void mapsEveryPackagedAvaritiaCrafterToItsReAvaritiaWorkstation() {
        assertAlias("packagedavaritia", "sculk_crafter", "avaritia", "sculk_crafting_table");
        assertAlias("packagedavaritia", "nether_crafter", "avaritia", "nether_crafting_table");
        assertAlias("packagedavaritia", "end_crafter", "avaritia", "end_crafting_table");
        assertAlias("packagedavaritia", "extreme_crafter", "avaritia", "extreme_crafting_table");
    }

    @Test
    void mapsEveryAppliedExtendedCraftingStationToItsExtendedCraftingWorkstation() {
        assertAlias("applied_extended_crafting", "table_basic_pattern_provider", "extendedcrafting", "basic_table");
        assertAlias("applied_extended_crafting", "table_advanced_pattern_provider", "extendedcrafting", "advanced_table");
        assertAlias("applied_extended_crafting", "table_elite_pattern_provider", "extendedcrafting", "elite_table");
        assertAlias("applied_extended_crafting", "table_ultimate_pattern_provider", "extendedcrafting", "ultimate_table");
        assertAlias("applied_extended_crafting", "ender_crafter_pattern_provider", "extendedcrafting", "ender_crafter");
        assertAlias("applied_extended_crafting", "flux_crafter_pattern_provider", "extendedcrafting", "flux_crafter");
        assertAlias("applied_extended_crafting", "crafter_core_pattern_provider", "extendedcrafting", "crafting_core");
    }

    @Test
    void leavesOrdinaryMachinesUnchanged() {
        ResourceLocation id = id("mekmm", "large_chemical_infuser");
        assertEquals(id, ClientRecipeMachineResolver.catalystAlias(id));
    }

    private static void assertAlias(
            String sourceNamespace, String sourcePath, String targetNamespace, String targetPath) {
        assertEquals(
                id(targetNamespace, targetPath),
                ClientRecipeMachineResolver.catalystAlias(id(sourceNamespace, sourcePath)));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
