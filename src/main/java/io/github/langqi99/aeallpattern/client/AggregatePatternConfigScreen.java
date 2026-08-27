package io.github.langqi99.aeallpattern.client;

import io.github.langqi99.aeallpattern.aggregate.AggregatePatternConfigMenu;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternRef;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Compact AE-colored editor for one held aggregate pattern. */
public final class AggregatePatternConfigScreen extends AbstractContainerScreen<AggregatePatternConfigMenu> {
    private Button splitButton;
    private Button ignoreOutputButton;
    private Button skipProbabilisticMainButton;
    private Button ignoreProbabilisticByproductsButton;

    public AggregatePatternConfigScreen(
            AggregatePatternConfigMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
        imageWidth = 196;
        imageHeight = 180;
        titleLabelX = 34;
        titleLabelY = 10;
    }

    @Override
    protected void init() {
        super.init();
        splitButton = addRenderableWidget(Button.builder(splitLabel(), ignored -> toggle(
                        AggregatePatternConfigMenu.TOGGLE_SPLIT_SAME_ITEMS))
                .bounds(leftPos + 12, topPos + 38, 172, 24)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.aeallpattern.aggregate_config.split_same_items.tooltip")))
                .build());
        ignoreOutputButton = addRenderableWidget(Button.builder(ignoreOutputLabel(), ignored -> toggle(
                        AggregatePatternConfigMenu.TOGGLE_IGNORE_OUTPUT_COMPONENTS))
                .bounds(leftPos + 12, topPos + 70, 172, 24)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.aeallpattern.aggregate_config.ignore_output_nbt.tooltip")))
                .build());
        skipProbabilisticMainButton = addRenderableWidget(Button.builder(
                        skipProbabilisticMainLabel(), ignored -> toggle(
                                AggregatePatternConfigMenu.TOGGLE_SKIP_PROBABILISTIC_MAIN_OUTPUT))
                .bounds(leftPos + 12, topPos + 102, 172, 24)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.aeallpattern.aggregate_config.skip_probabilistic_main.tooltip")))
                .build());
        ignoreProbabilisticByproductsButton = addRenderableWidget(Button.builder(
                        ignoreProbabilisticByproductsLabel(), ignored -> toggle(
                                AggregatePatternConfigMenu.TOGGLE_IGNORE_PROBABILISTIC_BYPRODUCTS))
                .bounds(leftPos + 12, topPos + 134, 172, 24)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.aeallpattern.aggregate_config.ignore_probabilistic_byproducts.tooltip")))
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        splitButton.setMessage(splitLabel());
        ignoreOutputButton.setMessage(ignoreOutputLabel());
        skipProbabilisticMainButton.setMessage(skipProbabilisticMainLabel());
        ignoreProbabilisticByproductsButton.setMessage(ignoreProbabilisticByproductsLabel());
    }

    private void toggle(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component splitLabel() {
        return optionLabel(
                "gui.aeallpattern.aggregate_config.split_same_items",
                menu.getOptions().splitSameItems());
    }

    private Component ignoreOutputLabel() {
        return optionLabel(
                "gui.aeallpattern.aggregate_config.ignore_output_nbt",
                menu.getOptions().ignoreOutputComponents());
    }

    private Component skipProbabilisticMainLabel() {
        return optionLabel(
                "gui.aeallpattern.aggregate_config.skip_probabilistic_main",
                menu.getOptions().skipProbabilisticMainOutput());
    }

    private Component ignoreProbabilisticByproductsLabel() {
        return optionLabel(
                "gui.aeallpattern.aggregate_config.ignore_probabilistic_byproducts",
                menu.getOptions().ignoreProbabilisticByproducts());
    }

    private static Component optionLabel(String key, boolean enabled) {
        return Component.translatable(
                "gui.aeallpattern.aggregate_config.option",
                Component.translatable(enabled
                        ? "gui.aeallpattern.aggregate_config.enabled"
                        : "gui.aeallpattern.aggregate_config.disabled"),
                Component.translatable(key));
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
                164,
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
