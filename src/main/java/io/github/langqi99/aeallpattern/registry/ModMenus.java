package io.github.langqi99.aeallpattern.registry;

import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.tianshu.TianshuRoutingMenu;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternConfigMenu;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternSelectionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AeAllPattern.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TianshuRoutingMenu>> TIANSHU_ROUTING =
            MENUS.register("tianshu_routing", () -> IMenuTypeExtension.create(TianshuRoutingMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AggregatePatternConfigMenu>> AGGREGATE_PATTERN_CONFIG =
            MENUS.register("aggregate_pattern_config", () ->
                    IMenuTypeExtension.create(AggregatePatternConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AggregatePatternSelectionMenu>> AGGREGATE_PATTERN_SELECTION =
            MENUS.register("aggregate_pattern_selection", () ->
                    IMenuTypeExtension.create(AggregatePatternSelectionMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
