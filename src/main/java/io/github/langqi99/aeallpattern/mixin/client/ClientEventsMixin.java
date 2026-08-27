package io.github.langqi99.aeallpattern.mixin.client;

import io.github.langqi99.aeallpattern.client.ClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientEvents.class)
public abstract class ClientEventsMixin {
    @ModifyArg(method = "registerScreens", at = @At(value = "INVOKE",
            target = "Lappeng/init/client/InitScreens;register(Lnet/neoforged/neoforge/client/event/RegisterMenuScreensEvent;Lnet/minecraft/world/inventory/MenuType;Lappeng/init/client/InitScreens$StyledScreenFactory;Ljava/lang/String;)V"), index = 3)
    private static String extendedStyle(String path) { return "/screens/goldentweaks_tianshu_priority.json"; }
}
