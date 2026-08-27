package io.github.langqi99.aeallpattern.internal.routing.ae2.crafting;

import org.jetbrains.annotations.Nullable;

/** Private per-calculation bridge used only by an explicitly active Tianshu router. */
public interface TianshuFastCraftingControl {
    void aeallpattern$setRoutePolicy(@Nullable CraftingRoutePolicy policy);

    @Nullable
    CraftingRoutePolicy aeallpattern$getRoutePolicy();
}
