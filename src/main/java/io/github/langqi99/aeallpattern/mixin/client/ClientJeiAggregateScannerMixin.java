package io.github.langqi99.aeallpattern.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.*;
import dev.nolij.toomanyrecipeviewers.impl.ingredient.TMRVStack;
import dev.nolij.toomanyrecipeviewers.impl.jei.api.gui.ingredient.TMRVSlotWidget;
import io.github.langqi99.aeallpattern.client.ClientJeiAggregateScanner;
import io.github.langqi99.aeallpattern.compat.emi.EmiAggregateScanner;
import java.util.Objects;
import java.util.stream.Stream;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientJeiAggregateScanner.class)
public abstract class ClientJeiAggregateScannerMixin {
    @Redirect(method = {"chooseStack", "chooseInputSlot"}, at = @At(value = "INVOKE",
            target = "Lmezz/jei/api/gui/ingredient/IRecipeSlotView;getAllIngredients()Ljava/util/stream/Stream;"))
    private static Stream<?> tmrvIngredients(IRecipeSlotView slot) {
        if (slot instanceof TMRVSlotWidget widget) {
            EmiIngredient ingredient = widget.getStack();
            return Stream.concat(slot.getAllIngredients(), ingredient.getEmiStacks().stream()
                    .map(ClientJeiAggregateScannerMixin::typed).filter(Objects::nonNull));
        }
        return slot.getAllIngredients();
    }

    @Unique @SuppressWarnings({"rawtypes", "unchecked"})
    private static ITypedIngredient<?> typed(EmiStack stack) {
        if (stack instanceof TMRVStack tmrv) return typed(tmrv.type, tmrv.ingredient);
        ItemStack item = stack.getItemStack();
        return item.isEmpty() ? null : typed(VanillaTypes.ITEM_STACK, item);
    }

    @Unique
    private static <T> ITypedIngredient<T> typed(mezz.jei.api.ingredients.IIngredientType<T> type, T ingredient) {
        return new ITypedIngredient<>() {
            @Override public mezz.jei.api.ingredients.IIngredientType<T> getType() { return type; }
            @Override public T getIngredient() { return ingredient; }
        };
    }

    @WrapOperation(method = "onRightClickBlock", at = @At(value = "INVOKE",
            target = "Lio/github/langqi99/aeallpattern/client/ClientJeiAggregateScanner;scan(Lmezz/jei/api/runtime/IJeiRuntime;Lnet/minecraft/core/BlockPos;)V"))
    private static void preferEmi(IJeiRuntime runtime, BlockPos pos, Operation<Void> original) {
        if (ModList.get().isLoaded("toomanyrecipeviewers")) {
            try { if (EmiAggregateScanner.scan(pos)) return; } catch (RuntimeException ignored) {}
        }
        try { original.call(runtime, pos); } catch (RuntimeException ignored) {}
    }
}
