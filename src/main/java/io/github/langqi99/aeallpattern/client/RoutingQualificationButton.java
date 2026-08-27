package io.github.langqi99.aeallpattern.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/** Fixed-position recipe-qualification switch shown beneath mandatory feasibility. */
public final class RoutingQualificationButton extends AbstractWidget implements ITooltip {
    private final Supplier<Boolean> enabled;
    private final Consumer<Boolean> change;

    public RoutingQualificationButton(
            int x, int y, int width, Supplier<Boolean> enabled, Consumer<Boolean> change) {
        super(x, y, width, 14, Component.translatable("gui.aeallpattern.routing.byproduct_orders"));
        this.enabled = enabled;
        this.change = change;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean value = enabled.get();
        int background = isHovered() ? 0xFFE7E7EE : value ? 0xFFC7C7D2 : 0xFFB8B8C3;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.renderOutline(getX(), getY(), width, height, 0xFF777789);
        (value ? Icon.S_SUBSTITUTION_ENABLED : Icon.S_SUBSTITUTION_DISABLED)
                .getBlitter().dest(getX() + 1, getY() - 1).blit(graphics);

        var font = Minecraft.getInstance().font;
        Component label = Component.translatable("gui.aeallpattern.routing.byproduct_orders_short");
        Component state = Component.translatable(value
                ? "gui.aeallpattern.routing.qualification_on"
                : "gui.aeallpattern.routing.qualification_off");
        int color = value ? 0xFF303044 : 0xFF666674;
        graphics.drawString(font, label, getX() + 19, getY() + 3, color, false);
        graphics.drawString(font, state, getX() + width - 4 - font.width(state), getY() + 3, color, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        change.accept(!enabled.get());
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(
                Component.translatable("gui.aeallpattern.routing.byproduct_orders"),
                Component.translatable("gui.aeallpattern.routing.byproduct_orders_details"),
                Component.translatable(enabled.get()
                        ? "gui.aeallpattern.routing.byproduct_orders_enabled"
                        : "gui.aeallpattern.routing.byproduct_orders_disabled"));
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
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
