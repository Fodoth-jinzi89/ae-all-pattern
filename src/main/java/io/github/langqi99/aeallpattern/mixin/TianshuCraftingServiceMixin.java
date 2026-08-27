package io.github.langqi99.aeallpattern.mixin;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.me.service.CraftingService;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.CraftingRoutePolicyContext;
import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.TianshuFastCraftingControl;
import java.util.concurrent.Future;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures a router policy on the AE calculation object before it moves to the worker thread. */
@Mixin(value = CraftingService.class, remap = false, priority = 2000)
public abstract class TianshuCraftingServiceMixin {
    @Inject(
            method = "beginCraftingCalculation",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ExecutorService;submit(Ljava/util/concurrent/Callable;)"
                            + "Ljava/util/concurrent/Future;",
                    shift = At.Shift.BEFORE))
    private void aeallpattern$captureRouterPolicy(
            Level level,
            ICraftingSimulationRequester requester,
            AEKey what,
            long amount,
            CalculationStrategy strategy,
            CallbackInfoReturnable<Future<ICraftingPlan>> cir,
            @Local CraftingCalculation job) {
        if (CraftingRoutePolicyContext.isActive()) {
            ((TianshuFastCraftingControl) job)
                    .aeallpattern$setRoutePolicy(CraftingRoutePolicyContext.current());
        }
    }
}
