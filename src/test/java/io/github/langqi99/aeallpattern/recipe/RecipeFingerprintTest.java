package io.github.langqi99.aeallpattern.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RecipeFingerprintTest {
    @Test
    void identicalRecipesProduceTheSameStableKey() {
        var first = new RecipeFingerprint("minecraft:furnace", "minecraft:iron_ingot",
                "minecraft:raw_iron*1", "minecraft:iron_ingot*1", 1);
        var second = new RecipeFingerprint("minecraft:furnace", "minecraft:iron_ingot",
                "minecraft:raw_iron*1", "minecraft:iron_ingot*1", 1);

        assertEquals(first.stableKey(), second.stableKey());
    }

    @Test
    void schemaChangesInvalidateTheFingerprint() {
        var oldFingerprint = new RecipeFingerprint("mekanism:smelting", "test:iron",
                "minecraft:raw_iron*1", "minecraft:iron_ingot*1", 1);
        var newFingerprint = new RecipeFingerprint("mekanism:smelting", "test:iron",
                "minecraft:raw_iron*1", "minecraft:iron_ingot*1", 2);

        assertNotEquals(oldFingerprint.stableKey(), newFingerprint.stableKey());
    }

    @Test
    void fieldBoundariesCannotCollideThroughNewlines() {
        var first = new RecipeFingerprint("test:adapter", "test:recipe", "x\ny", "z", 1);
        var second = new RecipeFingerprint("test:adapter", "test:recipe", "x", "y\nz", 1);

        assertNotEquals(first.stableKey(), second.stableKey());
    }

    @Test
    void rejectsInvalidSchemaVersions() {
        assertThrows(IllegalArgumentException.class, () -> new RecipeFingerprint(
                "minecraft:furnace", "minecraft:iron_ingot", "in", "out", 0));
    }
}
