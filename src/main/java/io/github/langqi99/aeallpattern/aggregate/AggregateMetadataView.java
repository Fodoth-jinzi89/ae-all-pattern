package io.github.langqi99.aeallpattern.aggregate;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/** Client-facing metadata cache. It deliberately contains no recipe payloads. */
public final class AggregateMetadataView {
    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    private AggregateMetadataView() {
    }

    public static void replace(Collection<Entry> entries) {
        ENTRIES.clear();
        entries.forEach(entry -> ENTRIES.put(entry.libraryId(), entry));
    }

    public static Optional<Entry> find(UUID libraryId) {
        return Optional.ofNullable(ENTRIES.get(libraryId));
    }

    public record Entry(
            UUID libraryId,
            ResourceLocation catalystId,
            String machineTranslationKey,
            String contentHash,
            int recipeCount) {
    }
}
