package io.github.langqi99.aeallpattern.client;

import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Resolves recipe-viewer workstations that differ from the clicked block. */
public final class ClientRecipeMachineResolver {
    private static final String MEKANISM_BOUNDING_TILE = "mekanism.common.tile.TileEntityBoundingBlock";
    private static final Map<ResourceLocation, ResourceLocation> CATALYST_ALIASES = Map.ofEntries(
            alias("packagedexcrafting", "basic_crafter", "extendedcrafting", "basic_table"),
            alias("packagedexcrafting", "advanced_crafter", "extendedcrafting", "advanced_table"),
            alias("packagedexcrafting", "elite_crafter", "extendedcrafting", "elite_table"),
            alias("packagedexcrafting", "ultimate_crafter", "extendedcrafting", "ultimate_table"),
            alias("packagedexcrafting", "ender_crafter", "extendedcrafting", "ender_crafter"),
            alias("packagedexcrafting", "flux_crafter", "extendedcrafting", "flux_crafter"),
            alias("packagedexcrafting", "combination_crafter", "extendedcrafting", "crafting_core"),
            alias("packagedexcrafting", "marked_pedestal", "extendedcrafting", "pedestal"),
            alias("packagedavaritia", "sculk_crafter", "avaritia", "sculk_crafting_table"),
            alias("packagedavaritia", "nether_crafter", "avaritia", "nether_crafting_table"),
            alias("packagedavaritia", "end_crafter", "avaritia", "end_crafting_table"),
            alias("packagedavaritia", "extreme_crafter", "avaritia", "extreme_crafting_table"));

    private ClientRecipeMachineResolver() {
    }

    public static BlockPos resolvePosition(Level level, BlockPos clickedPos) {
        Object blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity == null || !blockEntity.getClass().getName().equals(MEKANISM_BOUNDING_TILE)) {
            return clickedPos;
        }
        try {
            Method getMainPos = blockEntity.getClass().getMethod("getMainPos");
            Object mainPos = getMainPos.invoke(blockEntity);
            return mainPos instanceof BlockPos pos && level.hasChunkAt(pos) ? pos.immutable() : clickedPos;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return clickedPos;
        }
    }

    public static ItemStack recipeViewerCatalyst(Level level, BlockPos machinePos) {
        var block = level.getBlockState(machinePos).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation catalystId = catalystAlias(blockId);
        ItemStack catalyst = BuiltInRegistries.ITEM.get(catalystId).getDefaultInstance();
        return catalyst.isEmpty() ? block.asItem().getDefaultInstance() : catalyst;
    }

    static ResourceLocation catalystAlias(ResourceLocation blockId) {
        return CATALYST_ALIASES.getOrDefault(blockId, blockId);
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static Map.Entry<ResourceLocation, ResourceLocation> alias(
            String sourceNamespace, String sourcePath, String targetNamespace, String targetPath) {
        return Map.entry(id(sourceNamespace, sourcePath), id(targetNamespace, targetPath));
    }
}
