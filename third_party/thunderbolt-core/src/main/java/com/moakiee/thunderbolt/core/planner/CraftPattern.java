package com.moakiee.thunderbolt.core.planner;

import java.util.List;
import java.util.Objects;

/**
 * A single crafting pattern (recipe) in the planner's view: it produces {@code outputAmount} of
 * {@code output} per firing, consuming the given {@code inputs}.
 *
 * <p>The primary {@code output} is modeled directly; every other item produced per firing is a
 * {@link CraftOutput byproduct}. The v1 closed-form planner ignores byproducts (and its caller
 * declines such patterns), while the v2 planner ({@code CraftPlannerV2}) routes byproducts into a
 * shared pool for opportunistic reuse. {@code source} is an opaque handle back to the original recipe
 * object (e.g. AE2 {@code IPatternDetails}); the planner never inspects it and uses object identity
 * for {@link CraftPlan#firings()} keys.
 *
 * @param <K> item key type
 */
public final class CraftPattern<K> {

    private final K output;
    private final long outputAmount;
    private final List<CraftInput<K>> inputs;
    private final List<CraftOutput<K>> byproducts;
    private final Object source;
    private final int idleProviderCount;
    private final int providerCount;

    public CraftPattern(K output, long outputAmount, List<CraftInput<K>> inputs, Object source) {
        this(output, outputAmount, inputs, List.of(), source);
    }

    public CraftPattern(K output, long outputAmount, List<CraftInput<K>> inputs,
                        List<CraftOutput<K>> byproducts, Object source) {
        this(output, outputAmount, inputs, byproducts, source, -1, -1);
    }

    public CraftPattern(K output, long outputAmount, List<CraftInput<K>> inputs,
                        List<CraftOutput<K>> byproducts, Object source,
                        int idleProviderCount, int providerCount) {
        this.output = Objects.requireNonNull(output, "output");
        if (outputAmount <= 0) {
            throw new IllegalArgumentException("outputAmount must be > 0, was " + outputAmount);
        }
        this.outputAmount = outputAmount;
        this.inputs = List.copyOf(inputs);
        this.byproducts = List.copyOf(byproducts);
        this.source = source;
        this.providerCount = Math.max(-1, providerCount);
        this.idleProviderCount = this.providerCount < 0
                ? -1
                : Math.max(0, Math.min(idleProviderCount, this.providerCount));
    }

    public K output() {
        return output;
    }

    public long outputAmount() {
        return outputAmount;
    }

    public List<CraftInput<K>> inputs() {
        return inputs;
    }

    /** Extra outputs produced per firing besides the primary {@link #output()}. Empty if none. */
    public List<CraftOutput<K>> byproducts() {
        return byproducts;
    }

    /** Opaque handle to the originating recipe; may be {@code null} in tests. */
    public Object source() {
        return source;
    }

    /** Number of providers that reported idle when this order was planned, or -1 if unknown. */
    public int idleProviderCount() {
        return idleProviderCount;
    }

    /** Total providers able to execute this pattern, or -1 if unknown. */
    public int providerCount() {
        return providerCount;
    }

    @Override
    public String toString() {
        return "CraftPattern[" + outputAmount + "x" + output + " <- " + inputs + "]";
    }
}
