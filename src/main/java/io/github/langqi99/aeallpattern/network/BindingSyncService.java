package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import java.util.List;
import java.util.Objects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BindingSyncService {
    private BindingSyncService() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
        }
    }

    public static void send(ServerPlayer player) {
        List<BindingRenderEntry> entries = BindingSavedData.get(Objects.requireNonNull(player.getServer())).all().stream()
                .filter(binding -> binding.ownerId().equals(player.getUUID()))
                .map(binding -> new BindingRenderEntry(
                        binding.bindingId(), binding.target().dimension(), binding.target().pos(), (byte) 1))
                .toList();
        PacketDistributor.sendToPlayer(player, new BindingSyncPayload(entries));
    }

    public static void sendToOnlinePlayers(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(BindingSyncService::send);
    }
}
