package io.github.langqi99.aeallpattern.diagnostics;

import appeng.api.config.Actionable;
import com.mojang.brigadier.CommandDispatcher;
import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlockEntity;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import java.util.List;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("aeallpattern")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("seed-test-materials")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("linker", BlockPosArgument.blockPos())
                                .executes(context -> seedTestMaterials(
                                        context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "linker")))))
                .then(Commands.literal("perf")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> perf(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        var records = BindingSavedData.get(source.getServer()).all();
        long visible = source.getPlayer() == null || source.hasPermission(2)
                ? records.size()
                : records.stream().filter(record -> record.ownerId().equals(source.getPlayer().getUUID())).count();
        source.sendSuccess(() -> Component.literal(
                "AE All Pattern: bindings=" + visible + ", recipeGeneration=" + RecipeIndexService.generation()), false);
        return (int) Math.min(Integer.MAX_VALUE, visible);
    }

    private static int perf(CommandSourceStack source) {
        PerformanceMetrics.Snapshot metrics = PerformanceMetrics.snapshot();
        double rebuildMillis = metrics.catalogRebuildNanos() / 1_000_000.0;
        source.sendSuccess(() -> Component.literal(String.format(
                java.util.Locale.ROOT,
                "catalog rebuilds=%d time=%.2fms accepted=%d filtered=%d | provider refreshes=%d diff=%d | pushes accepted=%d rejected=%d | machine input=%d recovered=%d",
                metrics.catalogRebuilds(), rebuildMillis, metrics.recipesAccepted(), metrics.recipesFiltered(),
                metrics.providerRefreshes(), metrics.providerDiffSize(),
                metrics.pushAccepted(), metrics.pushRejected(),
                metrics.machineInputInserted(), metrics.machineOutputRecovered())), false);
        return 1;
    }

    private static int seedTestMaterials(CommandSourceStack source, net.minecraft.core.BlockPos linkerPos) {
        if (!(source.getLevel().getBlockEntity(linkerPos) instanceof PatternLinkerBlockEntity linker)) {
            source.sendFailure(Component.literal("No All Pattern Linker at " + linkerPos.toShortString()));
            return 0;
        }
        if (!linker.getMainNode().isOnline()) {
            source.sendFailure(Component.literal("All Pattern Linker is not online"));
            return 0;
        }

        int inserted = 0;
        for (SeedStack seed : TEST_MATERIALS) {
            var item = BuiltInRegistries.ITEM.getOptional(seed.id());
            if (item.isPresent()) {
                inserted += linker.insertIntoNetwork(
                        new ItemStack(item.orElseThrow(), seed.count()), Actionable.MODULATE);
            }
        }
        var grid = linker.getMainNode().getGrid();
        if (grid != null) {
            grid.getStorageService().invalidateCache();
        }
        int result = inserted;
        source.sendSuccess(() -> Component.literal("Seeded " + result + " test items into the ME network"), false);
        return inserted;
    }

    private static final List<SeedStack> TEST_MATERIALS = List.of(
            seed("minecraft:raw_iron", 16),
            seed("minecraft:raw_gold", 16),
            seed("minecraft:raw_copper", 16),
            seed("minecraft:cobblestone", 16),
            seed("minecraft:beef", 16),
            seed("minecraft:potato", 16),
            seed("minecraft:redstone", 16),
            seed("minecraft:quartz", 16),
            seed("minecraft:diamond", 8),
            seed("minecraft:oak_log", 16),
            seed("minecraft:stone_bricks", 16),
            seed("mekanism:raw_osmium", 16),
            seed("mekanism:raw_tin", 16),
            seed("mekanism:raw_lead", 16),
            seed("mysticalagriculture:prosperity_seed_base", 16),
            seed("mysticalagriculture:inferium_essence", 64),
            seed("mysticalagriculture:prudentium_essence", 32),
            seed("mysticalagriculture:tertium_essence", 32));

    private static SeedStack seed(String id, int count) {
        return new SeedStack(ResourceLocation.parse(id), count);
    }

    private record SeedStack(ResourceLocation id, int count) {
    }
}
