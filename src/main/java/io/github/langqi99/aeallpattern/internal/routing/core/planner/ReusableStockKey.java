package io.github.langqi99.aeallpattern.internal.routing.core.planner;

import java.util.Objects;

/** Identifies one concrete key inside a host-owned reusable-stock scope. */
public record ReusableStockKey<K>(Object scope, K key) {
    public ReusableStockKey {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(key, "key");
    }
}
