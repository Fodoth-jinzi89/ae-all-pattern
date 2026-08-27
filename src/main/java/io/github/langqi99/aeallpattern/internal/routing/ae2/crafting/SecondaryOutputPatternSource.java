package io.github.langqi99.aeallpattern.internal.routing.ae2.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import java.util.Collection;

/** Optional bridge supplied by host mods that expose patterns indexed by their secondary outputs. */
public interface SecondaryOutputPatternSource {
    Collection<IPatternDetails> aeallpattern$getSecondaryCraftingFor(AEKey output);

    default void aeallpattern$secondaryOutputsChanged() {
    }
}
