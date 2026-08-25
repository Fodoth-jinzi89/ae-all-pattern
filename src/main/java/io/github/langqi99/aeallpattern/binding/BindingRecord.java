package io.github.langqi99.aeallpattern.binding;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Persisted binding identity. No AE grid object is ever stored here. */
public record BindingRecord(
        int schemaVersion,
        UUID bindingId,
        UUID ownerId,
        GlobalPos anchor,
        GlobalPos target,
        Direction clickedSide,
        String anchorFingerprint,
        String targetFingerprint,
        String adapterId,
        int adapterSchema,
        long createdAtGameTime,
        long lastValidatedGameTime) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public BindingRecord {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported binding schema: " + schemaVersion);
        }
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(clickedSide, "clickedSide");
        anchorFingerprint = requireText(anchorFingerprint, "anchorFingerprint");
        targetFingerprint = requireText(targetFingerprint, "targetFingerprint");
        adapterId = requireText(adapterId, "adapterId");
        if (adapterSchema < 1) {
            throw new IllegalArgumentException("adapterSchema must be positive");
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", schemaVersion);
        tag.putUUID("BindingId", bindingId);
        tag.putUUID("OwnerId", ownerId);
        putGlobalPos(tag, "Anchor", anchor);
        putGlobalPos(tag, "Target", target);
        tag.putString("ClickedSide", clickedSide.getName());
        tag.putString("AnchorFingerprint", anchorFingerprint);
        tag.putString("TargetFingerprint", targetFingerprint);
        tag.putString("AdapterId", adapterId);
        tag.putInt("AdapterSchema", adapterSchema);
        tag.putLong("CreatedAtGameTime", createdAtGameTime);
        tag.putLong("LastValidatedGameTime", lastValidatedGameTime);
        return tag;
    }

    public static BindingRecord fromTag(CompoundTag tag) {
        Direction side = Direction.byName(tag.getString("ClickedSide"));
        if (side == null) {
            throw new IllegalArgumentException("invalid clicked side");
        }
        return new BindingRecord(
                tag.getInt("SchemaVersion"),
                tag.getUUID("BindingId"),
                tag.getUUID("OwnerId"),
                getGlobalPos(tag, "Anchor"),
                getGlobalPos(tag, "Target"),
                side,
                tag.getString("AnchorFingerprint"),
                tag.getString("TargetFingerprint"),
                tag.getString("AdapterId"),
                tag.getInt("AdapterSchema"),
                tag.getLong("CreatedAtGameTime"),
                tag.getLong("LastValidatedGameTime"));
    }

    private static void putGlobalPos(CompoundTag parent, String key, GlobalPos value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", value.dimension().location().toString());
        tag.putLong("Pos", value.pos().asLong());
        parent.put(key, tag);
    }

    private static GlobalPos getGlobalPos(CompoundTag parent, String key) {
        CompoundTag tag = parent.getCompound(key);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimensionId == null) {
            throw new IllegalArgumentException("invalid dimension in " + key);
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return GlobalPos.of(dimension, BlockPos.of(tag.getLong("Pos")));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
