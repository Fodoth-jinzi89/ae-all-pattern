package io.github.langqi99.aeallpattern.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import com.moakiee.thunderbolt.ae2.crafting.CraftingRoutePolicy;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/** Compact AE-styled, drag-sortable lexicographic route policy editor. */
public final class RoutingPolicyEditor extends AbstractWidget implements ITooltip {
    private static final int ROW_HEIGHT = 17;

    private final Supplier<CraftingRoutePolicy> policy;
    private final Consumer<CraftingRoutePolicy> change;
    private int dragFrom = -1;
    private int dragTo = -1;
    private int hoveredRow = -1;

    public RoutingPolicyEditor(
            int x,
            int y,
            int width,
            Supplier<CraftingRoutePolicy> policy,
            Consumer<CraftingRoutePolicy> change) {
        super(x, y, width, ROW_HEIGHT * CraftingRoutePolicy.CRITERION_COUNT,
                Component.translatable("gui.aeallpattern.routing.order"));
        this.policy = policy;
        this.change = change;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredRow = rowAt(mouseX, mouseY);
        CraftingRoutePolicy current = policy.get();
        for (int row = 0; row < CraftingRoutePolicy.CRITERION_COUNT; row++) {
            int criterion = current.criterionAt(row);
            int rowY = getY() + row * ROW_HEIGHT;
            boolean enabled = enabled(current, criterion);
            boolean hovered = row == hoveredRow;
            boolean dragging = row == dragFrom;
            int background = dragging
                    ? 0xFFD6C6E8
                    : hovered ? 0xFFE7E7EE : enabled ? 0xFFC7C7D2 : 0xFFB8B8C3;
            graphics.fill(getX(), rowY, getX() + width, rowY + ROW_HEIGHT - 1, background);
            graphics.renderOutline(
                    getX(), rowY, width, ROW_HEIGHT - 1,
                    row == dragTo && dragFrom >= 0 ? 0xFFA85BE0 : 0xFF777789);

            drawHandle(graphics, getX() + 4, rowY + 5, dragging ? 0xFFA85BE0 : 0xFFF4F4F7);
            Icon icon = icon(current, criterion);
            var blitter = icon.getBlitter().dest(getX() + 16, rowY).zOffset(3);
            if (!enabled) {
                blitter.opacity(0.45F);
            }
            blitter.blit(graphics);

            int textColor = enabled ? 0xFF303044 : 0xFF777784;
            graphics.drawString(
                    Minecraft.getInstance().font,
                    criterionName(criterion),
                    getX() + 35,
                    rowY + 4,
                    textColor,
                    false);
            Component value = criterionValue(current, criterion);
            graphics.drawString(
                    Minecraft.getInstance().font,
                    value,
                    getX() + width - 5 - Minecraft.getInstance().font.width(value),
                    rowY + 4,
                    textColor,
                    false);
        }
    }

    private static void drawHandle(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 7, y + 1, color);
        graphics.fill(x, y + 3, x + 7, y + 4, color);
        graphics.fill(x, y + 6, x + 7, y + 7, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (row < 0) {
            return false;
        }
        CraftingRoutePolicy current = policy.get();
        if (button == 0) {
            if (mouseX < getX() + 15) {
                // The owning screen captures handle drags before the container
                // can turn them into ordinary slot/widget clicks.
                return false;
            }
            change.accept(cycle(current, current.criterionAt(row), false));
            return true;
        }
        if (button == 1) {
            int criterion = current.criterionAt(row);
            change.accept(cycle(current, criterion, true));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        return button == 0 && dragHandle(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && endHandleDrag()) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Called by the owning screen before normal container click dispatch. */
    public boolean beginHandleDrag(double mouseX, double mouseY) {
        int row = rowAt(mouseX, mouseY);
        if (row < 0 || mouseX >= getX() + 15) {
            return false;
        }
        dragFrom = row;
        dragTo = row;
        return true;
    }

    /** Keeps the drag even when the cursor leaves this widget. */
    public boolean dragHandle(double mouseX, double mouseY) {
        if (dragFrom < 0) {
            return false;
        }
        dragTo = Math.max(0, Math.min(CraftingRoutePolicy.CRITERION_COUNT - 1,
                (int) ((mouseY - getY()) / ROW_HEIGHT)));
        return true;
    }

    /** Commits exactly once on release, avoiding replans during movement. */
    public boolean endHandleDrag() {
        if (dragFrom < 0) {
            return false;
        }
        int from = dragFrom;
        int to = dragTo;
        dragFrom = -1;
        dragTo = -1;
        if (from != to) {
            change.accept(policy.get().moveCriterion(from, to));
        }
        return true;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (!visible || mouseX < getX() || mouseX >= getX() + width
                || mouseY < getY() || mouseY >= getY() + height) {
            return -1;
        }
        return (int) ((mouseY - getY()) / ROW_HEIGHT);
    }

    private static CraftingRoutePolicy cycle(CraftingRoutePolicy policy, int criterion, boolean reverse) {
        return switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> {
                int next = reverse
                        ? (policy.pathPreference() <= -1 ? 0 : policy.pathPreference() - 1)
                        : (policy.pathPreference() >= 1 ? 0 : policy.pathPreference() + 1);
                yield policy.withPathPreference(next);
            }
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS ->
                    policy.withStockSurplus(!policy.preferStockSurplus());
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD ->
                    policy.withHighYield(!policy.preferHighYield());
            case CraftingRoutePolicy.CRITERION_FAST -> policy.withFast(!policy.preferFast());
            default -> policy;
        };
    }

    private static boolean enabled(CraftingRoutePolicy policy, int criterion) {
        return switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> policy.pathPreference() != 0;
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS -> policy.preferStockSurplus();
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD -> policy.preferHighYield();
            case CraftingRoutePolicy.CRITERION_FAST -> policy.preferFast();
            default -> false;
        };
    }

    private static Icon icon(CraftingRoutePolicy policy, int criterion) {
        return switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> policy.pathPreference() < 0
                    ? Icon.ARROW_LEFT
                    : policy.pathPreference() > 0 ? Icon.ARROW_RIGHT : Icon.S_CYCLE;
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS -> policy.preferStockSurplus()
                    ? Icon.FULLNESS_FULL : Icon.FULLNESS_EMPTY;
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD -> Icon.ARROW_UP;
            case CraftingRoutePolicy.CRITERION_FAST -> policy.preferFast() ? Icon.COG : Icon.COG_DISABLED;
            default -> Icon.S_CYCLE;
        };
    }

    private static Component criterionName(int criterion) {
        return Component.translatable(switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> "gui.aeallpattern.routing.path";
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS -> "gui.aeallpattern.routing.surplus";
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD -> "gui.aeallpattern.routing.yield";
            case CraftingRoutePolicy.CRITERION_FAST -> "gui.aeallpattern.routing.waiting";
            default -> "gui.aeallpattern.routing.disabled";
        });
    }

    private static Component criterionValue(CraftingRoutePolicy policy, int criterion) {
        if (!enabled(policy, criterion)) {
            return Component.translatable("gui.aeallpattern.routing.disabled");
        }
        return Component.translatable(switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> policy.pathPreference() < 0
                    ? "gui.aeallpattern.routing.path_short"
                    : "gui.aeallpattern.routing.path_long";
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS -> "gui.aeallpattern.routing.more";
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD -> "gui.aeallpattern.routing.high";
            case CraftingRoutePolicy.CRITERION_FAST -> "gui.aeallpattern.routing.waiting_value";
            default -> "gui.aeallpattern.routing.disabled";
        });
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (hoveredRow < 0) {
            return List.of();
        }
        int criterion = policy.get().criterionAt(hoveredRow);
        return List.of(
                criterionName(criterion),
                criterionDescription(policy.get(), criterion),
                Component.translatable("gui.aeallpattern.routing.order_rule"),
                Component.translatable("gui.aeallpattern.routing.drag_hint"),
                Component.translatable("gui.aeallpattern.routing.toggle_hint"));
    }

    private static Component criterionDescription(CraftingRoutePolicy policy, int criterion) {
        return Component.translatable(switch (criterion) {
            case CraftingRoutePolicy.CRITERION_PATH -> policy.pathPreference() < 0
                    ? "gui.aeallpattern.routing.path_details_short"
                    : policy.pathPreference() > 0
                            ? "gui.aeallpattern.routing.path_details_long"
                            : "gui.aeallpattern.routing.path_details_off";
            case CraftingRoutePolicy.CRITERION_STOCK_SURPLUS ->
                    "gui.aeallpattern.routing.surplus_details";
            case CraftingRoutePolicy.CRITERION_HIGH_YIELD ->
                    "gui.aeallpattern.routing.yield_details";
            case CraftingRoutePolicy.CRITERION_FAST ->
                    "gui.aeallpattern.routing.waiting_details";
            default -> "gui.aeallpattern.routing.disabled";
        });
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible && hoveredRow >= 0;
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
