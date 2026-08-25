package io.github.langqi99.aeallpattern.ae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.BindingPatternKey;
import io.github.langqi99.aeallpattern.binding.BindingRecord;
import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import io.github.langqi99.aeallpattern.linker.IncomingBuffer;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlockEntity;
import io.github.langqi99.aeallpattern.machine.MachineAdapterRegistry;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import io.github.langqi99.aeallpattern.recipe.RecipeSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.binding.BlockEntityFingerprint;
import io.github.langqi99.aeallpattern.diagnostics.PerformanceMetrics;

public final class VirtualCraftingProvider implements ICraftingProvider {
    private final PatternLinkerBlockEntity linker;
    private final IncomingBuffer buffer;
    private volatile List<IPatternDetails> patterns = List.of();
    private Map<VirtualPatternDetails, PatternRoute> routes = Map.of();
    private long catalogGeneration;
    private boolean lastAvailable;

    public VirtualCraftingProvider(PatternLinkerBlockEntity linker, IncomingBuffer buffer) {
        this.linker = linker;
        this.buffer = buffer;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return linker.getMainNode().isActive() ? patterns : List.of();
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputHolders) {
        if (!(details instanceof VirtualPatternDetails virtual) || !linker.getMainNode().isActive()) {
            PerformanceMetrics.pushRejected();
            return false;
        }
        PatternRoute route = routes.get(virtual);
        if (route == null || !buffer.canAccept(route.binding.bindingId())) {
            PerformanceMetrics.pushRejected();
            return false;
        }
        ItemStack expected = route.recipe.input();
        AEItemKey expectedKey = AEItemKey.of(expected);
        long available = 0;
        for (KeyCounter counter : inputHolders) {
            for (var entry : counter) {
                if (!entry.getKey().equals(expectedKey) && entry.getLongValue() > 0) {
                    PerformanceMetrics.pushRejected();
                    return false;
                }
                if (entry.getKey().equals(expectedKey)) {
                    available += entry.getLongValue();
                }
            }
        }
        if (available != expected.getCount()) {
            PerformanceMetrics.pushRejected();
            return false;
        }

        if (!routeStillValid(route)) {
            refresh();
            PerformanceMetrics.pushRejected();
            return false;
        }
        buffer.enqueue(
                route.binding,
                route.recipe.fingerprint().stableKey(),
                expected,
                route.recipe.output(),
                route.recipe.processingTicks());
        linker.saveChanges();
        PerformanceMetrics.pushAccepted();
        return true;
    }

    @Override
    public boolean isBusy() {
        if (routes.isEmpty()) {
            return true;
        }
        return routes.values().stream().allMatch(route -> buffer.hasWork(route.binding.bindingId()));
    }

    public long catalogGeneration() {
        return catalogGeneration;
    }

    public void refresh() {
        if (!(linker.getLevel() instanceof ServerLevel linkerLevel)) {
            return;
        }
        Map<VirtualPatternDetails, PatternRoute> rebuilt = new LinkedHashMap<>();
        var anchor = linker.getGlobalPos();
        List<BindingRecord> bindings = BindingSavedData.get(linkerLevel.getServer()).all().stream()
                .filter(binding -> binding.anchor().equals(anchor))
                .sorted(java.util.Comparator.comparing(BindingRecord::bindingId))
                .toList();
        for (BindingRecord binding : bindings) {
            ServerLevel targetLevel = linkerLevel.getServer().getLevel(binding.target().dimension());
            if (targetLevel == null || !targetLevel.hasChunkAt(binding.target().pos())) {
                continue;
            }
            BlockEntity target = targetLevel.getBlockEntity(binding.target().pos());
            if (target == null || !binding.targetFingerprint().equals(BlockEntityFingerprint.of(target))) {
                continue;
            }
            ResourceLocation adapterId = ResourceLocation.tryParse(binding.adapterId());
            if (adapterId == null) {
                continue;
            }
            var adapter = MachineAdapterRegistry.byId(adapterId)
                    .filter(candidate -> candidate.schemaVersion() == binding.adapterSchema())
                    .filter(candidate -> candidate.supports(targetLevel, target));
            if (adapter.isEmpty()) {
                continue;
            }
            var catalog = RecipeIndexService.catalog(targetLevel, target, adapter.get());
            for (RecipeSnapshot recipe : catalog.recipes()) {
                BindingPatternKey patternKey = new BindingPatternKey(binding.bindingId(), recipe.fingerprint());
                ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                        List.of(GenericStack.fromItemStack(recipe.input())),
                        List.of(GenericStack.fromItemStack(recipe.output())));
                encoded.set(ModDataComponents.VIRTUAL_PATTERN_ID.get(),
                        binding.bindingId() + ":" + recipe.fingerprint().stableKey());
                IPatternDetails decoded = PatternDetailsHelper.decodePattern(encoded, linkerLevel);
                if (decoded == null) {
                    AeAllPattern.LOGGER.warn("AE2 rejected generated processing pattern for {}", recipe.recipeId());
                    continue;
                }
                VirtualPatternDetails details = new VirtualPatternDetails(
                        patternKey, decoded);
                rebuilt.put(details, new PatternRoute(binding, recipe));
            }
        }

        Set<BindingPatternKey> oldKeys = routes.keySet().stream().map(VirtualPatternDetails::key).collect(java.util.stream.Collectors.toSet());
        Set<BindingPatternKey> newKeys = rebuilt.keySet().stream().map(VirtualPatternDetails::key).collect(java.util.stream.Collectors.toSet());
        routes = Map.copyOf(rebuilt);
        patterns = List.copyOf(new ArrayList<>(rebuilt.keySet()));
        catalogGeneration = RecipeIndexService.generation();
        int diffSize = com.google.common.collect.Sets.symmetricDifference(oldKeys, newKeys).size();
        PerformanceMetrics.providerRefreshed(diffSize);
        boolean available = linker.getMainNode().isActive();
        if ((!oldKeys.equals(newKeys) || available != lastAvailable) && linker.getMainNode().isReady()) {
            ICraftingProvider.requestUpdate(linker.getMainNode());
        }
        lastAvailable = available;
    }

    public void tickAvailability() {
        boolean available = linker.getMainNode().isActive();
        if (available != lastAvailable && linker.getMainNode().isReady()) {
            lastAvailable = available;
            ICraftingProvider.requestUpdate(linker.getMainNode());
        }
    }

    private boolean routeStillValid(PatternRoute route) {
        if (!(linker.getLevel() instanceof ServerLevel linkerLevel)) {
            return false;
        }
        if (BindingSavedData.get(linkerLevel.getServer()).find(route.binding.bindingId())
                .filter(route.binding::equals)
                .isEmpty()) {
            return false;
        }
        ServerLevel targetLevel = linkerLevel.getServer().getLevel(route.binding.target().dimension());
        if (targetLevel == null || !targetLevel.hasChunkAt(route.binding.target().pos())) {
            return false;
        }
        BlockEntity target = targetLevel.getBlockEntity(route.binding.target().pos());
        return target != null && route.binding.targetFingerprint().equals(BlockEntityFingerprint.of(target));
    }

    private record PatternRoute(BindingRecord binding, RecipeSnapshot recipe) {
    }
}
