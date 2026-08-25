package io.github.langqi99.aeallpattern.gametest;

import appeng.api.networking.GridFlags;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.BindingRecord;
import io.github.langqi99.aeallpattern.linker.IncomingBuffer;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlockEntity;
import io.github.langqi99.aeallpattern.machine.MachineAdapterRegistry;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import io.github.langqi99.aeallpattern.registry.ModBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AeAllPattern.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void linkerCreatesChannelNode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.PATTERN_LINKER.get());
        helper.runAfterDelay(2, () -> {
            PatternLinkerBlockEntity linker = helper.getBlockEntity(pos);
            helper.assertTrue(linker != null, "linker block entity was not created");
            helper.assertTrue(linker.getMainNode().getNode() != null, "managed grid node was not created");
            helper.assertTrue(linker.getMainNode().getNode().hasFlag(GridFlags.REQUIRE_CHANNEL),
                    "linker must consume a channel");
            helper.assertValueEqual(linker.getMainNode().getNode().getIdlePowerUsage(), 2.0,
                    "unexpected idle power usage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void furnaceCatalogIsDiscoverable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, Blocks.FURNACE);
        BlockEntity furnace = helper.getBlockEntity(pos);
        var adapter = MachineAdapterRegistry.find(helper.getLevel(), furnace);
        helper.assertTrue(adapter.isPresent(), "vanilla furnace adapter was not found");
        var catalog = RecipeIndexService.catalog(helper.getLevel(), furnace, adapter.orElseThrow());
        helper.assertFalse(catalog.recipes().isEmpty(), "smelting catalog is empty");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void furnaceTransferIgnoresWrongClickedFace(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(0, 1, 0);
        helper.setBlock(relativePos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(relativePos);
        var adapter = MachineAdapterRegistry.find(helper.getLevel(), furnace).orElseThrow();
        BindingRecord binding = new BindingRecord(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 0))),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relativePos)),
                Direction.NORTH,
                "anchor",
                "target",
                adapter.id().toString(),
                adapter.schemaVersion(),
                helper.getLevel().getGameTime(),
                helper.getLevel().getGameTime());

        helper.assertTrue(adapter.insert(helper.getLevel(), binding, new ItemStack(Items.RAW_IRON)),
                "front-face binding did not fall back to the furnace input");
        helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "input was not placed in slot 0");
        helper.assertTrue(furnace.getItem(1).isEmpty(), "input was incorrectly placed in the fuel slot");

        furnace.setItem(2, new ItemStack(Items.IRON_INGOT));
        ItemStack simulated = adapter.extractAnyOutput(helper.getLevel(), binding, true);
        helper.assertTrue(simulated.is(Items.IRON_INGOT), "output extraction simulation failed");
        helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "simulation removed the output");
        ItemStack extracted = adapter.extractAnyOutput(helper.getLevel(), binding, false);
        helper.assertTrue(extracted.is(Items.IRON_INGOT), "output was not extracted");
        helper.assertTrue(furnace.getItem(2).isEmpty(), "committed extraction left the output behind");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void incomingBufferPersistsOwnedInput(GameTestHelper helper) {
        UUID bindingId = UUID.randomUUID();
        BindingRecord binding = new BindingRecord(
                1,
                bindingId,
                UUID.randomUUID(),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(0, 1, 0))),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 0))),
                Direction.UP,
                "anchor",
                "target",
                "minecraft:furnace",
                1,
                helper.getLevel().getGameTime(),
                helper.getLevel().getGameTime());
        IncomingBuffer original = new IncomingBuffer();
        original.enqueue(binding, "pattern", new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT), 200);
        var tag = new net.minecraft.nbt.CompoundTag();
        original.save(tag, helper.getLevel().registryAccess());

        IncomingBuffer restored = new IncomingBuffer();
        restored.load(tag, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.recoverableDrops().size(), 1, "buffered input count changed");
        helper.assertTrue(restored.recoverableDrops().getFirst().is(Items.RAW_IRON),
                "buffered input item changed");
        helper.assertValueEqual(restored.removeBinding(bindingId).size(), 1,
                "unbind did not recover buffered input");
        helper.assertTrue(restored.recoverableDrops().isEmpty(),
                "unbind left an owned input in the queue");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mekanismAdapterLoadsConditionally(GameTestHelper helper) {
        var smelter = BuiltInRegistries.BLOCK.getOptional(
                ResourceLocation.fromNamespaceAndPath("mekanism", "energized_smelter"));
        if (smelter.isEmpty()) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, smelter.get());
        BlockEntity machine = helper.getBlockEntity(pos);
        var adapter = MachineAdapterRegistry.find(helper.getLevel(), machine);
        helper.assertTrue(adapter.isPresent(), "Mekanism smelting adapter was not found");
        helper.assertValueEqual(adapter.orElseThrow().id().toString(), "mekanism:smelting",
                "wrong Mekanism adapter selected");
        var catalog = RecipeIndexService.catalog(helper.getLevel(), machine, adapter.orElseThrow());
        helper.assertFalse(catalog.recipes().isEmpty(), "Mekanism smelting catalog is empty");
        helper.succeed();
    }
}
