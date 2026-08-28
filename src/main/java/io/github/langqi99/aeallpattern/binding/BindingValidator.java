package io.github.langqi99.aeallpattern.binding;

/** Ordered, side-effect-free validation for the server-authoritative binding protocol. */
public final class BindingValidator {
    private BindingValidator() {
    }

    public static BindingDecision validate(Context context) {
        if (!context.selectionPresent()) {
            return BindingDecision.MISSING_SELECTION;
        }
        if (!context.schemaSupported()) {
            return BindingDecision.UNSUPPORTED_SCHEMA;
        }
        if (!context.ownerMatches()) {
            return BindingDecision.WRONG_OWNER;
        }
        if (!context.dimensionAllowed()) {
            return BindingDecision.WRONG_DIMENSION;
        }
        if (!context.withinRange()) {
            return BindingDecision.TOO_FAR;
        }
        if (!context.anchorLoaded()) {
            return BindingDecision.ANCHOR_UNLOADED;
        }
        if (!context.anchorMatches()) {
            return BindingDecision.ANCHOR_CHANGED;
        }
        if (!context.anchorOnline()) {
            return BindingDecision.ANCHOR_OFFLINE;
        }
        if (!context.targetSupported()) {
            return BindingDecision.UNSUPPORTED_TARGET;
        }
        if (!context.targetAvailable()) {
            return BindingDecision.TARGET_OCCUPIED;
        }
        return BindingDecision.SUCCESS;
    }

    public record Context(
            boolean selectionPresent,
            boolean schemaSupported,
            boolean ownerMatches,
            boolean dimensionAllowed,
            boolean withinRange,
            boolean anchorLoaded,
            boolean anchorMatches,
            boolean anchorOnline,
            boolean targetSupported,
            boolean targetAvailable) {
    }
}
