package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.client.ClientBindingState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BindingNetwork {
    private BindingNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                BindingSyncPayload.TYPE,
                BindingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBindingState.replace(payload.entries())));
        registrar.playToClient(
                AggregateMetadataPayload.TYPE,
                AggregateMetadataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        io.github.langqi99.aeallpattern.aggregate.AggregateMetadataView.replace(payload.entries())));
        registrar.playToServer(
                GenerateAggregatePayload.TYPE,
                GenerateAggregatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        AggregateGenerationService.handle(payload, player);
                    }
                }));
    }
}
