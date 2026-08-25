package io.github.langqi99.aeallpattern.compat.jei;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Optional client-side JEI help; recipe discovery remains server-authoritative. */
@JeiPlugin
public final class AeAllPatternJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AeAllPattern.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
                ModItems.PATTERN_BINDER.get().getDefaultInstance(),
                Component.translatable("jei.aeallpattern.pattern_binder.info"));
        registration.addItemStackInfo(
                ModItems.PATTERN_LINKER.get().getDefaultInstance(),
                Component.translatable("jei.aeallpattern.pattern_linker.info"));
    }
}
