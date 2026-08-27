package io.github.langqi99.aeallpattern.tianshu;

import appeng.api.networking.IGrid;
import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.CraftingRoutePolicy;
import java.util.Comparator;

/** Resolves the deterministic network default when more than one router is connected. */
public final class TianshuRoutingPolicies {
    private TianshuRoutingPolicies() {
    }

    public static boolean isAvailable(IGrid grid) {
        return findRouter(grid) != null;
    }

    public static CraftingRoutePolicy resolve(IGrid grid) {
        TianshuPatternSelectorBlockEntity router = findRouter(grid);
        return router == null ? CraftingRoutePolicy.DEFAULT : router.getRoutingPolicy();
    }

    public static TianshuPatternSelectorBlockEntity findRouter(IGrid grid) {
        if (grid == null) {
            return null;
        }
        return grid.getActiveMachines(TianshuPatternSelectorBlockEntity.class).stream()
                .min(Comparator
                        .comparing((TianshuPatternSelectorBlockEntity router) ->
                                router.getLevel() == null ? "" : router.getLevel().dimension().location().toString())
                        .thenComparingLong(router -> router.getBlockPos().asLong()))
                .orElse(null);
    }
}
