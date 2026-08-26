package io.github.langqi99.aeallpattern.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/** AE-style white icon button with an optional live value and right-click action. */
public final class RoutingOptionButton extends Button implements ITooltip {
    private final Supplier<Icon> icon;
    private final Supplier<Component> value;
    private final Supplier<List<Component>> tooltip;
    private final Runnable rightClick;

    public RoutingOptionButton(
            int x,
            int y,
            int width,
            Supplier<Icon> icon,
            Supplier<Component> value,
            OnPress onPress,
            Runnable rightClick,
            Supplier<List<Component>> tooltip) {
        super(x, y, width, 20, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.icon = icon;
        this.value = value;
        this.tooltip = tooltip;
        this.rightClick = rightClick;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = isHoveredOrFocused() ? 0xFFE4E4EC : 0xFFB7B7C5;
        int border = active ? 0xFF4B4B60 : 0xFF747486;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.renderOutline(getX(), getY(), width, height, border);

        var blitter = icon.get().getBlitter().dest(getX() + 2, getY() + 2).zOffset(3);
        if (!active) {
            blitter.opacity(0.55F);
        }
        blitter.blit(graphics);

        Component text = value.get();
        if (text != null && !text.getString().isEmpty()) {
            int color = active ? 0xFF303044 : 0xFF777784;
            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    text,
                    getX() + 18 + Math.max(0, width - 18) / 2,
                    getY() + 6,
                    color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && active && visible && isMouseOver(mouseX, mouseY) && rightClick != null) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            rightClick.run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        return visible && !getTooltipMessage().isEmpty();
    }
}
