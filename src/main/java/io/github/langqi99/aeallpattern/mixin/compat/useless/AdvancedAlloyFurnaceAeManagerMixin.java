package io.github.langqi99.aeallpattern.mixin.compat.useless;

import appeng.api.crafting.IPatternDetails;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternMarkerDetails;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes one aggregate pattern slot publish every child pattern in an alloy furnace. */
@Pseudo
@Mixin(targets = "com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.AdvancedAlloyFurnaceAeManager", remap = false)
public abstract class AdvancedAlloyFurnaceAeManagerMixin {
    @Inject(method = "rebuildPatterns", at = @At("TAIL"), remap = false)
    private void aeallpattern$expandAggregatePatterns(CallbackInfo callback) {
        try {
            Object owner = field(this, "owner");
            Level level = (Level) invoke(owner, "getLevel");
            if (level == null || level.isClientSide || !Boolean.TRUE.equals(invoke(owner, "canPublishPatterns"))) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<IPatternDetails> patterns = (List<IPatternDetails>) field(this, "patterns");
            patterns.removeIf(AggregatePatternMarkerDetails.class::isInstance);

            Object stacks = invoke(owner, "getPatternStacks");
            if (!(stacks instanceof Iterable<?> iterable)) {
                return;
            }
            for (Object value : iterable) {
                if (value instanceof ItemStack stack) {
                    for (IPatternDetails pattern : AggregatePatternExpander.expand(stack, level)) {
                        if (accepts(owner, pattern)) {
                            patterns.add(pattern);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            io.github.langqi99.aeallpattern.AeAllPattern.LOGGER.debug(
                    "Could not expand aggregate patterns for an advanced alloy furnace", error);
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name);
        return method.invoke(target);
    }

    private static boolean accepts(Object owner, IPatternDetails pattern) throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod("acceptsPattern", IPatternDetails.class);
        return Boolean.TRUE.equals(method.invoke(owner, pattern));
    }
}
