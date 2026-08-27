package io.github.langqi99.aeallpattern.compat;

import appeng.api.crafting.IPatternDetails;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TechStartPatternCompat {
    private static final Method EXPAND = find();
    private TechStartPatternCompat() {}
    @SuppressWarnings("unchecked")
    public static List<IPatternDetails> expand(ItemStack stack, Level level) {
        if (EXPAND == null) return List.of();
        try { return (List<IPatternDetails>) EXPAND.invoke(null, stack, level); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return List.of(); }
    }
    private static Method find() {
        try { return Class.forName("com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion")
                .getMethod("expand", ItemStack.class, Level.class); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
    }
}
