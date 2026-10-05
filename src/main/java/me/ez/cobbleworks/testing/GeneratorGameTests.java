package me.ez.cobbleworks.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.ez.cobbleworks.Config;
import me.ez.cobbleworks.Init;
import me.ez.cobbleworks.Main;
import me.ez.cobbleworks.common.GeneratorBlock;
import me.ez.cobbleworks.common.GeneratorBlockEntity;
import me.ez.cobbleworks.common.MachinePhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real-server integration tests, registered only in the explicit gameTestServer run. */
public final class GeneratorGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("missing_buckets", GeneratorGameTests::missing);
        TESTS.put("infinite_catalysts", GeneratorGameTests::production);
        TESTS.put("full_buffer", GeneratorGameTests::full);
        TESTS.put("redstone_required", GeneratorGameTests::redstone);
        TESTS.put("transactional_automation", GeneratorGameTests::transactions);
        TESTS.put("save_reload", GeneratorGameTests::saveReload);
        TESTS.put("hopper_extraction", GeneratorGameTests::hopper);
        TESTS.put("auto_transport", GeneratorGameTests::export);
        TESTS.put("break_drops_inventory", GeneratorGameTests::breaking);
        TESTS.put("reservoir_blockstates", GeneratorGameTests::reservoirs);
    }
    private GeneratorGameTests() {}
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath(Main.MOD_ID, name); }
    public static void register(IEventBus bus) {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, Main.MOD_ID);
        TESTS.forEach((name, fn) -> functions.register(name, () -> fn));
        functions.register(bus);
        bus.addListener(GeneratorGameTests::tests);
    }
    private static void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("workshop"));
        TESTS.forEach((name, fn) -> event.registerTest(id(name),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                        new TestData<>(environment, id("empty"), 100, 0, true))));
    }
    private static GeneratorBlockEntity place(GameTestHelper h, boolean ready) {
        h.setBlock(POS, Init.GENERATOR.get());
        GeneratorBlockEntity be = h.getBlockEntity(POS, GeneratorBlockEntity.class);
        if (ready) { be.setItem(0, new ItemStack(Items.WATER_BUCKET)); be.setItem(1, new ItemStack(Items.LAVA_BUCKET)); }
        return be;
    }
    /** Production now always requires an active redstone signal. */
    private static void power(GameTestHelper h) { h.setBlock(POS.east(), Blocks.REDSTONE_BLOCK); }
    private static void ticks(GameTestHelper h, GeneratorBlockEntity be, int n) {
        for (int i = 0; i < n; i++) GeneratorBlockEntity.tick(h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private static void buckets(GameTestHelper h, GeneratorBlockEntity be) {
        h.assertTrue(be.getItem(0).is(Items.WATER_BUCKET) && be.getItem(0).getCount() == 1, "Water catalyst must remain");
        h.assertTrue(be.getItem(1).is(Items.LAVA_BUCKET) && be.getItem(1).getCount() == 1, "Lava catalyst must remain");
    }
    private static void missing(GameTestHelper h) {
        var be = place(h, false);
        ticks(h, be, 80); h.assertValueEqual(be.stored(), 0, "No production without buckets");
        be.setItem(0, new ItemStack(Items.WATER_BUCKET));
        ticks(h, be, 80); h.assertValueEqual(be.stored(), 0, "Both buckets required");
        h.assertValueEqual(be.getBlockState().getValue(GeneratorBlock.PHASE), MachinePhase.WAITING, "Waiting state");
        h.succeed();
    }
    private static void reservoirs(GameTestHelper h) {
        var be = place(h, false);
        ticks(h, be, 1);
        h.assertTrue(!be.getBlockState().getValue(GeneratorBlock.WATER) && !be.getBlockState().getValue(GeneratorBlock.LAVA), "Empty reservoirs");
        be.setItem(0, new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(be.getBlockState().getValue(GeneratorBlock.WATER) && !be.getBlockState().getValue(GeneratorBlock.LAVA), "Water alone updates model immediately");
        be.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        ticks(h, be, 1);
        h.assertTrue(be.getBlockState().getValue(GeneratorBlock.WATER) && be.getBlockState().getValue(GeneratorBlock.LAVA), "Phase transition preserves both reservoir states");
        be.control(0);
        be.removeItem(0, 1);
        h.assertTrue(!be.getBlockState().getValue(GeneratorBlock.WATER) && be.getBlockState().getValue(GeneratorBlock.LAVA), "Removal while paused empties only water reservoir");
        be.removeItem(1, 1);
        h.assertTrue(!be.getBlockState().getValue(GeneratorBlock.WATER) && !be.getBlockState().getValue(GeneratorBlock.LAVA), "Both removed empties model");
        h.succeed();
    }
    private static void production(GameTestHelper h) {
        var be = place(h, true); power(h);
        ticks(h, be, Config.CYCLE_TICKS.get() - 1);
        h.assertValueEqual(be.stored(), 0, "No premature output");
        ticks(h, be, 1 + Config.CYCLE_TICKS.get());
        h.assertValueEqual(be.stored(), 2 * Config.OUTPUT_PER_CYCLE.get(), "Two completed cycles");
        buckets(h, be); h.succeed();
    }
    private static void full(GameTestHelper h) {
        var be = place(h, true); power(h);
        for (int i = 2; i < be.getContainerSize(); i++) be.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        ticks(h, be, Config.CYCLE_TICKS.get() * 2);
        h.assertValueEqual(be.stored(), 576, "Full inventory unchanged");
        h.assertValueEqual(be.getBlockState().getValue(GeneratorBlock.PHASE), MachinePhase.FULL, "Full state");
        be.removeItem(2, Config.OUTPUT_PER_CYCLE.get());
        ticks(h, be, Config.CYCLE_TICKS.get());
        h.assertValueEqual(be.stored(), 576, "Resumes after extraction"); buckets(h, be); h.succeed();
    }
    private static void redstone(GameTestHelper h) {
        var be = place(h, true);
        ticks(h, be, Config.CYCLE_TICKS.get());
        h.assertValueEqual(be.stored(), 0, "No production without signal");
        h.assertValueEqual(be.getBlockState().getValue(GeneratorBlock.PHASE), MachinePhase.PAUSED, "Paused without signal");
        power(h);
        ticks(h, be, Config.CYCLE_TICKS.get());
        h.assertValueEqual(be.stored(), Config.OUTPUT_PER_CYCLE.get(), "Produces with signal");
        h.setBlock(POS.east(), Blocks.AIR);
        ticks(h, be, Config.CYCLE_TICKS.get() * 2);
        h.assertValueEqual(be.stored(), Config.OUTPUT_PER_CYCLE.get(), "Stops when signal removed");
        be.control(0);
        power(h);
        ticks(h, be, Config.CYCLE_TICKS.get() * 2);
        h.assertValueEqual(be.stored(), Config.OUTPUT_PER_CYCLE.get(), "Manual pause overrides signal");
        buckets(h, be); h.succeed();
    }
    private static void transactions(GameTestHelper h) {
        var be = place(h, false);
        var top = be.handler(Direction.UP);
        var water = ItemResource.of(new ItemStack(Items.WATER_BUCKET));
        try (var tx = Transaction.openRoot()) { h.assertValueEqual(top.insert(water, 1, tx), 1, "Simulated insertion"); }
        h.assertTrue(be.getItem(0).isEmpty(), "Aborted insertion must rollback");
        try (var tx = Transaction.openRoot()) { top.insert(water, 1, tx); tx.commit(); }
        try (var tx = Transaction.openRoot()) { h.assertValueEqual(top.extract(water, 1, tx), 0, "Automation cannot steal catalyst"); tx.commit(); }
        be.setItem(2, new ItemStack(Items.COBBLESTONE, 8));
        var bottom = be.handler(Direction.DOWN);
        var cobble = ItemResource.of(new ItemStack(Items.COBBLESTONE));
        try (var tx = Transaction.openRoot()) { h.assertValueEqual(bottom.extract(cobble, 4, tx), 4, "Simulated extraction"); }
        h.assertValueEqual(be.stored(), 8, "Aborted extraction must rollback");
        try (var tx = Transaction.openRoot()) { bottom.extract(cobble, 4, tx); tx.commit(); }
        h.assertValueEqual(be.stored(), 4, "Committed extraction");
        try (var tx = Transaction.openRoot()) { h.assertValueEqual(bottom.insert(cobble, 4, tx), 0, "Output cannot accept inserts"); }
        h.succeed();
    }
    private static void saveReload(GameTestHelper h) {
        var be = place(h, true); power(h); ticks(h, be, Math.max(0, Config.CYCLE_TICKS.get() / 2));
        be.setItem(2, new ItemStack(Items.COBBLESTONE, 23)); be.control(0);
        var tag = be.saveWithFullMetadata(h.getLevel().registryAccess());
        var loaded = (GeneratorBlockEntity) BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, h.getLevel().registryAccess());
        h.assertTrue(loaded != null, "Load must recreate block entity");
        h.assertValueEqual(loaded.stored(), 23, "Persisted inventory");
        for (int i : new int[]{0, 3, 4}) h.assertValueEqual(loaded.data.get(i), be.data.get(i), "Persisted control/progress " + i);
        buckets(h, loaded); h.succeed();
    }
    private static void hopper(GameTestHelper h) {
        var be = place(h, true); be.setItem(2, new ItemStack(Items.COBBLESTONE, 4));
        h.setBlock(POS.below(), Blocks.HOPPER);
        var hopper = h.getBlockEntity(POS.below(), HopperBlockEntity.class);
        h.assertTrue(HopperBlockEntity.suckInItems(h.getLevel(), hopper), "Bottom hopper must pull output");
        h.assertValueEqual(be.stored(), 3, "One cobble extracted");
        h.assertTrue(hopper.getItem(0).is(Items.COBBLESTONE), "Cobble arrives in hopper"); buckets(h, be); h.succeed();
    }
    private static void export(GameTestHelper h) {
        var be = place(h, true); be.setItem(2, new ItemStack(Items.COBBLESTONE, 16));
        h.setBlock(POS.below(), Blocks.CHEST);
        var chest = h.getBlockEntity(POS.below(), ChestBlockEntity.class);
        h.runAtTickTime(20, () -> {
            int moved = 0; for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.COBBLESTONE)) moved += chest.getItem(i).getCount();
            h.assertTrue(moved > 0, "Output must always flow into a connected transport");
            h.assertValueEqual(moved + be.stored(), 16, "No transfer loss or duplication"); buckets(h, be); h.succeed();
        });
    }
    private static void breaking(GameTestHelper h) {
        var be = place(h, true); be.setItem(2, new ItemStack(Items.COBBLESTONE, 4));
        h.setBlock(POS, Blocks.AIR);
        h.assertItemEntityPresent(Items.WATER_BUCKET, POS, 2);
        h.assertItemEntityPresent(Items.LAVA_BUCKET, POS, 2);
        h.assertItemEntityPresent(Items.COBBLESTONE, POS, 2);
        h.succeed();
    }
}
