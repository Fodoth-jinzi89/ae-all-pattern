package io.github.langqi99.aeallpattern.internal.routing.ae2.crafting;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.GenericStack;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Weak side-channel for confirmation-only metadata that AE's concrete CraftingPlan cannot carry. */
public final class ByproductPlanWarnings {
    private static final Map<ICraftingPlan, List<GenericStack>> WARNINGS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ByproductPlanWarnings() {
    }

    public static void attach(ICraftingPlan plan, List<GenericStack> extraOutputs) {
        if (plan != null && extraOutputs != null && !extraOutputs.isEmpty()) {
            WARNINGS.put(plan, List.copyOf(extraOutputs));
        }
    }

    public static List<GenericStack> get(ICraftingPlan plan) {
        if (plan == null) {
            return List.of();
        }
        List<GenericStack> result = WARNINGS.get(plan);
        return result == null ? List.of() : result;
    }
}
