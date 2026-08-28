package io.github.langqi99.aeallpattern.ae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import io.github.langqi99.aeallpattern.binding.BindingPatternKey;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternOptions;
import io.github.langqi99.aeallpattern.aggregate.AggregateRecipe;
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
        List<ItemStack> inputs = suppliedInputs(virtual, inputHolders);
        if (inputs.isEmpty()) {
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
                route.recipe,
                inputs,
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
        AggregatePatternOptions options = linker.getPatternOptions();
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
                AggregateRecipe aggregateRecipe = AggregateRecipe.from(recipe);
                IPatternDetails processed = AggregatePatternExpander.expandRecipe(
                        aggregateRecipe,
                        options,
                        linkerLevel,
                        "linker:" + binding.bindingId() + ":" + recipe.fingerprint().stableKey());
                if (processed == null) {
                    continue;
                }
                VirtualPatternDetails details = new VirtualPatternDetails(
                        patternKey, processed);
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

    private List<ItemStack> suppliedInputs(VirtualPatternDetails details, KeyCounter[] holders) {
        IPatternDetails.IInput[] expected = details.getInputs();
        if (holders.length != expected.length) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>(holders.length);
        for (int index = 0; index < holders.length; index++) {
            AEItemKey key = null;
            long amount = 0;
            for (var entry : holders[index]) {
                if (entry.getLongValue() <= 0) {
                    continue;
                }
                if (!(entry.getKey() instanceof AEItemKey itemKey) || key != null) {
                    return List.of();
                }
                key = itemKey;
                amount = entry.getLongValue();
            }
            if (key == null || amount < 1 || amount > Integer.MAX_VALUE
                    || !expected[index].isValid(key, linker.getLevel())) {
                return List.of();
            }
            long required = -1;
            for (var possible : expected[index].getPossibleInputs()) {
                if (possible.what().equals(key)) {
                    required = possible.amount() * expected[index].getMultiplier();
                    break;
                }
            }
            if (amount != required) {
                return List.of();
            }
            result.add(key.toStack((int) amount));
        }
        return List.copyOf(result);
    }

    private record PatternRoute(BindingRecord binding, RecipeSnapshot recipe) {
    }
}
