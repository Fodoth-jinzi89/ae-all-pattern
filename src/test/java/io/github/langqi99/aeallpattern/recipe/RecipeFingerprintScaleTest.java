package io.github.langqi99.aeallpattern.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class RecipeFingerprintScaleTest {
    @Test
    void tenThousandFingerprintsRemainFastAndUnique() {
        assertTimeout(Duration.ofSeconds(5), () -> {
            var keys = new HashSet<String>();
            for (int index = 0; index < 10_000; index++) {
                var fingerprint = new RecipeFingerprint(
                        "benchmark:machine",
                        "benchmark:recipe_" + index,
                        "benchmark:input_" + index + "*1",
                        "benchmark:output_" + index + "*1",
                        1);
                keys.add(fingerprint.stableKey());
            }
            assertEquals(10_000, keys.size());
        });
    }
}
