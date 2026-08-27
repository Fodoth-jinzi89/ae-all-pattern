package io.github.langqi99.aeallpattern.network;

import io.github.langqi99.aeallpattern.aggregate.AggregatePatternLibrary;
import io.github.langqi99.aeallpattern.aggregate.AggregateRecipe;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Validates and assembles bounded JEI scan pages before updating the server library. */
public final class AggregateGenerationService {
    private static final long UPLOAD_TIMEOUT_TICKS = 20L * 30L;
    private static final Map<UploadKey, Upload> UPLOADS = new HashMap<>();

    private AggregateGenerationService() {
    }

    public static void handle(GenerateAggregatePayload payload, ServerPlayer player) {
        long now = player.level().getGameTime();
        UPLOADS.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateTick > UPLOAD_TIMEOUT_TICKS);
        if (!validTarget(payload, player)) {
            return;
        }

        UploadKey key = new UploadKey(player.getUUID(), payload.uploadId());
        Upload upload = UPLOADS.computeIfAbsent(key, ignored -> new Upload(payload, now));
        if (!upload.matches(payload) || !upload.add(payload, now)) {
            UPLOADS.remove(key);
            return;
        }
        if (!upload.complete()) {
            return;
        }
        UPLOADS.remove(key);
        List<AggregateRecipe> recipes = upload.flatten();
        if (recipes.size() != payload.totalRecipeCount()) {
            return;
        }
        var library = AggregatePatternLibrary.get(player.getServer());
        var ref = library.put(
                player.getServer(), payload.catalystId(), payload.machineTranslationKey(), recipes);
        AggregateMetadataSyncService.sendToOnlinePlayers(player.getServer());

        ItemStack aggregate = new ItemStack(ModItems.AGGREGATE_PATTERN.get());
        aggregate.set(ModDataComponents.AGGREGATE_PATTERN.get(), ref);
        if (!player.addItem(aggregate)) {
            player.drop(aggregate, false);
        }
        player.level().playSound(
                null, payload.machinePos(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.6F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "message.aeallpattern.generator.created",
                Component.translatable(payload.machineTranslationKey()), recipes.size()), true);
    }

    private static boolean validTarget(GenerateAggregatePayload payload, ServerPlayer player) {
        if (!holdsGenerator(player)
                || player.distanceToSqr(payload.machinePos().getCenter()) > 64.0
                || !player.level().hasChunkAt(payload.machinePos())) {
            return false;
        }
        var block = player.level().getBlockState(payload.machinePos()).getBlock();
        return BuiltInRegistries.BLOCK.getKey(block).equals(payload.catalystId())
                && block.getDescriptionId().equals(payload.machineTranslationKey());
    }

    private static boolean holdsGenerator(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.ALL_PATTERN_GENERATOR.get())
                || player.getOffhandItem().is(ModItems.ALL_PATTERN_GENERATOR.get());
    }

    private record UploadKey(UUID playerId, UUID uploadId) {
    }

    private static final class Upload {
        private final net.minecraft.core.BlockPos machinePos;
        private final net.minecraft.resources.ResourceLocation catalystId;
        private final String machineKey;
        private final int pageCount;
        private final int totalRecipeCount;
        private final List<List<AggregateRecipe>> pages;
        private long lastUpdateTick;

        private Upload(GenerateAggregatePayload first, long now) {
            machinePos = first.machinePos();
            catalystId = first.catalystId();
            machineKey = first.machineTranslationKey();
            pageCount = first.pageCount();
            totalRecipeCount = first.totalRecipeCount();
            pages = new ArrayList<>(java.util.Collections.nCopies(pageCount, null));
            lastUpdateTick = now;
        }

        private boolean matches(GenerateAggregatePayload page) {
            return machinePos.equals(page.machinePos())
                    && catalystId.equals(page.catalystId())
                    && machineKey.equals(page.machineTranslationKey())
                    && pageCount == page.pageCount()
                    && totalRecipeCount == page.totalRecipeCount();
        }

        private boolean add(GenerateAggregatePayload page, long now) {
            List<AggregateRecipe> previous = pages.get(page.pageIndex());
            if (previous != null && !previous.equals(page.recipes())) {
                return false;
            }
            pages.set(page.pageIndex(), page.recipes());
            lastUpdateTick = now;
            return true;
        }

        private boolean complete() {
            return pages.stream().allMatch(java.util.Objects::nonNull);
        }

        private List<AggregateRecipe> flatten() {
            return pages.stream().flatMap(List::stream).toList();
        }
    }
}
