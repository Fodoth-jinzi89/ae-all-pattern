package io.github.langqi99.aeallpattern.linker;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.GlobalPos;
import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import io.github.langqi99.aeallpattern.network.BindingSyncService;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.util.ArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public final class PatternLinkerBlock extends BaseEntityBlock {
    public static final MapCodec<PatternLinkerBlock> CODEC = simpleCodec(PatternLinkerBlock::new);

    public PatternLinkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PatternLinkerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, io.github.langqi99.aeallpattern.registry.ModBlockEntities.PATTERN_LINKER.get(),
                        PatternLinkerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(level.getBlockEntity(pos) instanceof PatternLinkerBlockEntity linker)
                    || !linker.isOwnedBy(player)) {
                return InteractionResult.FAIL;
            }
            serverPlayer.openMenu(linker, data -> {
                data.writeBoolean(true);
                data.writeBlockPos(pos);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return stack.is(ModItems.PATTERN_BINDER.get())
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof PatternLinkerBlockEntity linker) {
            linker.setOwner(player);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof PatternLinkerBlockEntity linker) {
                var bufferedDrops = new ArrayList<ItemStack>();
                linker.addAdditionalDrops(level, pos, bufferedDrops);
                bufferedDrops.forEach(stack -> Block.popResource(level, pos, stack));
                linker.clearContent();
            }
            BindingSavedData.get(serverLevel.getServer())
                    .removeByAnchor(GlobalPos.of(serverLevel.dimension(), pos.immutable()));
            BindingSyncService.sendToOnlinePlayers(serverLevel.getServer());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
