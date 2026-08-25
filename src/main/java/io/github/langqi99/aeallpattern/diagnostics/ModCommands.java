package io.github.langqi99.aeallpattern.diagnostics;

import com.mojang.brigadier.CommandDispatcher;
import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("aeallpattern")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
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
}
