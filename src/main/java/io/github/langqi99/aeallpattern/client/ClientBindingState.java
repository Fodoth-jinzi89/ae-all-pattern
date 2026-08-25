package io.github.langqi99.aeallpattern.client;

import io.github.langqi99.aeallpattern.network.BindingRenderEntry;
import java.util.List;

public final class ClientBindingState {
    private static volatile List<BindingRenderEntry> bindings = List.of();

    private ClientBindingState() {
    }

    public static void replace(List<BindingRenderEntry> updated) {
        bindings = List.copyOf(updated);
    }

    public static List<BindingRenderEntry> bindings() {
        return bindings;
    }

    public static void clear() {
        bindings = List.of();
    }
}
