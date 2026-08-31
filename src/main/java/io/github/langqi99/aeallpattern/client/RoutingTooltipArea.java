package io.github.langqi99.aeallpattern.client;

import appeng.client.gui.widgets.ITooltip;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Invisible hover area for explanatory text on non-interactive policy rows. */
public final class RoutingTooltipArea extends AbstractWidget implements ITooltip {
    private final Supplier<List<Component>> tooltip;

    public RoutingTooltipArea(
            int x, int y, int width, int height, Supplier<List<Component>> tooltip) {
        super(x, y, width, height, Component.empty());
        this.tooltip = tooltip;
        active = false;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public List<Component> getTooltipMessage() {
        return tooltip.get();
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }
}
