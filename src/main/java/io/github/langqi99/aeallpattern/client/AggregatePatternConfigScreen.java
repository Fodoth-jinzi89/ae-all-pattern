package io.github.langqi99.aeallpattern.client;

import io.github.langqi99.aeallpattern.aggregate.AggregatePatternConfigMenu;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternRef;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Compact AE-colored editor for one held aggregate pattern. */
public final class AggregatePatternConfigScreen extends AbstractContainerScreen<AggregatePatternConfigMenu> {
    public AggregatePatternConfigScreen(
            AggregatePatternConfigMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
        imageWidth = 196;
        imageHeight = 128;
        titleLabelX = 34;
        titleLabelY = 10;
    }

    @Override
    protected void init() {
        super.init();
        addOption(38,
                "gui.aeallpattern.aggregate_config.split_same_items",
                "gui.aeallpattern.aggregate_config.split_same_items.tooltip",
                () -> menu.getOptions().splitSameItems(),
                AggregatePatternConfigMenu.TOGGLE_SPLIT_SAME_ITEMS);
        addOption(55,
                "gui.aeallpattern.aggregate_config.ignore_output_nbt",
                "gui.aeallpattern.aggregate_config.ignore_output_nbt.tooltip",
                () -> menu.getOptions().ignoreOutputComponents(),
                AggregatePatternConfigMenu.TOGGLE_IGNORE_OUTPUT_COMPONENTS);
        addOption(72,
                "gui.aeallpattern.aggregate_config.skip_probabilistic_main",
                "gui.aeallpattern.aggregate_config.skip_probabilistic_main.tooltip",
                () -> menu.getOptions().skipProbabilisticMainOutput(),
                AggregatePatternConfigMenu.TOGGLE_SKIP_PROBABILISTIC_MAIN_OUTPUT);
        addOption(89,
                "gui.aeallpattern.aggregate_config.ignore_probabilistic_byproducts",
                "gui.aeallpattern.aggregate_config.ignore_probabilistic_byproducts.tooltip",
                () -> menu.getOptions().ignoreProbabilisticByproducts(),
                AggregatePatternConfigMenu.TOGGLE_IGNORE_PROBABILISTIC_BYPRODUCTS);
    }

    private void addOption(
            int y,
            String labelKey,
            String tooltipKey,
            java.util.function.BooleanSupplier enabled,
            int toggleId) {
        addRenderableWidget(new AggregateConfigOptionButton(
                leftPos + 12,
                topPos + y,
                172,
                Component.translatable(labelKey),
                Component.translatable(tooltipKey),
                enabled,
                () -> toggle(toggleId)));
    }

    private void toggle(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFD8D8E2);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF4B4B61);
        graphics.renderOutline(leftPos + 3, topPos + 3, imageWidth - 6, imageHeight - 6, 0xFFF2F2F7);
        graphics.fill(leftPos + 8, topPos + 31, leftPos + imageWidth - 8, topPos + 32, 0xFF777789);

        ItemStack machine = machineStack();
        if (!machine.isEmpty()) {
            graphics.renderItem(machine, leftPos + 10, topPos + 7);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF3A3A50, false);
        graphics.drawString(
                font,
                Component.translatable("gui.aeallpattern.aggregate_config.hint"),
                12,
                112,
                0xFF67677A,
                false);
    }

    private ItemStack machineStack() {
        AggregatePatternRef ref = menu.stack().get(ModDataComponents.AGGREGATE_PATTERN.get());
        if (ref == null) {
            return ItemStack.EMPTY;
        }
        var block = BuiltInRegistries.BLOCK.get(ref.catalystId());
        return block.asItem().getDefaultInstance();
    }
}
