package com.moakiee.thunderbolt.ae2.crafting;

/** Optional metadata supplied by virtual patterns to the dynamic route scorer. */
public interface RoutingPatternMetadata {
    /** True for a child exposed by an aggregate pattern. */
    boolean isAggregatePattern();

    /** Approximate processing time for one firing. Values below one are normalized to one. */
    default int processingTicks() {
        return 1;
    }

    /** Stable identifier used only as the final deterministic tie-breaker. */
    default String stableRouteId() {
        return "";
    }
}
