package com.moakiee.thunderbolt.ae2.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CraftingRoutePolicyContextTest {
    @Test
    void explicitPolicyMarksOnlyTheSynchronousCalculationCreationWindowActive() {
        CraftingRoutePolicy policy = CraftingRoutePolicy.DEFAULT.withYieldPreference(-1);

        assertFalse(CraftingRoutePolicyContext.isActive());
        CraftingRoutePolicy captured = CraftingRoutePolicyContext.withPolicy(policy, () -> {
            assertTrue(CraftingRoutePolicyContext.isActive());
            return CraftingRoutePolicyContext.current();
        });

        assertEquals(policy, captured);
        assertFalse(CraftingRoutePolicyContext.isActive());
    }

    @Test
    void byproductQualificationRoundTripsWithoutChangingLegacyDefaults() {
        CraftingRoutePolicy enabled = CraftingRoutePolicy.DEFAULT.withByproductOrders(true);

        assertTrue(CraftingRoutePolicy.deserialize(enabled.serialize()).allowByproductOrders());
        assertFalse(CraftingRoutePolicy.deserialize("-1,1,-1,1,1,1,8241").allowByproductOrders());
    }
}
