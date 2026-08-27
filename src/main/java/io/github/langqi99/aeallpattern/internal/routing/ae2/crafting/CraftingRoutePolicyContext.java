package io.github.langqi99.aeallpattern.internal.routing.ae2.crafting;

import java.util.Objects;
import java.util.function.Supplier;

/** Transfers a menu's policy into the calculation created synchronously on that thread. */
public final class CraftingRoutePolicyContext {
    private static final ThreadLocal<CraftingRoutePolicy> CURRENT = new ThreadLocal<>();

    private CraftingRoutePolicyContext() {
    }

    public static CraftingRoutePolicy current() {
        CraftingRoutePolicy policy = CURRENT.get();
        return policy == null ? CraftingRoutePolicy.DEFAULT : policy;
    }

    /** Whether the current calculation was explicitly claimed by a route controller. */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    public static <T> T withPolicy(CraftingRoutePolicy policy, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        CraftingRoutePolicy previous = CURRENT.get();
        CURRENT.set(policy == null ? CraftingRoutePolicy.DEFAULT : policy);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
