package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.AeAllPattern;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record BindingSyncPayload(List<BindingRenderEntry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 4096;
    public static final Type<BindingSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeAllPattern.MOD_ID, "binding_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BindingSyncPayload> STREAM_CODEC = StreamCodec.of(
            BindingSyncPayload::encode,
            BindingSyncPayload::decode);

    public BindingSyncPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("too many binding render entries");
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, BindingSyncPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (BindingRenderEntry entry : payload.entries) {
            buffer.writeUUID(entry.bindingId());
            buffer.writeResourceLocation(entry.dimension().location());
            buffer.writeBlockPos(entry.pos());
            buffer.writeByte(entry.status());
        }
    }

    private static BindingSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("invalid binding render entry count: " + count);
        }
        List<BindingRenderEntry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new BindingRenderEntry(
                    buffer.readUUID(),
                    ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()),
                    buffer.readBlockPos(),
                    buffer.readByte()));
        }
        return new BindingSyncPayload(entries);
    }
}
