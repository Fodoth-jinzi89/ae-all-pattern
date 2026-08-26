package io.github.langqi99.aeallpattern.mixin.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.moakiee.thunderbolt.ae2.crafting.CraftingRoutePolicy;
import io.github.langqi99.aeallpattern.client.RoutingOptionButton;
import io.github.langqi99.aeallpattern.client.RoutingPolicyEditor;
import io.github.langqi99.aeallpattern.client.RoutingPolicyPanelBackground;
import io.github.langqi99.aeallpattern.client.RoutingTooltipArea;
import io.github.langqi99.aeallpattern.tianshu.CraftConfirmRoutingMenu;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a compact, temporary route-policy popover to AE's native confirmation screen. */
@Mixin(value = CraftConfirmScreen.class, remap = false)
public abstract class CraftConfirmScreenRoutingMixin extends AEBaseScreen<CraftConfirmMenu> {
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

    @Inject(method = "updateBeforeRender", at = @At("HEAD"))
    private void aeallpattern$createRouteEditor(CallbackInfo ci) {
        if (aeallpattern$routeButton != null) {
            return;
        }

        int panelX = getGuiLeft() + 74;
        int panelY = getGuiTop() + 17;
        aeallpattern$panel = addRenderableWidget(new RoutingPolicyPanelBackground(
                panelX, panelY, 160, 107));

        aeallpattern$priorityField = new AETextField(style, font, panelX + 113, panelY + 3, 40, 12);
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
                152,
                14,
                () -> List.of(
                        Component.translatable("gui.aeallpattern.routing.feasible"),
                        Component.translatable("gui.aeallpattern.routing.feasible_details"),
                        Component.translatable("gui.aeallpattern.routing.feasible_locked"))));

        aeallpattern$editor = addRenderableWidget(new RoutingPolicyEditor(
                panelX + 4,
                panelY + 37,
                152,
                this::policy,
                this::update));

        aeallpattern$routeButton = addRenderableWidget(new RoutingOptionButton(
                getGuiLeft() + imageWidth - 27,
                getGuiTop() + 2,
                20,
                () -> Icon.COG,
                Component::empty,
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

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void aeallpattern$syncRouteEditor(CallbackInfo ci) {
        if (aeallpattern$routeButton == null) {
            return;
        }
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
        return super.mouseClicked(mouseX, mouseY, button);
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
