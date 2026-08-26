package com.moakiee.thunderbolt.ae2.crafting;

/**
 * Per-order recipe routing preferences consumed by the fast crafting planner.
 *
 * <p>The policy is deliberately compact so host mods can attach it to one
 * calculation without mutating global recipe state.</p>
 */
public record CraftingRoutePolicy(
        int aggregatePriority,
        boolean requireFeasible,
        int pathPreference,
        boolean preferStockSurplus,
        boolean preferHighYield,
        boolean preferFast,
        int preferenceOrder) {
    public static final int CRITERION_PATH = 0;
    public static final int CRITERION_STOCK_SURPLUS = 1;
    public static final int CRITERION_HIGH_YIELD = 2;
    public static final int CRITERION_FAST = 3;
    public static final int CRITERION_COUNT = 4;
    /** Stock surplus -> waiting time -> short path -> single-run output. */
    public static final int DEFAULT_PREFERENCE_ORDER = 0x2031;

    public static final CraftingRoutePolicy DEFAULT =
            new CraftingRoutePolicy(-1, true, -1, true, true, true, DEFAULT_PREFERENCE_ORDER);

    public static final int MIN_PRIORITY = -32;
    public static final int MAX_PRIORITY = 32;

    public CraftingRoutePolicy {
        aggregatePriority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, aggregatePriority));
        pathPreference = Math.max(-1, Math.min(1, pathPreference));
        preferenceOrder = normalizeOrder(preferenceOrder);
    }

    public CraftingRoutePolicy withAggregatePriority(int value) {
        return new CraftingRoutePolicy(
                value, requireFeasible, pathPreference, preferStockSurplus, preferHighYield, preferFast,
                preferenceOrder);
    }

    public CraftingRoutePolicy withPathPreference(int value) {
        return new CraftingRoutePolicy(
                aggregatePriority, requireFeasible, value, preferStockSurplus, preferHighYield, preferFast,
                preferenceOrder);
    }

    public CraftingRoutePolicy withStockSurplus(boolean value) {
        return new CraftingRoutePolicy(
                aggregatePriority, requireFeasible, pathPreference, value, preferHighYield, preferFast,
                preferenceOrder);
    }

    public CraftingRoutePolicy withHighYield(boolean value) {
        return new CraftingRoutePolicy(
                aggregatePriority, requireFeasible, pathPreference, preferStockSurplus, value, preferFast,
                preferenceOrder);
    }

    public CraftingRoutePolicy withFast(boolean value) {
        return new CraftingRoutePolicy(
                aggregatePriority, requireFeasible, pathPreference, preferStockSurplus, preferHighYield, value,
                preferenceOrder);
    }

    public CraftingRoutePolicy withPreferenceOrder(int value) {
        return new CraftingRoutePolicy(
                aggregatePriority, requireFeasible, pathPreference, preferStockSurplus, preferHighYield, preferFast,
                value);
    }

    public int criterionAt(int position) {
        if (position < 0 || position >= CRITERION_COUNT) {
            throw new IndexOutOfBoundsException(position);
        }
        return (preferenceOrder >>> (position * 4)) & 0xF;
    }

    public CraftingRoutePolicy moveCriterion(int from, int to) {
        if (from < 0 || from >= CRITERION_COUNT || to < 0 || to >= CRITERION_COUNT || from == to) {
            return this;
        }
        int[] order = new int[CRITERION_COUNT];
        for (int i = 0; i < CRITERION_COUNT; i++) {
            order[i] = criterionAt(i);
        }
        int moved = order[from];
        if (from < to) {
            System.arraycopy(order, from + 1, order, from, to - from);
        } else {
            System.arraycopy(order, to, order, to + 1, from - to);
        }
        order[to] = moved;
        return withPreferenceOrder(packOrder(order));
    }

    public String serialize() {
        return aggregatePriority + "," + (requireFeasible ? 1 : 0) + "," + pathPreference + ","
                + (preferStockSurplus ? 1 : 0) + "," + (preferHighYield ? 1 : 0) + ","
                + (preferFast ? 1 : 0) + "," + preferenceOrder;
    }

    public static CraftingRoutePolicy deserialize(String serialized) {
        if (serialized == null) {
            return DEFAULT;
        }
        String[] values = serialized.split(",", -1);
        if (values.length != 6 && values.length != 7) {
            return DEFAULT;
        }
        try {
            return new CraftingRoutePolicy(
                    Integer.parseInt(values[0]),
                    !values[1].equals("0"),
                    Integer.parseInt(values[2]),
                    values[3].equals("1"),
                    values[4].equals("1"),
                    values[5].equals("1"),
                    values.length == 7 ? Integer.parseInt(values[6]) : DEFAULT_PREFERENCE_ORDER);
        } catch (NumberFormatException ignored) {
            return DEFAULT;
        }
    }

    private static int normalizeOrder(int packed) {
        boolean[] seen = new boolean[CRITERION_COUNT];
        int[] order = new int[CRITERION_COUNT];
        for (int i = 0; i < CRITERION_COUNT; i++) {
            int criterion = (packed >>> (i * 4)) & 0xF;
            if (criterion >= CRITERION_COUNT || seen[criterion]) {
                return DEFAULT_PREFERENCE_ORDER;
            }
            seen[criterion] = true;
            order[i] = criterion;
        }
        return packOrder(order);
    }

    private static int packOrder(int[] order) {
        int packed = 0;
        for (int i = 0; i < CRITERION_COUNT; i++) {
            packed |= (order[i] & 0xF) << (i * 4);
        }
        return packed;
    }
}
