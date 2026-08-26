package io.github.langqi99.aeallpattern.mixin;

import appeng.menu.AEBaseMenu;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AEBaseMenu.class, remap = false)
public interface AEBaseMenuAccessor {
    @Invoker("registerClientAction")
    <T> void aeallpattern$registerClientAction(String name, Class<T> type, Consumer<T> handler);

    @Invoker("sendClientAction")
    <T> void aeallpattern$sendClientAction(String name, T value);
}
