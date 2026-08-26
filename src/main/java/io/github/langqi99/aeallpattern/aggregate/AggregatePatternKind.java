package io.github.langqi99.aeallpattern.aggregate;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** The native AE2 pattern format used by one aggregate child recipe. */
public enum AggregatePatternKind {
    PROCESSING("processing"),
    CRAFTING("crafting"),
    STONECUTTING("stonecutting"),
    SMITHING("smithing");

    public static final Codec<AggregatePatternKind> CODEC = Codec.STRING.xmap(
            AggregatePatternKind::fromName,
            AggregatePatternKind::serializedName);

    private final String serializedName;

    AggregatePatternKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static AggregatePatternKind fromName(String name) {
        if (name != null) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (AggregatePatternKind kind : values()) {
                if (kind.serializedName.equals(normalized)) {
                    return kind;
                }
            }
        }
        return PROCESSING;
    }
}
