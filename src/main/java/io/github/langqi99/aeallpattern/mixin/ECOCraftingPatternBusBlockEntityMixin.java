package io.github.langqi99.aeallpattern.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.util.inv.AppEngInternalInventory;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "cn.dancingsnow.neoecoae.blocks.entity.crafting.ECOCraftingPatternBusBlockEntity", remap = false)
public abstract class ECOCraftingPatternBusBlockEntityMixin {
    @Shadow @Final private AppEngInternalInventory inventory;
    @Shadow @Final private List<IPatternDetails> patternDetails;

    @Inject(method = "isExecutablePattern", at = @At("HEAD"), cancellable = true)
    private void allowAggregate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.has(ModDataComponents.AGGREGATE_PATTERN.get())) cir.setReturnValue(true);
    }

    @Inject(method = "updatePatternDetails", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;requestUpdate(Lappeng/api/networking/IManagedGridNode;)V",
            shift = At.Shift.BEFORE))
    private void addAggregatePatterns(CallbackInfo ci) {
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) return;
        for (var stack : inventory) for (var pattern : AggregatePatternExpander.expand(stack, level))
            if (pattern instanceof IMolecularAssemblerSupportedPattern) patternDetails.add(pattern);
    }
}
