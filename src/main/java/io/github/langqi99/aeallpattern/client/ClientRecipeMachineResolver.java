package io.github.langqi99.aeallpattern.client;

import io.github.langqi99.aeallpattern.machine.MachineTargetResolver;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Resolves recipe-viewer workstations that differ from the clicked block. */
public final class ClientRecipeMachineResolver {
    private static final String[] MEKANISM_EXTRAS_TIERS = {"absolute_", "supreme_", "cosmic_", "infinite_"};
    private static final Map<String, ResourceLocation> MEKANISM_EXTRAS_FACTORY_ALIASES = Map.ofEntries(
            factoryAlias("combining_factory", "mekanism", "basic_combining_factory"),
            factoryAlias("compressing_factory", "mekanism", "basic_compressing_factory"),
            factoryAlias("crushing_factory", "mekanism", "basic_crushing_factory"),
            factoryAlias("enriching_factory", "mekanism", "basic_enriching_factory"),
            factoryAlias("infusing_factory", "mekanism", "basic_infusing_factory"),
            factoryAlias("injecting_factory", "mekanism", "basic_injecting_factory"),
            factoryAlias("purifying_factory", "mekanism", "basic_purifying_factory"),
            factoryAlias("sawing_factory", "mekanism", "basic_sawing_factory"),
            factoryAlias("smelting_factory", "mekanism", "basic_smelting_factory"),
            factoryAlias("centrifuging_factory", "mekmm", "basic_centrifuging_factory"),
            factoryAlias("crystallizing_factory", "mekmm", "basic_crystallizing_factory"),
            factoryAlias("dissolving_factory", "mekmm", "basic_dissolving_factory"),
            factoryAlias("lathing_factory", "mekmm", "basic_lathing_factory"),
            factoryAlias("liquifying_factory", "mekmm", "basic_liquifying_factory"),
            factoryAlias("painting_factory", "mekmm", "basic_painting_factory"),
            factoryAlias("pigment_extracting_factory", "mekmm", "basic_pigment_extracting_factory"),
            factoryAlias("planting_factory", "mekmm", "basic_planting_factory"),
            factoryAlias("pressurised_reacting_factory", "mekmm", "basic_pressurised_reacting_factory"),
            factoryAlias("recycling_factory", "mekmm", "basic_recycling_factory"),
            factoryAlias("replicating_factory", "mekmm", "basic_replicating_factory"),
            factoryAlias("rolling_mill_factory", "mekmm", "basic_rolling_mill_factory"),
            factoryAlias("stamping_factory", "mekmm", "basic_stamping_factory"),
            factoryAlias("washing_factory", "mekmm", "basic_washing_factory"));
    private static final Map<ResourceLocation, ResourceLocation> CATALYST_ALIASES = Map.ofEntries(
            alias("packagedexcrafting", "basic_crafter", "extendedcrafting", "basic_table"),
            alias("packagedexcrafting", "advanced_crafter", "extendedcrafting", "advanced_table"),
            alias("packagedexcrafting", "elite_crafter", "extendedcrafting", "elite_table"),
            alias("packagedexcrafting", "ultimate_crafter", "extendedcrafting", "ultimate_table"),
            alias("packagedexcrafting", "ender_crafter", "extendedcrafting", "ender_crafter"),
            alias("packagedexcrafting", "flux_crafter", "extendedcrafting", "flux_crafter"),
            alias("packagedexcrafting", "combination_crafter", "extendedcrafting", "crafting_core"),
            alias("packagedexcrafting", "marked_pedestal", "extendedcrafting", "pedestal"),
            alias("applied_extended_crafting", "table_basic_pattern_provider", "extendedcrafting", "basic_table"),
            alias("applied_extended_crafting", "table_advanced_pattern_provider", "extendedcrafting", "advanced_table"),
            alias("applied_extended_crafting", "table_elite_pattern_provider", "extendedcrafting", "elite_table"),
            alias("applied_extended_crafting", "table_ultimate_pattern_provider", "extendedcrafting", "ultimate_table"),
            alias("applied_extended_crafting", "ender_crafter_pattern_provider", "extendedcrafting", "ender_crafter"),
            alias("applied_extended_crafting", "flux_crafter_pattern_provider", "extendedcrafting", "flux_crafter"),
            alias("applied_extended_crafting", "crafter_core_pattern_provider", "extendedcrafting", "crafting_core"),
            alias("packagedavaritia", "sculk_crafter", "avaritia", "sculk_crafting_table"),
            alias("packagedavaritia", "nether_crafter", "avaritia", "nether_crafting_table"),
            alias("packagedavaritia", "end_crafter", "avaritia", "end_crafting_table"),
            alias("packagedavaritia", "extreme_crafter", "avaritia", "extreme_crafting_table"));

    private ClientRecipeMachineResolver() {
    }

    public static BlockPos resolvePosition(Level level, BlockPos clickedPos) {
        return MachineTargetResolver.resolvePosition(level, clickedPos);
    }

    public static ItemStack recipeViewerCatalyst(Level level, BlockPos machinePos) {
        var block = level.getBlockState(machinePos).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation catalystId = catalystAlias(blockId);
        ItemStack catalyst = BuiltInRegistries.ITEM.get(catalystId).getDefaultInstance();
        return catalyst.isEmpty() ? block.asItem().getDefaultInstance() : catalyst;
    }

    static ResourceLocation catalystAlias(ResourceLocation blockId) {
        if ((blockId.getNamespace().equals("mekanism_extras") || blockId.getNamespace().equals("mekmm"))
                && (blockId.getPath().endsWith("_oxidizing_factory")
                        || blockId.getPath().endsWith("_chemical_oxidizing_factory"))) {
            return id("mekanism", "chemical_oxidizer");
        }
        ResourceLocation alias = CATALYST_ALIASES.get(blockId);
        if (alias != null || !blockId.getNamespace().equals("mekanism_extras")) {
            return alias == null ? blockId : alias;
        }
        for (String tier : MEKANISM_EXTRAS_TIERS) {
            if (blockId.getPath().startsWith(tier)) {
                return MEKANISM_EXTRAS_FACTORY_ALIASES.getOrDefault(
                        blockId.getPath().substring(tier.length()), blockId);
            }
        }
        return blockId;
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static Map.Entry<ResourceLocation, ResourceLocation> alias(
            String sourceNamespace, String sourcePath, String targetNamespace, String targetPath) {
        return Map.entry(id(sourceNamespace, sourcePath), id(targetNamespace, targetPath));
    }

    private static Map.Entry<String, ResourceLocation> factoryAlias(
            String sourcePath, String targetNamespace, String targetPath) {
        return Map.entry(sourcePath, id(targetNamespace, targetPath));
    }
}
