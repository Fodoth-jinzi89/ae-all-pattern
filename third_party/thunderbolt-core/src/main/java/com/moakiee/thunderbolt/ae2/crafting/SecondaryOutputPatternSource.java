package com.moakiee.thunderbolt.ae2.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import java.util.Collection;

/** Optional bridge supplied by host mods that expose patterns indexed by their secondary outputs. */
public interface SecondaryOutputPatternSource {
    Collection<IPatternDetails> thunderbolt$getSecondaryCraftingFor(AEKey output);

    default void thunderbolt$secondaryOutputsChanged() {
    }
}
