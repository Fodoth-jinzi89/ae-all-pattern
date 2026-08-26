package io.github.langqi99.aeallpattern.gametest;

import appeng.api.networking.GridFlags;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEBlocks;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternData;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternDecoder;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternExpander;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternKind;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternLibrary;
import io.github.langqi99.aeallpattern.aggregate.AggregatePatternRef;
import io.github.langqi99.aeallpattern.aggregate.AggregateRecipe;
import io.github.langqi99.aeallpattern.AeAllPattern;
import io.github.langqi99.aeallpattern.binding.BindingSavedData;
import io.github.langqi99.aeallpattern.binding.BindingRecord;
import io.github.langqi99.aeallpattern.linker.IncomingBuffer;
import io.github.langqi99.aeallpattern.linker.PatternLinkerBlockEntity;
import io.github.langqi99.aeallpattern.machine.MachineAdapterRegistry;
import io.github.langqi99.aeallpattern.recipe.RecipeIndexService;
import io.github.langqi99.aeallpattern.registry.ModBlocks;
import io.github.langqi99.aeallpattern.registry.ModDataComponents;
import io.github.langqi99.aeallpattern.registry.ModItems;
import io.github.langqi99.aeallpattern.tianshu.TianshuPatternSelectorBlock;
import io.github.langqi99.aeallpattern.tianshu.TianshuPatternSelectorBlockEntity;
import io.github.langqi99.aeallpattern.tianshu.TianshuRoutingPolicies;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;

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

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void tianshuRouterPublishesRoutingWithoutCraftingCpu(GameTestHelper helper) {
        BlockPos selectorPos = new BlockPos(1, 1, 1);
        BlockPos energyPos = new BlockPos(1, 1, 2);
        helper.setBlock(selectorPos, ModBlocks.TIANSHU_PATTERN_SELECTOR.get());
        helper.setBlock(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block());

        helper.runAfterDelay(10, () -> {
            TianshuPatternSelectorBlockEntity selector = helper.getBlockEntity(selectorPos);
            helper.assertTrue(selector != null, "Tianshu router block entity was not created");
            helper.assertTrue(selector.isRouterOnline(), "powered Tianshu router did not come online");
            helper.assertTrue(
                    selector.getGrid().getCraftingService().getCpus().isEmpty(),
                    "Tianshu router must not register as an AE crafting CPU");
            helper.assertTrue(
                    TianshuRoutingPolicies.isAvailable(selector.getGrid()),
                    "online Tianshu router was not discoverable by route planning");
            helper.assertTrue(
                    helper.getBlockState(selectorPos).getValue(TianshuPatternSelectorBlock.ACTIVE),
                    "online Tianshu router did not switch to its active model");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void binderKeepsAnchorForContinuousBindings(GameTestHelper helper) {
        BlockPos linkerPos = new BlockPos(1, 1, 1);
        BlockPos energyPos = new BlockPos(1, 1, 2);
        BlockPos firstTarget = new BlockPos(3, 1, 1);
        BlockPos secondTarget = new BlockPos(4, 1, 1);
        helper.setBlock(linkerPos, ModBlocks.PATTERN_LINKER.get());
        helper.setBlock(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(firstTarget, Blocks.FURNACE);
        helper.setBlock(secondTarget, Blocks.FURNACE);

        helper.runAfterDelay(10, () -> {
            PatternLinkerBlockEntity linker = helper.getBlockEntity(linkerPos);
            helper.assertTrue(linker != null && linker.getMainNode().isOnline(),
                    "powered linker did not come online");

            var player = helper.makeMockPlayer(GameType.CREATIVE);
            player.setPos(helper.absolutePos(linkerPos).getCenter());
            ItemStack binder = new ItemStack(ModItems.PATTERN_BINDER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, binder);

            player.setShiftKeyDown(false);
            ModItems.PATTERN_BINDER.get().useOn(context(helper, player, linkerPos));
            helper.assertTrue(binder.has(ModDataComponents.ANCHOR_SELECTION.get()),
                    "selecting a linker did not store an anchor");

            player.setShiftKeyDown(true);
            ModItems.PATTERN_BINDER.get().useOn(context(helper, player, firstTarget));
            helper.assertTrue(binder.has(ModDataComponents.ANCHOR_SELECTION.get()),
                    "first binding cleared the selected linker");
            ModItems.PATTERN_BINDER.get().useOn(context(helper, player, secondTarget));
            helper.assertTrue(binder.has(ModDataComponents.ANCHOR_SELECTION.get()),
                    "second binding cleared the selected linker");

            BindingSavedData data = BindingSavedData.get(helper.getLevel().getServer());
            var first = data.findByTarget(GlobalPos.of(
                    helper.getLevel().dimension(), helper.absolutePos(firstTarget)));
            var second = data.findByTarget(GlobalPos.of(
                    helper.getLevel().dimension(), helper.absolutePos(secondTarget)));
            helper.assertTrue(first.isPresent() && second.isPresent(),
                    "continuous binding did not create both records");
            data.remove(first.orElseThrow().bindingId());
            data.remove(second.orElseThrow().bindingId());
            helper.succeed();
        });
    }

    private static UseOnContext context(
            GameTestHelper helper, net.minecraft.world.entity.player.Player player, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        return new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                player.getMainHandItem(),
                new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false));
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
    public static void generatorCreatesPersistentAggregatePattern(GameTestHelper helper) {
        BlockPos furnacePos = new BlockPos(1, 1, 1);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
        var adapter = MachineAdapterRegistry.find(helper.getLevel(), furnace).orElseThrow();
        int expectedCount = RecipeIndexService.catalog(helper.getLevel(), furnace, adapter).recipes().size();

        AggregatePatternData captured = AggregatePatternData.capture(
                furnace, adapter, RecipeIndexService.catalog(helper.getLevel(), furnace, adapter));
        AggregatePatternRef ref = AggregatePatternLibrary.get(helper.getLevel().getServer()).put(
                helper.getLevel().getServer(), BuiltInRegistries.BLOCK.getKey(Blocks.FURNACE),
                captured.machineTranslationKey(), captured.recipes());
        ItemStack aggregate = new ItemStack(ModItems.AGGREGATE_PATTERN.get());
        aggregate.set(ModDataComponents.AGGREGATE_PATTERN.get(), ref);

        AggregatePatternRef stored = aggregate.get(ModDataComponents.AGGREGATE_PATTERN.get());
        helper.assertTrue(stored != null, "aggregate item did not retain its lightweight reference");
        helper.assertValueEqual(
                AggregatePatternLibrary.get(helper.getLevel().getServer())
                        .recipes(helper.getLevel().getServer(), stored.libraryId()).orElseThrow().size(),
                expectedCount, "server library did not retain the complete machine catalog");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void patternProviderExpandsOneAggregateIntoAllRecipes(GameTestHelper helper) {
        BlockPos providerPos = new BlockPos(1, 1, 1);
        BlockPos energyPos = new BlockPos(1, 1, 2);
        BlockPos furnacePos = new BlockPos(3, 1, 1);
        helper.setBlock(providerPos, AEBlocks.PATTERN_PROVIDER.block());
        helper.setBlock(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(furnacePos, Blocks.FURNACE);

        helper.runAfterDelay(10, () -> {
            PatternProviderBlockEntity provider = helper.getBlockEntity(providerPos);
            FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
            var adapter = MachineAdapterRegistry.find(helper.getLevel(), furnace).orElseThrow();
            var catalog = RecipeIndexService.catalog(helper.getLevel(), furnace, adapter);
            ItemStack aggregate = new ItemStack(ModItems.AGGREGATE_PATTERN.get());
            AggregatePatternData captured = AggregatePatternData.capture(furnace, adapter, catalog);
            var ref = AggregatePatternLibrary.get(helper.getLevel().getServer()).put(
                    helper.getLevel().getServer(), BuiltInRegistries.BLOCK.getKey(Blocks.FURNACE),
                    captured.machineTranslationKey(), captured.recipes());
            aggregate.set(ModDataComponents.AGGREGATE_PATTERN.get(), ref);

            helper.assertTrue(provider.getLogic().getPatternInv().isItemValid(0, aggregate),
                    "AE2 rejected the aggregate item as an encoded pattern");
            provider.getLogic().getPatternInv().setItemDirect(0, aggregate);
            provider.getLogic().updatePatterns();
            helper.assertValueEqual(provider.getLogic().getAvailablePatterns().size(), catalog.recipes().size(),
                    "one aggregate item did not publish every child pattern");
            var firstOutput = catalog.recipes().getFirst().output();
            helper.assertTrue(provider.getMainNode().getGrid().getCraftingService()
                            .isCraftable(AEItemKey.of(firstOutput)),
                    "AE network crafting service did not receive the expanded aggregate pattern");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aggregatePatternPublishesFluidOutput(GameTestHelper helper) {
        BlockPos providerPos = new BlockPos(1, 1, 1);
        BlockPos energyPos = new BlockPos(1, 1, 2);
        helper.setBlock(providerPos, AEBlocks.PATTERN_PROVIDER.block());
        helper.setBlock(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block());

        helper.runAfterDelay(10, () -> {
            PatternProviderBlockEntity provider = helper.getBlockEntity(providerPos);
            AggregateRecipe fluidRecipe = new AggregateRecipe(
                    "fluid-output-test",
                    ResourceLocation.fromNamespaceAndPath("aeallpattern", "fluid_output_test"),
                    List.of(GenericStack.fromItemStack(new ItemStack(Items.ICE))),
                    List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1_000)),
                    1);
            AggregatePatternRef ref = AggregatePatternLibrary.get(helper.getLevel().getServer()).put(
                    helper.getLevel().getServer(),
                    ResourceLocation.fromNamespaceAndPath("aeallpattern", "fluid_test_machine"),
                    "block.aeallpattern.fluid_test_machine", List.of(fluidRecipe));
            ItemStack aggregate = new ItemStack(ModItems.AGGREGATE_PATTERN.get());
            aggregate.set(ModDataComponents.AGGREGATE_PATTERN.get(), ref);

            provider.getLogic().getPatternInv().setItemDirect(0, aggregate);
            provider.getLogic().updatePatterns();
            helper.assertTrue(provider.getMainNode().getGrid().getCraftingService()
                            .isCraftable(AEFluidKey.of(Fluids.WATER)),
                    "AE network did not publish aggregate fluid output");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aggregatePreservesNativeAePatternKinds(GameTestHelper helper) {
        List<AggregateRecipe> recipes = List.of(
                new AggregateRecipe(
                        "native-crafting-test",
                        ResourceLocation.withDefaultNamespace("oak_planks"),
                        AggregatePatternKind.CRAFTING,
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.OAK_LOG))),
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.OAK_PLANKS, 4))),
                        1),
                new AggregateRecipe(
                        "native-stonecutting-test",
                        ResourceLocation.withDefaultNamespace("andesite_slab_from_andesite_stonecutting"),
                        AggregatePatternKind.STONECUTTING,
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.ANDESITE))),
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.ANDESITE_SLAB, 2))),
                        1),
                new AggregateRecipe(
                        "native-smithing-test",
                        ResourceLocation.withDefaultNamespace("netherite_sword_smithing"),
                        AggregatePatternKind.SMITHING,
                        List.of(
                                GenericStack.fromItemStack(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                                GenericStack.fromItemStack(new ItemStack(Items.DIAMOND_SWORD)),
                                GenericStack.fromItemStack(new ItemStack(Items.NETHERITE_INGOT))),
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.NETHERITE_SWORD))),
                        1),
                new AggregateRecipe(
                        "native-processing-test",
                        ResourceLocation.fromNamespaceAndPath("aeallpattern", "native_processing_test"),
                        AggregatePatternKind.PROCESSING,
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.RAW_IRON))),
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.IRON_INGOT))),
                        1),
                new AggregateRecipe(
                        "dynamic-crafting-test",
                        ResourceLocation.fromNamespaceAndPath(
                                "minecraft", "jei.shulker.color.block.minecraft.white_shulker_box"),
                        AggregatePatternKind.CRAFTING,
                        List.of(
                                GenericStack.fromItemStack(new ItemStack(Items.SHULKER_BOX)),
                                GenericStack.fromItemStack(new ItemStack(Items.WHITE_DYE))),
                        List.of(GenericStack.fromItemStack(new ItemStack(Items.WHITE_SHULKER_BOX))),
                        1));
        AggregatePatternRef ref = AggregatePatternLibrary.get(helper.getLevel().getServer()).put(
                helper.getLevel().getServer(),
                ResourceLocation.fromNamespaceAndPath("aeallpattern", "native_pattern_test_machine"),
                "block.aeallpattern.native_pattern_test_machine", recipes);
        ItemStack aggregate = new ItemStack(ModItems.AGGREGATE_PATTERN.get());
        aggregate.set(ModDataComponents.AGGREGATE_PATTERN.get(), ref);

        List<IPatternDetails> expanded = AggregatePatternExpander.expand(aggregate, helper.getLevel());
        helper.assertValueEqual(expanded.size(), 5,
                "aggregate did not expand all native and dynamic AE pattern kinds");
        helper.assertTrue(expanded.get(0) instanceof IMolecularAssemblerSupportedPattern,
                "crafting aggregate child lost molecular assembler support");
        helper.assertTrue(expanded.get(1) instanceof IMolecularAssemblerSupportedPattern,
                "stonecutting aggregate child lost native crafting support");
        helper.assertTrue(expanded.get(2) instanceof IMolecularAssemblerSupportedPattern,
                "smithing aggregate child lost native crafting support");
        helper.assertFalse(expanded.get(3) instanceof IMolecularAssemblerSupportedPattern,
                "processing aggregate child was incorrectly exposed to molecular assemblers");
        helper.assertTrue(expanded.get(4) instanceof IMolecularAssemblerSupportedPattern,
                "dynamic JEI crafting child lost molecular assembler support");

        List<String> delegateTypes = expanded.stream()
                .map(IPatternDetails::getDefinition)
                .map(definition -> PatternDetailsHelper.decodePattern(definition, helper.getLevel()))
                .map(details -> details == null ? "null" : details.getClass().getSimpleName())
                .toList();
        helper.assertValueEqual(delegateTypes.get(0), "AECraftingPattern",
                "workbench recipe was not encoded as an AE crafting pattern");
        helper.assertValueEqual(delegateTypes.get(1), "AEStonecuttingPattern",
                "stonecutter recipe was not encoded as an AE stonecutting pattern");
        helper.assertValueEqual(delegateTypes.get(2), "AESmithingTablePattern",
                "smithing recipe was not encoded as an AE smithing pattern");
        helper.assertValueEqual(delegateTypes.get(3), "AEProcessingPattern",
                "machine recipe was not kept as an AE processing pattern");
        helper.assertValueEqual(delegateTypes.get(4), "AECraftingPattern",
                "dynamic JEI recipe was not resolved to an AE crafting pattern");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aggregateDecoderIgnoresEmptyMolecularAssemblerSlot(GameTestHelper helper) {
        helper.assertTrue(
                new AggregatePatternDecoder().decodePattern((AEItemKey) null, helper.getLevel()) == null,
                "aggregate decoder did not ignore an empty AE pattern key");
        helper.assertTrue(
                PatternDetailsHelper.decodePattern(ItemStack.EMPTY, helper.getLevel()) == null,
                "AE pattern decoding did not safely ignore an empty molecular assembler slot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void appliedMekanisticsConvertsJeiChemicalToAeKey(GameTestHelper helper) {
        if (!ModList.get().isLoaded("appmek") || !ModList.get().isLoaded("ae2jeiintegration")) {
            helper.succeed();
            return;
        }
        try {
            // AppMek registers this converter from its JEI plugin constructor.
            Class.forName("me.ramidzkh.mekae2.integration.jei.AMJeiPlugin")
                    .getConstructor().newInstance();
            Class<?> convertersClass = Class.forName(
                    "tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters");
            List<?> converters = (List<?>) convertersClass.getMethod("getConverters").invoke(null);
            Object converter = converters.stream()
                    .filter(candidate -> candidate.getClass().getName().equals(
                            "me.ramidzkh.mekae2.integration.jei.ChemicalIngredientConverter"))
                    .findFirst().orElseThrow();

            Class<?> mekanismApi = Class.forName("mekanism.api.MekanismAPI");
            net.minecraft.core.Registry<?> chemicals =
                    (net.minecraft.core.Registry<?>) mekanismApi.getField("CHEMICAL_REGISTRY").get(null);
            Object oxygen = chemicals.get(ResourceLocation.fromNamespaceAndPath("mekanism", "oxygen"));
            Class<?> chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object oxygenStack = chemicalStackClass
                    .getConstructor(chemicalClass, long.class).newInstance(oxygen, 1_000L);
            Class<?> converterApi = Class.forName(
                    "tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter");
            GenericStack converted = (GenericStack) converterApi
                    .getMethod("getStackFromIngredient", Object.class).invoke(converter, oxygenStack);

            helper.assertTrue(converted != null, "AppMek chemical converter returned no AE stack");
            helper.assertValueEqual(converted.what().getType().getId().toString(), "appmek:chemical",
                    "Mekanism chemical was not converted to AppMek's AE key type");
            helper.assertValueEqual(converted.amount(), 1_000L,
                    "Mekanism chemical amount changed during JEI conversion");
            ItemStack encoded = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                    List.of(converted),
                    List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1_000L)));
            var decoded = appeng.api.crafting.PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
            helper.assertTrue(decoded != null,
                    "AE2 rejected a processing pattern containing Chemical and fluid keys");
            helper.assertTrue(java.util.Arrays.stream(decoded.getInputs()).anyMatch(input ->
                            java.util.Arrays.stream(input.getPossibleInputs())
                            .anyMatch(candidate -> candidate.what().equals(converted.what())
                                    && candidate.amount() * input.getMultiplier() == converted.amount())),
                    "AE2 processing pattern lost its Chemical input");
            helper.succeed();
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Applied Mekanistics JEI chemical conversion failed", error);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aggregateLibraryPagesLargeCatalog(GameTestHelper helper) {
        List<AggregateRecipe> recipes = new ArrayList<>();
        for (int index = 0; index < AggregatePatternLibrary.PAGE_SIZE * 2 + 1; index++) {
            String id = String.format("%064x", index + 1);
            recipes.add(new AggregateRecipe(
                    id,
                    ResourceLocation.fromNamespaceAndPath("aeallpattern", "paging_test/" + index),
                    List.of(appeng.api.stacks.GenericStack.fromItemStack(new ItemStack(Items.COBBLESTONE))),
                    List.of(appeng.api.stacks.GenericStack.fromItemStack(new ItemStack(Items.STONE))),
                    1));
        }
        var library = AggregatePatternLibrary.get(helper.getLevel().getServer());
        AggregatePatternRef ref = library.put(
                helper.getLevel().getServer(), BuiltInRegistries.BLOCK.getKey(Blocks.BLAST_FURNACE),
                Blocks.BLAST_FURNACE.getDescriptionId(), recipes);
        var entry = library.find(ref.libraryId()).orElseThrow();
        helper.assertValueEqual(entry.pageCount(), 3, "large catalog was not split into three pages");
        helper.assertValueEqual(
                library.recipes(helper.getLevel().getServer(), ref.libraryId()).orElseThrow().size(),
                recipes.size(), "paged catalog did not reconstruct all recipes");
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
