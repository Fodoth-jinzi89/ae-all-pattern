package io.github.langqi99.aeallpattern.linker;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import io.github.langqi99.aeallpattern.ae.VirtualCraftingProvider;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import io.github.langqi99.aeallpattern.registry.ModBlockEntities;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.world.level.block.Block;

/** AE-owned anchor for bindings. It consumes one channel and a small idle power budget. */
public final class PatternLinkerBlockEntity extends AENetworkedBlockEntity {
    private static final String OWNER_TAG = "Owner";
    private static final double IDLE_POWER_USAGE = 2.0;

    @Nullable
    private UUID ownerId;
    private final IncomingBuffer incomingBuffer = new IncomingBuffer();
    private final VirtualCraftingProvider craftingProvider;

    public PatternLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PATTERN_LINKER.get(), pos, state);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(IDLE_POWER_USAGE);
        craftingProvider = new VirtualCraftingProvider(this, incomingBuffer);
        getMainNode().addService(ICraftingProvider.class, craftingProvider);
    }

    @Override
    protected Item getItemFromBlockEntity() {
        return ModItems.PATTERN_LINKER.get();
    }

    @Override
    public void setOwner(Player player) {
        super.setOwner(player);
        ownerId = player.getUUID();
        saveChanges();
    }

    public Optional<UUID> getOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public boolean isOwnedBy(Player player) {
        return ownerId == null || ownerId.equals(player.getUUID());
    }

    public void refreshPatterns() {
        craftingProvider.refresh();
    }

    public int insertIntoNetwork(ItemStack stack, Actionable mode) {
        if (stack.isEmpty() || !getMainNode().isOnline()) {
            return 0;
        }
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        long inserted = grid.getStorageService().getInventory().insert(
                AEItemKey.of(stack), stack.getCount(), mode, IActionSource.ofMachine(this));
        return (int) Math.min(stack.getCount(), inserted);
    }

    public void cancelBinding(UUID bindingId) {
        if (level == null || level.isClientSide()) {
            return;
        }
        incomingBuffer.removeBinding(bindingId).forEach(stack -> Block.popResource(level, worldPosition, stack));
        saveChanges();
    }

    @Override
    public void onReady() {
        super.onReady();
        refreshPatterns();
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        ownerId = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        incomingBuffer.load(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) {
            tag.putUUID(OWNER_TAG, ownerId);
        }
        incomingBuffer.save(tag, registries);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.addAll(incomingBuffer.recoverableDrops());
    }

    @Override
    public void clearContent() {
        super.clearContent();
        incomingBuffer.clear();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, PatternLinkerBlockEntity linker) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (linker.incomingBuffer.tick(serverLevel, linker)) {
            linker.saveChanges();
        }
        if (linker.craftingProvider.catalogGeneration() != RecipeIndexService.generation()) {
            linker.craftingProvider.refresh();
        }
        linker.craftingProvider.tickAvailability();
    }
}
