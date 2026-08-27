package io.github.langqi99.aeallpattern.tianshu;

import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.CraftingRoutePolicy;
import appeng.api.stacks.GenericStack;

/** Client-facing extension added to AE2's crafting confirmation menu. */
public interface CraftConfirmRoutingMenu {
    boolean aeallpattern$isRoutingAvailable();

    CraftingRoutePolicy aeallpattern$getRoutePolicy();

    void aeallpattern$updateRoutePolicy(CraftingRoutePolicy policy);

    GenericStack aeallpattern$getByproductWarning();

    int aeallpattern$getByproductWarningKinds();
}
