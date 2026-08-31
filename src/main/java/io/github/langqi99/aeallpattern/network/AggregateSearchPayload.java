package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.AeAllPattern;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request to re-filter the picker against the server's complete recipe list. */
public record AggregateSearchPayload(
        UUID requestId, String searchText, boolean searchOutputs) implements CustomPacketPayload {
    public static final Type<AggregateSearchPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeAllPattern.MOD_ID, "aggregate_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateSearchPayload> STREAM_CODEC =
            StreamCodec.of(AggregateSearchPayload::encode, AggregateSearchPayload::decode);

    public AggregateSearchPayload {
        searchText = searchText == null ? "" : searchText;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AggregateSearchPayload payload) {
        buffer.writeUUID(payload.requestId());
        buffer.writeUtf(payload.searchText(), 64);
        buffer.writeBoolean(payload.searchOutputs());
    }

    private static AggregateSearchPayload decode(RegistryFriendlyByteBuf buffer) {
        return new AggregateSearchPayload(
                buffer.readUUID(), buffer.readUtf(64), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
