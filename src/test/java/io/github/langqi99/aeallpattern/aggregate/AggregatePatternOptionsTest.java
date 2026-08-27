package io.github.langqi99.aeallpattern.aggregate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AggregatePatternOptionsTest {
    @Test
    void probabilitySafeguardsAreEnabledByDefault() {
        assertFalse(AggregatePatternOptions.DEFAULT.splitSameItems());
        assertFalse(AggregatePatternOptions.DEFAULT.ignoreOutputComponents());
        assertTrue(AggregatePatternOptions.DEFAULT.skipProbabilisticMainOutput());
        assertTrue(AggregatePatternOptions.DEFAULT.ignoreProbabilisticByproducts());
    }

    @Test
    void flagsRoundTripAllFourOptions() {
        var options = new AggregatePatternOptions(true, false, false, true);
        var decoded = AggregatePatternOptions.fromFlags(options.flags());

        assertTrue(decoded.splitSameItems());
        assertFalse(decoded.ignoreOutputComponents());
        assertFalse(decoded.skipProbabilisticMainOutput());
        assertTrue(decoded.ignoreProbabilisticByproducts());
    }
}
