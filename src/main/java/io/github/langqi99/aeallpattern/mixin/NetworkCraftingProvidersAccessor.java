package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.me.service.helpers.NetworkCraftingProviders;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = NetworkCraftingProviders.class, remap = false)
public interface NetworkCraftingProvidersAccessor {
    @Accessor("craftingMethods")
    Map<IPatternDetails, ?> aeallpattern$getCraftingMethods();

    @Invoker("setLastModifiedOnTick")
    void aeallpattern$markModified();
}
