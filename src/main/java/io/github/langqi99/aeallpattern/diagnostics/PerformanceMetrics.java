package io.github.langqi99.aeallpattern.diagnostics;

import java.util.concurrent.atomic.LongAdder;

public final class PerformanceMetrics {
    private static final LongAdder CATALOG_REBUILDS = new LongAdder();
    private static final LongAdder CATALOG_REBUILD_NANOS = new LongAdder();
    private static final LongAdder RECIPES_ACCEPTED = new LongAdder();
    private static final LongAdder RECIPES_FILTERED = new LongAdder();
    private static final LongAdder PROVIDER_REFRESHES = new LongAdder();
    private static final LongAdder PROVIDER_DIFF_SIZE = new LongAdder();
    private static final LongAdder PUSH_ACCEPTED = new LongAdder();
    private static final LongAdder PUSH_REJECTED = new LongAdder();
    private static final LongAdder MACHINE_INPUT_INSERTED = new LongAdder();
    private static final LongAdder MACHINE_OUTPUT_RECOVERED = new LongAdder();

    private PerformanceMetrics() {
    }

    public static void catalogRebuilt(long nanos, int accepted, int filtered) {
        CATALOG_REBUILDS.increment();
        CATALOG_REBUILD_NANOS.add(nanos);
        RECIPES_ACCEPTED.add(accepted);
        RECIPES_FILTERED.add(filtered);
    }

    public static void providerRefreshed(int diffSize) {
        PROVIDER_REFRESHES.increment();
        PROVIDER_DIFF_SIZE.add(diffSize);
    }

    public static void pushAccepted() {
        PUSH_ACCEPTED.increment();
    }

    public static void pushRejected() {
        PUSH_REJECTED.increment();
    }

    public static void machineInputInserted(int amount) {
        MACHINE_INPUT_INSERTED.add(amount);
    }

    public static void machineOutputRecovered(int amount) {
        MACHINE_OUTPUT_RECOVERED.add(amount);
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                CATALOG_REBUILDS.sum(),
                CATALOG_REBUILD_NANOS.sum(),
                RECIPES_ACCEPTED.sum(),
                RECIPES_FILTERED.sum(),
                PROVIDER_REFRESHES.sum(),
                PROVIDER_DIFF_SIZE.sum(),
                PUSH_ACCEPTED.sum(),
                PUSH_REJECTED.sum(),
                MACHINE_INPUT_INSERTED.sum(),
                MACHINE_OUTPUT_RECOVERED.sum());
    }

    public record Snapshot(
            long catalogRebuilds,
            long catalogRebuildNanos,
            long recipesAccepted,
            long recipesFiltered,
            long providerRefreshes,
            long providerDiffSize,
            long pushAccepted,
            long pushRejected,
            long machineInputInserted,
            long machineOutputRecovered) {
    }
}
