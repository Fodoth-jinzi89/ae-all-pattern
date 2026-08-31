package io.github.langqi99.aeallpattern.network;

import appeng.api.stacks.GenericStack;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternSelectionMenu;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternSelectionMenu.Entry;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One bounded page of a search result. A full result is several pages; the client assembles
 * them by request id and page index. Each page stays well below the protocol packet limit.
 */
public record AggregateSearchResultPayload(
        UUID requestId, int pageIndex, int pageCount, List<Entry> entries)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES_PER_PAGE = 64;
    private static final int MAX_STACKS_PER_LIST = 81;

    public static final Type<AggregateSearchResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeAllPattern.MOD_ID, "aggregate_search_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregateSearchResultPayload> STREAM_CODEC =
            StreamCodec.of(AggregateSearchResultPayload::encode, AggregateSearchResultPayload::decode);

    public AggregateSearchResultPayload {
        entries = List.copyOf(entries);
        if (pageIndex < 0 || pageCount < 1 || pageIndex >= pageCount
                || entries.size() > MAX_ENTRIES_PER_PAGE) {
            throw new IllegalArgumentException("invalid aggregate search result page");
        }
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AggregateSearchResultPayload payload) {
        buffer.writeUUID(payload.requestId());
        buffer.writeVarInt(payload.pageIndex());
        buffer.writeVarInt(payload.pageCount());
        buffer.writeVarInt(payload.entries().size());
        for (Entry entry : payload.entries()) {
            buffer.writeUtf(entry.patternId(), AggregatePatternSelection.MAX_ID_LENGTH);
            writeStacks(buffer, entry.inputs());
            writeStacks(buffer, entry.outputs());
        }
    }

    private static AggregateSearchResultPayload decode(RegistryFriendlyByteBuf buffer) {
        UUID requestId = buffer.readUUID();
        int pageIndex = buffer.readVarInt();
        int pageCount = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES_PER_PAGE) {
            throw new IllegalArgumentException("invalid search result entry count: " + count);
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String patternId = buffer.readUtf(AggregatePatternSelection.MAX_ID_LENGTH);
            entries.add(new Entry(patternId, readStacks(buffer), readStacks(buffer)));
        }
        return new AggregateSearchResultPayload(requestId, pageIndex, pageCount, entries);
    }

    private static void writeStacks(RegistryFriendlyByteBuf buffer, List<GenericStack> stacks) {
        buffer.writeVarInt(stacks.size());
        stacks.forEach(stack -> GenericStack.STREAM_CODEC.encode(buffer, stack));
    }

    private static List<GenericStack> readStacks(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_STACKS_PER_LIST) {
            throw new IllegalArgumentException("invalid search result stack count: " + count);
        }
        List<GenericStack> stacks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            stacks.add(GenericStack.STREAM_CODEC.decode(buffer));
        }
        return stacks;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
