package io.github.langqi99.aeallpattern.machine;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Dependency-free bridge for aggregate patterns in PackagedAuto's ME packaging provider. */
public final class PackagedAutoAggregateCompat {
    private PackagedAutoAggregateCompat() {
    }

    public static boolean isAggregatePattern(ItemStack stack) {
        return stack.is(ModItems.AGGREGATE_PATTERN.get())
                && stack.has(ModDataComponents.AGGREGATE_PATTERN.get());
    }

    public static boolean hasAggregatePattern(Object provider) {
        try {
            Object handler = provider.getClass().getMethod("getItemHandler").invoke(provider);
            ItemStack stack = (ItemStack) handler.getClass()
                    .getMethod("getStackInSlot", int.class).invoke(handler, 0);
            return isAggregatePattern(stack);
        } catch (ReflectiveOperationException | RuntimeException error) {
            AeAllPattern.LOGGER.debug("Failed to inspect ME packaging provider pattern", error);
            return false;
        }
    }

    public static void refreshRecipeList(Object handler) {
        try {
            ItemStack stack = (ItemStack) handler.getClass()
                    .getMethod("getStackInSlot", int.class).invoke(handler, 0);
            if (!isAggregatePattern(stack)) {
                return;
            }
            Field blockEntityField = handler.getClass().getField("blockEntity");
            Object provider = blockEntityField.get(handler);
            if (!(provider instanceof BlockEntity blockEntity)
                    || !(blockEntity.getLevel() instanceof ServerLevel level)) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Object> recipeList = (List<Object>) provider.getClass().getField("recipeList").get(provider);
            recipeList.clear();
            recipeList.addAll(PackagedCraftingAdapter.packageRecipeInfos(level, stack));
            provider.getClass().getMethod("postPatternChange").invoke(provider);
        } catch (ReflectiveOperationException | RuntimeException error) {
            AeAllPattern.LOGGER.debug("Failed to refresh aggregate recipes in ME packaging provider", error);
        }
    }
}
