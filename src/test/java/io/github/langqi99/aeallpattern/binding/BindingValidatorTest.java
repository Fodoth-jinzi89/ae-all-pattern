package io.github.langqi99.aeallpattern.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BindingValidatorTest {
    @Test
    void acceptsACompleteValidContext() {
        assertEquals(BindingDecision.SUCCESS, BindingValidator.validate(validContext()));
    }

    @Test
    void rejectsFailuresInSecurityOrder() {
        assertEquals(BindingDecision.UNSUPPORTED_SCHEMA, BindingValidator.validate(new BindingValidator.Context(
                true, false, false, false, false, false, false, false, false, false)));
        assertEquals(BindingDecision.WRONG_OWNER, BindingValidator.validate(new BindingValidator.Context(
                true, true, false, true, true, true, true, true, true, true)));
        assertEquals(BindingDecision.WRONG_DIMENSION, BindingValidator.validate(new BindingValidator.Context(
                true, true, true, false, true, true, true, true, true, true)));
        assertEquals(BindingDecision.TOO_FAR, BindingValidator.validate(new BindingValidator.Context(
                true, true, true, true, false, true, true, true, true, true)));
        assertEquals(BindingDecision.ANCHOR_UNLOADED, BindingValidator.validate(new BindingValidator.Context(
                true, true, true, true, true, false, false, false, true, true)));
        assertEquals(BindingDecision.UNSUPPORTED_TARGET, BindingValidator.validate(new BindingValidator.Context(
                true, true, true, true, true, true, true, true, false, true)));
    }

    @Test
    void missingSelectionAlwaysWins() {
        assertEquals(BindingDecision.MISSING_SELECTION, BindingValidator.validate(new BindingValidator.Context(
                false, false, false, false, false, false, false, false, false, false)));
    }

    private static BindingValidator.Context validContext() {
        return new BindingValidator.Context(true, true, true, true, true, true, true, true, true, true);
    }
}
