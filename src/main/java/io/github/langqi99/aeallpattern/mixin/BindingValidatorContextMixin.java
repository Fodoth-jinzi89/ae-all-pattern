package io.github.langqi99.aeallpattern.mixin;

import io.github.langqi99.aeallpattern.binding.BindingValidator;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BindingValidator.Context.class)
public abstract class BindingValidatorContextMixin {
    @ModifyReturnValue(method = "sameDimension", at = @At("RETURN"))
    private boolean allowAnyDimension(boolean original) { return true; }
    @ModifyReturnValue(method = "withinRange", at = @At("RETURN"))
    private boolean allowAnyDistance(boolean original) { return true; }
}
