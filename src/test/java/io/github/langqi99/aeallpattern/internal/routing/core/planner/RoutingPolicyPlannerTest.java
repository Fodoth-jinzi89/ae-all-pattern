package io.github.langqi99.aeallpattern.internal.routing.core.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.CraftingRoutePolicy;
import io.github.langqi99.aeallpattern.internal.routing.ae2.crafting.RoutingPatternMetadata;

final class RoutingPolicyPlannerTest {
    private record RouteMetadata(String id, boolean aggregate, int ticks)
            implements RoutingPatternMetadata {
        @Override
        public boolean isAggregatePattern() {
            return aggregate;
        }

        @Override
        public int processingTicks() {
            return ticks;
        }

        @Override
        public String stableRouteId() {
            return id;
        }
    }

    private static long firingsOf(CraftPlan<String> plan, CraftPattern<String> pattern) {
        return plan.firings().getOrDefault(pattern, 0L);
    }

    @Test
    void aggregatePriorityAndPathDirectionSelectDifferentRoutes() {
        var aggregate = new CraftPattern<>(
                "A", 1, List.of(CraftInput.of("aggregateRaw", 1)),
                new RouteMetadata("aggregate", true, 1));
        var direct = new CraftPattern<>("A", 1, List.of(CraftInput.of("directRaw", 1)), "direct");
        var deep = new CraftPattern<>("A", 1, List.of(CraftInput.of("B", 1)), "deep");
        var makeB = new CraftPattern<>("B", 1, List.of(CraftInput.of("deepRaw", 1)), "makeB");
        var graph = CraftGraph.<String>builder()
                .pattern(aggregate).pattern(direct).pattern(deep).pattern(makeB)
                .stock("aggregateRaw", 10).stock("directRaw", 10).stock("deepRaw", 10)
                .build();

        var aggregateFirst = CraftPlannerV2.plan(
                graph, "A", 1, CraftingRoutePolicy.DEFAULT.withAggregatePriority(1));
        var shortest = CraftPlannerV2.plan(
                graph, "A", 1, CraftingRoutePolicy.DEFAULT.withAggregatePriority(-1)
                        .withPathPreference(-1));
        var deepest = CraftPlannerV2.plan(
                graph, "A", 1, CraftingRoutePolicy.DEFAULT.withAggregatePriority(-1)
                        .withPathPreference(1));

        assertEquals(1L, firingsOf(aggregateFirst, aggregate));
        assertEquals(1L, firingsOf(shortest, direct));
        assertEquals(1L, firingsOf(deepest, deep));
        assertEquals(1L, firingsOf(deepest, makeB));
    }

    @Test
    void yieldAndStockDirectionsWorkBothWays() {
        var smallScarce = new CraftPattern<>(
                "A", 1, List.of(CraftInput.of("scarce", 1)), "smallScarce");
        var largeAbundant = new CraftPattern<>(
                "A", 4, List.of(CraftInput.of("abundant", 1)), "largeAbundant");
        var graph = CraftGraph.<String>builder()
                .pattern(smallScarce).pattern(largeAbundant)
                .stock("scarce", 2).stock("abundant", 20)
                .build();
        var neutral = CraftingRoutePolicy.DEFAULT.withPathPreference(0).withFast(false);

        var more = CraftPlannerV2.plan(
                graph, "A", 1, neutral.withStockSurplusPreference(1).withYieldPreference(1));
        var less = CraftPlannerV2.plan(
                graph, "A", 1, neutral.withStockSurplusPreference(-1).withYieldPreference(-1));

        assertEquals(1L, firingsOf(more, largeAbundant));
        assertEquals(1L, firingsOf(less, smallScarce));
    }

    @Test
    void draggedCriterionOrderIsLexicographicAndWaitingPrefersIdleProviders() {
        var shortLowYield = new CraftPattern<>(
                "A", 1, List.of(CraftInput.of("shortRaw", 1)), List.of(),
                new RouteMetadata("short", false, 1), 0, 2);
        var deepHighYield = new CraftPattern<>(
                "A", 3, List.of(CraftInput.of("B", 2)), List.of(),
                new RouteMetadata("deep", false, 200), 1, 1);
        var makeB = new CraftPattern<>("B", 1, List.of(CraftInput.of("deepRaw", 1)), "makeB");
        var graph = CraftGraph.<String>builder()
                .pattern(shortLowYield).pattern(deepHighYield).pattern(makeB)
                .stock("shortRaw", 10).stock("deepRaw", 10)
                .build();
        var pathThenYield = CraftingRoutePolicy.DEFAULT
                .withStockSurplusPreference(0).withFast(false)
                .withPathPreference(-1).withYieldPreference(1);
        var yieldThenPath = pathThenYield.moveCriterion(3, 0);

        assertEquals(1L, firingsOf(CraftPlannerV2.plan(graph, "A", 1, pathThenYield), shortLowYield));
        assertEquals(1L, firingsOf(CraftPlannerV2.plan(graph, "A", 1, yieldThenPath), deepHighYield));

        var waitingPlan = CraftPlannerV2.plan(
                graph, "A", 1, CraftingRoutePolicy.DEFAULT.withFast(true));
        assertEquals(1L, firingsOf(waitingPlan, deepHighYield));
    }
}
