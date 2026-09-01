package io.github.langqi99.aeallpattern.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AggregatePatternSelectionTest {
    @Test
    void absentSelectionPublishesEverything() {
        assertTrue(AggregatePatternSelection.ALL_ENABLED.isEnabled("a"));
        assertTrue(AggregatePatternSelection.ALL_ENABLED.isEnabled("b"));
        assertTrue(AggregatePatternSelection.ALL_ENABLED.isAllEnabled());
    }

    @Test
    void noneEnabledRepresentationStaysCompact() {
        var selection = AggregatePatternSelection.NONE_ENABLED;
        assertTrue(selection.isNoneEnabled());
        assertTrue(selection.ids().isEmpty());
        assertFalse(selection.isEnabled("a"));
    }

    @Test
    void togglingMovesPatternIdsBetweenEnabledAndDisabled() {
        var selection = AggregatePatternSelection.ALL_ENABLED
                .toggled("a")
                .toggled("b");
        assertFalse(selection.isEnabled("a"));
        assertFalse(selection.isEnabled("b"));
        assertTrue(selection.isEnabled("c"));

        var restored = selection.toggled("a");
        assertTrue(restored.isEnabled("a"));
        assertFalse(restored.isEnabled("b"));
        assertEquals(List.of("b"), restored.ids());
    }

    @Test
    void invertedSelectionTogglesEnabledSet() {
        var selection = AggregatePatternSelection.NONE_ENABLED.toggled("only");
        assertTrue(selection.isEnabled("only"));
        assertFalse(selection.isEnabled("other"));

        var restored = selection.toggled("only");
        assertTrue(restored.isNoneEnabled());
    }

    @Test
    void duplicateIdsAreDeduplicated() {
        var selection = new AggregatePatternSelection(false, List.of("a", "a", "b"));
        assertEquals(List.of("a", "b"), selection.ids());
    }

    @Test
    void rejectsInvalidIdsAndOversizedLists() {
        assertThrows(IllegalArgumentException.class, () -> new AggregatePatternSelection(false, List.of("")));
        assertThrows(IllegalArgumentException.class,
                () -> new AggregatePatternSelection(false, List.of("x".repeat(161))));
        var tooMany = java.util.stream.IntStream.range(0, AggregatePatternSelection.MAX_IDS + 1)
                .mapToObj(index -> "id" + index)
                .toList();
        assertThrows(IllegalArgumentException.class, () -> new AggregatePatternSelection(false, tooMany));
    }
}
