package io.github.langqi99.aeallpattern.mixin.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.moakiee.thunderbolt.ae2.crafting.CraftingRoutePolicy;
import io.github.langqi99.aeallpattern.client.RoutingOptionButton;
import io.github.langqi99.aeallpattern.client.RoutingPolicyEditor;
import io.github.langqi99.aeallpattern.client.RoutingPolicyPanelBackground;
import io.github.langqi99.aeallpattern.client.RoutingTooltipArea;
import io.github.langqi99.aeallpattern.tianshu.CraftConfirmRoutingMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a compact, temporary route-policy popover to AE's native confirmation screen. */
@Mixin(value = CraftConfirmScreen.class, remap = false)
public abstract class CraftConfirmScreenRoutingMixin extends AEBaseScreen<CraftConfirmMenu> {
    @Unique
    private static final int AEALLPATTERN_PANEL_WIDTH = 184;
    @Unique
    private static final int AEALLPATTERN_PANEL_HEIGHT = 120;

    @Unique
    private RoutingOptionButton aeallpattern$routeButton;
    @Unique
    private RoutingPolicyPanelBackground aeallpattern$panel;
    @Unique
    private RoutingPolicyEditor aeallpattern$editor;
    @Unique
    private AETextField aeallpattern$priorityField;
    @Unique
    private RoutingTooltipArea aeallpattern$feasibilityHelp;
    @Unique
    private boolean aeallpattern$expanded;
    @Unique
    private boolean aeallpattern$syncingPriority;

    protected CraftConfirmScreenRoutingMixin(
            CraftConfirmMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aeallpattern$addRouteToolbarButton(
            CraftConfirmMenu menu,
            Inventory inventory,
            Component title,
            ScreenStyle style,
            CallbackInfo ci) {
        aeallpattern$routeButton = addToLeftToolbar(new RoutingOptionButton(
                () -> Icon.COG,
                ignored -> aeallpattern$expanded = !aeallpattern$expanded,
                null,
                () -> routingMenu().aeallpattern$isRoutingAvailable()
                        ? List.of(
                                Component.translatable("gui.aeallpattern.routing.order_settings"),
                                Component.translatable("gui.aeallpattern.routing.order_settings_hint"))
                        : List.of(
                                Component.translatable("gui.aeallpattern.routing.order_settings"),
                                Component.translatable("gui.aeallpattern.routing.order_settings_unavailable"))));
    }

    @Inject(method = "updateBeforeRender", at = @At("HEAD"))
    private void aeallpattern$createRouteEditor(CallbackInfo ci) {
        if (aeallpattern$panel != null) {
            return;
        }

        int panelX = getGuiLeft() + (imageWidth - AEALLPATTERN_PANEL_WIDTH) / 2;
        int panelY = getGuiTop() + (imageHeight - AEALLPATTERN_PANEL_HEIGHT) / 2;
        aeallpattern$panel = addRenderableWidget(new RoutingPolicyPanelBackground(
                panelX, panelY, AEALLPATTERN_PANEL_WIDTH, AEALLPATTERN_PANEL_HEIGHT));

        aeallpattern$priorityField = new AETextField(style, font, panelX + 137, panelY + 3, 40, 12);
        aeallpattern$priorityField.setBordered(false);
        aeallpattern$priorityField.setMaxLength(3);
        aeallpattern$priorityField.setValue(Integer.toString(policy().aggregatePriority()));
        aeallpattern$priorityField.setResponder(this::aeallpattern$priorityChanged);
        aeallpattern$priorityField.setTooltipMessage(List.of(
                Component.translatable("gui.aeallpattern.routing.aggregate_priority"),
                Component.translatable("gui.aeallpattern.routing.priority_semantics")));
        addRenderableWidget(aeallpattern$priorityField);

        aeallpattern$feasibilityHelp = addRenderableWidget(new RoutingTooltipArea(
                panelX + 4,
                panelY + 20,
                AEALLPATTERN_PANEL_WIDTH - 8,
                14,
                () -> List.of(
                        Component.translatable("gui.aeallpattern.routing.feasible"),
                        Component.translatable("gui.aeallpattern.routing.feasible_details"),
                        Component.translatable("gui.aeallpattern.routing.feasible_locked"))));

        aeallpattern$editor = addRenderableWidget(new RoutingPolicyEditor(
                panelX + 4,
                panelY + 37,
                AEALLPATTERN_PANEL_WIDTH - 8,
                this::policy,
                this::update));
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void aeallpattern$syncRouteEditor(CallbackInfo ci) {
        if (aeallpattern$routeButton == null || aeallpattern$panel == null) {
            return;
        }
        aeallpattern$layoutPopup();
        boolean available = routingMenu().aeallpattern$isRoutingAvailable();
        aeallpattern$routeButton.visible = true;
        aeallpattern$routeButton.active = available;
        boolean showPanel = available && aeallpattern$expanded;
        aeallpattern$panel.visible = showPanel;
        aeallpattern$priorityField.visible = showPanel;
        aeallpattern$feasibilityHelp.visible = showPanel;
        aeallpattern$editor.visible = showPanel;
        if (!showPanel) {
            aeallpattern$priorityField.setFocused(false);
        } else if (!aeallpattern$priorityField.isFocused()) {
            String expected = Integer.toString(policy().aggregatePriority());
            if (!expected.equals(aeallpattern$priorityField.getValue())) {
                aeallpattern$syncingPriority = true;
                aeallpattern$priorityField.setValue(expected);
                aeallpattern$syncingPriority = false;
            }
        }
    }

    @Unique
    private void aeallpattern$layoutPopup() {
        int panelX = getGuiLeft() + (imageWidth - AEALLPATTERN_PANEL_WIDTH) / 2;
        int panelY = getGuiTop() + (imageHeight - AEALLPATTERN_PANEL_HEIGHT) / 2;
        aeallpattern$panel.setX(panelX);
        aeallpattern$panel.setY(panelY);
        aeallpattern$priorityField.setX(panelX + 137);
        aeallpattern$priorityField.setY(panelY + 3);
        aeallpattern$feasibilityHelp.setX(panelX + 4);
        aeallpattern$feasibilityHelp.setY(panelY + 20);
        aeallpattern$editor.setX(panelX + 4);
        aeallpattern$editor.setY(panelY + 37);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!aeallpattern$expanded || !routingMenu().aeallpattern$isRoutingAvailable()
                || aeallpattern$panel == null) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 600.0F);
        try {
            graphics.fill(
                    getGuiLeft() + 1,
                    getGuiTop() + 1,
                    getGuiLeft() + imageWidth - 1,
                    getGuiTop() + imageHeight - 1,
                    0x72000000);
            aeallpattern$panel.render(graphics, mouseX, mouseY, partialTick);
            aeallpattern$priorityField.render(graphics, mouseX, mouseY, partialTick);
            aeallpattern$feasibilityHelp.render(graphics, mouseX, mouseY, partialTick);
            aeallpattern$editor.render(graphics, mouseX, mouseY, partialTick);

            if (!aeallpattern$renderTooltip(graphics, mouseX, mouseY, aeallpattern$editor)
                    && !aeallpattern$renderTooltip(graphics, mouseX, mouseY, aeallpattern$feasibilityHelp)) {
                aeallpattern$renderTooltip(graphics, mouseX, mouseY, aeallpattern$priorityField);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    @Unique
    private boolean aeallpattern$renderTooltip(
            GuiGraphics graphics, int mouseX, int mouseY, ITooltip tooltip) {
        Rect2i area = tooltip.getTooltipArea();
        if (!tooltip.isTooltipAreaVisible()
                || mouseX < area.getX() || mouseX >= area.getX() + area.getWidth()
                || mouseY < area.getY() || mouseY >= area.getY() + area.getHeight()) {
            return false;
        }
        List<Component> lines = tooltip.getTooltipMessage();
        if (lines.isEmpty()) {
            return false;
        }
        drawTooltip(graphics, mouseX, mouseY, lines);
        return true;
    }

    @Unique
    private void aeallpattern$priorityChanged(String text) {
        if (aeallpattern$syncingPriority || text == null || !text.matches("-?\\d{1,2}")) {
            return;
        }
        try {
            int value = Integer.parseInt(text);
            if (value >= CraftingRoutePolicy.MIN_PRIORITY && value <= CraftingRoutePolicy.MAX_PRIORITY
                    && value != policy().aggregatePriority()) {
                update(policy().withAggregatePriority(value));
            }
        } catch (NumberFormatException ignored) {
            // A partial edit such as just '-' is allowed until it becomes valid.
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && aeallpattern$editor != null && aeallpattern$editor.visible
                && aeallpattern$editor.beginHandleDrag(mouseX, mouseY)) {
            return true;
        }
        if (aeallpattern$expanded && aeallpattern$panel != null) {
            if (aeallpattern$routeButton.isMouseOver(mouseX, mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            if (!aeallpattern$isInsidePanel(mouseX, mouseY)) {
                aeallpattern$expanded = false;
                return true;
            }
            if (aeallpattern$editor.visible && aeallpattern$editor.isMouseOver(mouseX, mouseY)) {
                // Dispatch directly so AE's crafting table and tooltip layer
                // cannot consume a row click before it toggles the criterion.
                return aeallpattern$editor.mouseClicked(mouseX, mouseY, button);
            }
            if (aeallpattern$priorityField.visible
                    && aeallpattern$priorityField.isMouseOver(mouseX, mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Unique
    private boolean aeallpattern$isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= aeallpattern$panel.getX()
                && mouseX < aeallpattern$panel.getX() + aeallpattern$panel.getWidth()
                && mouseY >= aeallpattern$panel.getY()
                && mouseY < aeallpattern$panel.getY() + aeallpattern$panel.getHeight();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void aeallpattern$closePopupWithEscape(
            int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (aeallpattern$expanded && keyCode == 256) {
            aeallpattern$expanded = false;
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && aeallpattern$editor != null
                && aeallpattern$editor.dragHandle(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && aeallpattern$editor != null && aeallpattern$editor.endHandleDrag()) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Unique
    private void update(CraftingRoutePolicy policy) {
        routingMenu().aeallpattern$updateRoutePolicy(policy);
    }

    @Unique
    private CraftingRoutePolicy policy() {
        return routingMenu().aeallpattern$getRoutePolicy();
    }

    @Unique
    private CraftConfirmRoutingMenu routingMenu() {
        return (CraftConfirmRoutingMenu) menu;
    }
}
