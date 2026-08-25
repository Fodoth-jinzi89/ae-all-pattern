package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.client.ClientBindingState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BindingNetwork {
    private BindingNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                BindingSyncPayload.TYPE,
                BindingSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBindingState.replace(payload.entries())));
    }
}
