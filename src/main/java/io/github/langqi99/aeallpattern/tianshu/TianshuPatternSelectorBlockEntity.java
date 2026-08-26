package io.github.langqi99.aeallpattern.tianshu;

import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.MachineSource;
import com.moakiee.thunderbolt.ae2.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.thunderbolt.ae2.timewheel.TimeWheelCraftingCpuPoolHost;
import com.moakiee.thunderbolt.ae2.timewheel.TimeWheelCraftingCpuPoolProvider;
import io.github.langqi99.aeallpattern.registry.ModBlockEntities;
import io.github.langqi99.aeallpattern.registry.ModBlocks;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Restored single-block host based on AE2 Lightning Tech's former test CPU. */
public final class TianshuPatternSelectorBlockEntity extends AENetworkedBlockEntity
        implements TimeWheelCraftingCpuPoolHost {
    public static final long STORAGE_BYTES = Long.MAX_VALUE;
    public static final int PARALLELISM = 16_384;

    private static final double IDLE_POWER_USAGE = 16.0D;
    private static final String CPU_POOL_TAG = "CpuPool";

    private final IActionSource actionSource = new MachineSource(getMainNode()::getNode);
    private final TimeWheelCraftingCpuPool cpuPool =
            new TimeWheelCraftingCpuPool(this, STORAGE_BYTES, PARALLELISM);
    private long lastCpuDirtyTick = Long.MIN_VALUE;

    public TianshuPatternSelectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIANSHU_PATTERN_SELECTOR.get(), pos, state);
        getMainNode().addService(TimeWheelCraftingCpuPoolProvider.class, this);
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return super.createMainNode()
                .setTagName("tianshu_pattern_selector")
                .setVisualRepresentation(ModBlocks.TIANSHU_PATTERN_SELECTOR.get())
                .setIdlePowerUsage(IDLE_POWER_USAGE);
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool() {
        return cpuPool;
    }

    @Override
    public IActionSource getActionSource() {
        return actionSource;
    }

    @Override
    public IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public boolean isCpuActive() {
        return getMainNode().isActive() && getMainNode().getGrid() != null;
    }

    @Override
    public void markCpuDirty() {
        long now = TickHandler.instance().getCurrentTick();
        if (lastCpuDirtyTick != now) {
            lastCpuDirtyTick = now;
            saveChanges();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.aeallpattern.tianshu_pattern_selector");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (cpuPool.hasPersistentState()) {
            var poolTag = new CompoundTag();
            cpuPool.writeToNBT(poolTag, registries);
            if (!poolTag.isEmpty()) {
                tag.put(CPU_POOL_TAG, poolTag);
            }
        }
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        if (tag.contains(CPU_POOL_TAG, CompoundTag.TAG_COMPOUND)) {
            cpuPool.readFromNBT(tag.getCompound(CPU_POOL_TAG), registries);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        cpuPool.resolvePendingLoad();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        cpuPool.addRemovalDrops(level, pos, drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        cpuPool.clearRemovedContent();
    }

    @Override
    protected Item getItemFromBlockEntity() {
        return ModBlocks.TIANSHU_PATTERN_SELECTOR.get().asItem();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TianshuPatternSelectorBlockEntity selector) {
        boolean active = selector.isCpuActive();
        if (state.getValue(TianshuPatternSelectorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(TianshuPatternSelectorBlock.ACTIVE, active), 3);
        }
    }
}
