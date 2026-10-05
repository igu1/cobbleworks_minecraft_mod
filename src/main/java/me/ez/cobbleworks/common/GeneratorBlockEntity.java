package me.ez.cobbleworks.common;

import java.util.EnumMap;
import me.ez.cobbleworks.Config;
import me.ez.cobbleworks.Init;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Server-only production; buckets are reusable catalysts, never drained or duplicated. */
public final class GeneratorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SIZE = 11, DATA_SIZE = 5;
    private static final int[] INPUT = {0, 1};
    private static final int[] OUTPUT = {2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] SIDES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnumMap<Direction, ResourceHandler<ItemResource>> handlers = new EnumMap<>(Direction.class);
    private int progress, lastComparator = -1;
    private boolean enabled = true;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress; case 1 -> Config.CYCLE_TICKS.get();
                case 2 -> getBlockState().getValue(GeneratorBlock.PHASE).ordinal();
                case 3 -> enabled ? 1 : 0; case 4 -> stored();
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {}
        @Override public int getCount() { return DATA_SIZE; }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) { super(Init.GENERATOR_BE.get(), pos, state); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.cobbleworks.cobble_generator"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new GeneratorMenu(id, inventory, this, data); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(Items.WATER_BUCKET) : slot == 1 && stack.is(Items.LAVA_BUCKET);
    }
    @Override public int[] getSlotsForFace(Direction side) { return (side == Direction.UP ? INPUT : side == Direction.DOWN ? OUTPUT : SIDES).clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2 && side != Direction.UP; }

    public ResourceHandler<ItemResource> handler(Direction side) {
        return handlers.computeIfAbsent(side, d -> new WorldlyContainerWrapper(this, d));
    }
    public int stored() {
        int total = 0;
        for (int i = 2; i < SIZE; i++) if (items.get(i).is(Items.COBBLESTONE)) total += items.get(i).getCount();
        return total;
    }
    private int room() {
        int room = 0;
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        for (int i = 2; i < SIZE; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) room += cobble.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, cobble)) room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
        }
        return room;
    }
    public void control(int button) {
        if (button != 0) return;
        enabled = !enabled;
        setChanged();
    }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            syncReservoirs();
            int signal = GeneratorRules.comparator(stored());
            if (signal != lastComparator) {
                lastComparator = signal;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    private void syncReservoirs() {
        if (level == null || level.isClientSide() || isRemoved()) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!state.is(Init.GENERATOR.get())) return;
        boolean water = items.get(0).is(Items.WATER_BUCKET);
        boolean lava = items.get(1).is(Items.LAVA_BUCKET);
        if (state.getValue(GeneratorBlock.WATER) != water || state.getValue(GeneratorBlock.LAVA) != lava) {
            level.setBlock(worldPosition, state.setValue(GeneratorBlock.WATER, water).setValue(GeneratorBlock.LAVA, lava), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if (!(level instanceof ServerLevel)) return;
        be.syncReservoirs();
        // Always push into a connected transport below, regardless of production state.
        if ((level.getGameTime() + pos.asLong()) % 8 == 0) be.exportBelow();
        int duration = Config.CYCLE_TICKS.get();
        be.progress = GeneratorRules.boundedProgress(be.progress, duration);
        MachinePhase phase;
        if (!be.items.get(0).is(Items.WATER_BUCKET) || !be.items.get(1).is(Items.LAVA_BUCKET)) {
            phase = MachinePhase.WAITING;
            if (be.progress != 0) { be.progress = 0; be.setChanged(); }
        } else if (!Config.ENABLED.get() || !be.enabled || !level.hasNeighborSignal(pos)) {
            phase = MachinePhase.PAUSED;
        } else if (be.room() < Config.OUTPUT_PER_CYCLE.get()) {
            phase = MachinePhase.FULL;
        } else {
            phase = MachinePhase.RUNNING;
            if (++be.progress >= duration) {
                be.produce(Config.OUTPUT_PER_CYCLE.get());
                be.progress = 0;
                be.setChanged();
            } else {
                // Persist progress, without broadcasting a full inventory packet every tick.
                be.setChanged();
            }
        }
        BlockState current = level.getBlockState(pos);
        if (current.getValue(GeneratorBlock.PHASE) != phase) level.setBlock(pos, current.setValue(GeneratorBlock.PHASE, phase), 3);
    }
    private void produce(int amount) {
        for (int i = 2; i < SIZE && amount > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) { int n = Math.min(64, amount); items.set(i, new ItemStack(Items.COBBLESTONE, n)); amount -= n; }
            else if (ItemStack.isSameItemSameComponents(stack, new ItemStack(Items.COBBLESTONE))) {
                int n = Math.min(amount, stack.getMaxStackSize() - stack.getCount()); stack.grow(n); amount -= n;
            }
        }
    }
    private void exportBelow() {
        BlockPos below = worldPosition.below();
        if (level == null || !level.hasChunkAt(below)) return;
        ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, below, Direction.UP);
        if (target == null) return;
        ResourceHandler<ItemResource> source = handler(Direction.DOWN);
        for (int i = 0; i < source.size(); i++) {
            ItemResource resource = source.getResource(i);
            if (resource.isEmpty()) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(resource, Math.min(8, source.getAmountAsInt(i)), transaction);
                if (accepted > 0 && source.extract(i, resource, accepted, transaction) == accepted) transaction.commit();
            }
            return; // One stack attempt per transfer interval, including rejected transfers.
        }
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Progress", progress);
        output.putBoolean("Enabled", enabled);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = Math.clamp(input.getIntOr("Progress", 0), 0, 1199);
        enabled = input.getBooleanOr("Enabled", true);
    }
}
