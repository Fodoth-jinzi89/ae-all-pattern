package io.github.langqi99.aeallpattern.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AeAllPatternMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("ClientJeiAggregateScannerMixin")) {
            return loaded("emi") && loaded("toomanyrecipeviewers");
        }
        if (mixin.endsWith("ECOCraftingPatternBusBlockEntityMixin")) {
            return loaded("neoecoae");
        }
        if (mixin.endsWith("AdvancedAlloyFurnaceAeManagerMixin")) {
            return loaded("useless_mod");
        }
        return true;
    }
    private static boolean loaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
