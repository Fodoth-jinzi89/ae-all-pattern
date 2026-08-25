package io.github.langqi99.aeallpattern.binding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

/** Short-lived, item-bound first step of the two-step binding protocol. */
public record AnchorSelection(
        int schemaVersion,
        UUID ownerId,
        GlobalPos anchor,
        String anchorFingerprint,
        long selectedAtGameTime) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<AnchorSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(AnchorSelection::schemaVersion),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(AnchorSelection::ownerId),
            GlobalPos.CODEC.fieldOf("anchor").forGetter(AnchorSelection::anchor),
            Codec.STRING.fieldOf("anchor_fingerprint").forGetter(AnchorSelection::anchorFingerprint),
            Codec.LONG.fieldOf("selected_at_game_time").forGetter(AnchorSelection::selectedAtGameTime)
    ).apply(instance, AnchorSelection::new));

    public AnchorSelection {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(anchor, "anchor");
        anchorFingerprint = requireText(anchorFingerprint, "anchorFingerprint");
    }

    public static AnchorSelection create(
            UUID ownerId, GlobalPos anchor, String anchorFingerprint, long selectedAtGameTime) {
        return new AnchorSelection(
                CURRENT_SCHEMA_VERSION, ownerId, anchor, anchorFingerprint, selectedAtGameTime);
    }

    public boolean hasSupportedSchema() {
        return schemaVersion == CURRENT_SCHEMA_VERSION;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
