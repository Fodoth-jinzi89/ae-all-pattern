package io.github.langqi99.aeallpattern.aggregate;

import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import io.github.langqi99.aeallpattern.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative editor for the aggregate pattern held by the player. */
public final class AggregatePatternConfigMenu extends AbstractContainerMenu {
    public static final int TOGGLE_SPLIT_SAME_ITEMS = 0;
    public static final int TOGGLE_IGNORE_OUTPUT_COMPONENTS = 1;
    public static final int TOGGLE_SKIP_PROBABILISTIC_MAIN_OUTPUT = 2;
    public static final int TOGGLE_IGNORE_PROBABILISTIC_BYPRODUCTS = 3;

    private final Inventory inventory;
    private final InteractionHand hand;
    private int optionFlags;

    public AggregatePatternConfigMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readEnum(InteractionHand.class));
    }

    public AggregatePatternConfigMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.AGGREGATE_PATTERN_CONFIG.get(), id);
        this.inventory = inventory;
        this.hand = hand;
        optionFlags = options(stack()).flags();
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return inventory.player.level().isClientSide() ? optionFlags : options(stack()).flags();
            }

            @Override
            public void set(int value) {
                optionFlags = value & 15;
            }
        });
    }

    public AggregatePatternOptions getOptions() {
        return AggregatePatternOptions.fromFlags(optionFlags);
    }

    public ItemStack stack() {
        return inventory.player.getItemInHand(hand);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < TOGGLE_SPLIT_SAME_ITEMS || id > TOGGLE_IGNORE_PROBABILISTIC_BYPRODUCTS) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!isConfigurable(stack)) {
            return false;
        }
        int mask = 1 << id;
        optionFlags = options(stack).flags() ^ mask;
        stack.set(ModDataComponents.AGGREGATE_PATTERN_OPTIONS.get(), AggregatePatternOptions.fromFlags(optionFlags));
        player.getInventory().setChanged();
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return isConfigurable(player.getItemInHand(hand));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static boolean isConfigurable(ItemStack stack) {
        return stack.is(ModItems.AGGREGATE_PATTERN.get())
                && stack.has(ModDataComponents.AGGREGATE_PATTERN.get());
    }

    private static AggregatePatternOptions options(ItemStack stack) {
        AggregatePatternOptions options = stack.get(ModDataComponents.AGGREGATE_PATTERN_OPTIONS.get());
        return options == null ? AggregatePatternOptions.DEFAULT : options;
    }
}
