package io.github.langqi99.aeallpattern.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Guards the recipe-category choice for addon machines.
 *
 * A machine is often a catalyst for more than one recipe viewer category, and picking the wrong
 * one generates patterns that have nothing to do with the machine.
 */
class JeiCategorySelectionTest {
    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    @Test
    void infusingFactoryPicksMetallurgicInfusingNotConversion() {
        // Mekanism registers the infusing factory as a catalyst for both the production recipes
        // and the "item -> infusion type" refilling recipes. Only the former makes a pattern.
        var picked = ClientJeiAggregateScanner.pickCategoryId(
                List.of(id("mekanism:infusion_conversion"), id("mekanism:metallurgic_infusing")),
                id("mekanism:advanced_infusing_factory"));

        assertEquals(id("mekanism:metallurgic_infusing"), picked,
                "infusing factory must generate metallurgic infusing recipes, not infusion conversions");
    }

    @Test
    void everyMekanismFactoryMapsToItsOwnOperation() {
        var categories = List.of(
                id("mekanism:infusion_conversion"),
                id("mekanism:metallurgic_infusing"),
                id("mekanism:enriching"),
                id("mekanism:crushing"),
                id("mekanism:compressing"),
                id("mekanism:combining"),
                id("mekanism:purifying"),
                id("mekanism:injecting"),
                id("mekanism:sawing"));

        assertEquals(id("mekanism:enriching"),
                pick("mekanism:advanced_enriching_factory", categories));
        assertEquals(id("mekanism:crushing"),
                pick("mekanism:basic_crushing_factory", categories));
        assertEquals(id("mekanism:compressing"),
                pick("mekanism:elite_compressing_factory", categories));
        assertEquals(id("mekanism:combining"),
                pick("mekanism:ultimate_combining_factory", categories));
        assertEquals(id("mekanism:purifying"),
                pick("mekanism:advanced_purifying_factory", categories));
        assertEquals(id("mekanism:injecting"),
                pick("mekanism:advanced_injecting_factory", categories));
        assertEquals(id("mekanism:sawing"),
                pick("mekanism:advanced_sawing_factory", categories));
    }

    @Test
    void nonFactoryMachineStillResolvesByKeyword() {
        assertEquals(id("mekanism:metallurgic_infusing"),
                pick("mekanism:metallurgic_infuser",
                        List.of(id("mekanism:infusion_conversion"), id("mekanism:metallurgic_infusing"))));
    }

    @Test
    void chemicalOxidizerPicksOxidizingCategory() {
        assertEquals(id("mekanism:oxidizing"),
                pick("mekanism:chemical_oxidizer",
                        List.of(id("mekanism:reaction"), id("mekanism:oxidizing"))));
    }

    @Test
    void fallsBackToFirstCategoryOfTheSameNamespace() {
        // Unknown machines keep the previous behaviour instead of failing to scan at all.
        var picked = ClientJeiAggregateScanner.pickCategoryId(
                List.of(id("somemod:alpha"), id("somemod:beta")), id("somemod:mystery_machine"));
        assertEquals(id("somemod:alpha"), picked);
    }

    @Test
    void addonMachineBorrowsTheBaseModCategory() {
        // mekmm's oxidizing factory runs Mekanism's oxidizing recipes; the namespaces differ.
        var picked = ClientJeiAggregateScanner.pickCategoryId(
                List.of(id("mekanism:oxidizing"), id("mekanism:crushing")),
                id("mekmm:advanced_oxidizing_factory"));

        assertEquals(id("mekanism:oxidizing"), picked,
                "mekmm oxidizing factory must generate mekanism oxidizing recipes");
    }

    @Test
    void addonMachinePrefersItsOwnCategoryWhenBothExist() {
        var picked = ClientJeiAggregateScanner.pickCategoryId(
                List.of(id("mekmm:oxidizing"), id("mekanism:oxidizing")),
                id("mekmm:advanced_oxidizing_factory"));

        assertEquals(id("mekmm:oxidizing"), picked,
                "the machine's own namespace wins over the borrowed category");
    }

    @Test
    void prefersSameNamespaceWhenTheKeywordMatchesBoth() {
        // The recipe viewer only returns categories the machine is a catalyst for, so a cross
        // namespace hit is legitimate; the machine's own namespace still wins.
        var picked = ClientJeiAggregateScanner.pickCategoryId(
                List.of(id("othermod:infusing"), id("mekanism:metallurgic_infusing")),
                id("mekanism:advanced_infusing_factory"));
        assertEquals(id("mekanism:metallurgic_infusing"), picked);
    }

    @Test
    void stripsTierAndFactorySuffix() {
        assertEquals("infusing", ClientJeiAggregateScanner.machineKeyword("advanced_infusing_factory"));
        assertEquals("infusing", ClientJeiAggregateScanner.machineKeyword("basic_infusing_factory"));
        assertEquals("infusing", ClientJeiAggregateScanner.machineKeyword("infusing_factory"));
        assertEquals("crushing", ClientJeiAggregateScanner.machineKeyword("ultimate_crushing_factory"));
        assertEquals("enrichment", ClientJeiAggregateScanner.machineKeyword("enrichment_chamber"));
    }

    private static ResourceLocation pick(String machine, List<ResourceLocation> categories) {
        return ClientJeiAggregateScanner.pickCategoryId(categories, id(machine));
    }
}
