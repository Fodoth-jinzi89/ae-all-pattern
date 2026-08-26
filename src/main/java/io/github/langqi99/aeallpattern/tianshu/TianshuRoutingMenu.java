package io.github.langqi99.aeallpattern.tianshu;

import appeng.menu.AEBaseMenu;
import com.moakiee.thunderbolt.ae2.crafting.CraftingRoutePolicy;
import io.github.langqi99.aeallpattern.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative editor for the network's default route policy. */
public final class TianshuRoutingMenu extends AEBaseMenu {
    private static final String UPDATE_POLICY = "aeallpattern:updateTianshuRoutingPolicy";

    private final TianshuPatternSelectorBlockEntity router;
    private int aggregatePriority = -1;
    private int pathPreference;
    private int preferenceFlags;
    private int preferenceOrder = CraftingRoutePolicy.DEFAULT_PREFERENCE_ORDER;

    public TianshuRoutingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, blockEntity(inventory, data.readBlockPos()));
    }

    public TianshuRoutingMenu(
            int id, Inventory inventory, TianshuPatternSelectorBlockEntity router) {
        super(ModMenus.TIANSHU_ROUTING.get(), id, inventory, router);
        this.router = router;
        if (router != null) {
            setLocalPolicy(router.getRoutingPolicy());
        }
        registerClientAction(UPDATE_POLICY, String.class,
                serialized -> applyServerPolicy(CraftingRoutePolicy.deserialize(serialized)));
        addDataSlot(policySlot(0));
        addDataSlot(policySlot(1));
        addDataSlot(policySlot(2));
        addDataSlot(policySlot(3));
    }

    private static TianshuPatternSelectorBlockEntity blockEntity(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof TianshuPatternSelectorBlockEntity router
                ? router
                : null;
    }

    private DataSlot policySlot(int index) {
        return new DataSlot() {
            @Override
            public int get() {
                CraftingRoutePolicy policy = router == null ? getPolicy() : router.getRoutingPolicy();
                return switch (index) {
                    case 0 -> policy.aggregatePriority();
                    case 1 -> policy.pathPreference();
                    case 2 -> flags(policy);
                    default -> policy.preferenceOrder();
                };
            }

            @Override
            public void set(int value) {
                switch (index) {
                    case 0 -> aggregatePriority = value;
                    case 1 -> pathPreference = value;
                    case 2 -> preferenceFlags = value;
                    default -> preferenceOrder = value;
                }
            }
        };
    }

    public CraftingRoutePolicy getPolicy() {
        return new CraftingRoutePolicy(
                aggregatePriority,
                true,
                pathPreference,
                (preferenceFlags & 1) != 0,
                (preferenceFlags & 2) != 0,
                (preferenceFlags & 4) != 0,
                preferenceOrder);
    }

    /** Applies immediately on the client and persists authoritatively on the server. */
    public void updatePolicy(CraftingRoutePolicy policy) {
        CraftingRoutePolicy normalized = forceFeasible(policy);
        setLocalPolicy(normalized);
        if (isClientSide()) {
            sendClientAction(UPDATE_POLICY, normalized.serialize());
        } else {
            applyServerPolicy(normalized);
        }
    }

    private void applyServerPolicy(CraftingRoutePolicy policy) {
        if (router == null || getPlayer().level().isClientSide()) {
            return;
        }
        CraftingRoutePolicy normalized = forceFeasible(policy);
        router.setRoutingPolicy(normalized);
        setLocalPolicy(normalized);
    }

    @Override
    public boolean stillValid(Player player) {
        return router == null || (!router.isRemoved()
                && player.distanceToSqr(router.getBlockPos().getCenter()) <= 64.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void setLocalPolicy(CraftingRoutePolicy policy) {
        aggregatePriority = policy.aggregatePriority();
        pathPreference = policy.pathPreference();
        preferenceFlags = flags(policy);
        preferenceOrder = policy.preferenceOrder();
    }

    private static int flags(CraftingRoutePolicy policy) {
        return (policy.preferStockSurplus() ? 1 : 0)
                | (policy.preferHighYield() ? 2 : 0)
                | (policy.preferFast() ? 4 : 0);
    }

    private static CraftingRoutePolicy forceFeasible(CraftingRoutePolicy policy) {
        CraftingRoutePolicy value = policy == null ? CraftingRoutePolicy.DEFAULT : policy;
        return new CraftingRoutePolicy(
                value.aggregatePriority(), true, value.pathPreference(), value.preferStockSurplus(),
                value.preferHighYield(), value.preferFast(), value.preferenceOrder());
    }
}
