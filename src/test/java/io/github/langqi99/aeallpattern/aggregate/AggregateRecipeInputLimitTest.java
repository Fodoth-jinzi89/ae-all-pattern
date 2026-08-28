package io.github.langqi99.aeallpattern.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AggregateRecipeInputLimitTest {
    @Test
    void matchesAe2ProcessingPatternCapacity() {
        assertEquals(81, AggregateRecipe.MAX_INPUTS);
        assertEquals(27, AggregateRecipe.MAX_OUTPUTS);
        assertEquals(32, AggregateInputSlot.MAX_ALTERNATIVES);
        assertEquals(81 * 32, AggregateRecipe.MAX_TOTAL_INPUT_ALTERNATIVES);
    }
}
