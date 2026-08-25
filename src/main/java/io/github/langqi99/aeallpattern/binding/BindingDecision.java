package io.github.langqi99.aeallpattern.binding;

/** Stable result codes shared by the server interaction and pure state-machine tests. */
public enum BindingDecision {
    SUCCESS,
    MISSING_SELECTION,
    UNSUPPORTED_SCHEMA,
    WRONG_OWNER,
    WRONG_DIMENSION,
    TOO_FAR,
    ANCHOR_UNLOADED,
    ANCHOR_CHANGED,
    ANCHOR_OFFLINE,
    UNSUPPORTED_TARGET,
    TARGET_OCCUPIED
}
