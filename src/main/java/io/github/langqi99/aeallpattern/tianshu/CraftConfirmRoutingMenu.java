package io.github.langqi99.aeallpattern.tianshu;

import com.moakiee.thunderbolt.ae2.crafting.CraftingRoutePolicy;

/** Client-facing extension added to AE2's crafting confirmation menu. */
public interface CraftConfirmRoutingMenu {
    boolean aeallpattern$isRoutingAvailable();

    CraftingRoutePolicy aeallpattern$getRoutePolicy();

    void aeallpattern$updateRoutePolicy(CraftingRoutePolicy policy);
}
