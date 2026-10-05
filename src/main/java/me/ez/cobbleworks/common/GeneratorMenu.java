package me.ez.cobbleworks.common;

import me.ez.cobbleworks.Init;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GeneratorMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData data;
    public GeneratorMenu(int id, Inventory player) {
        this(id, player, new SimpleContainer(GeneratorBlockEntity.SIZE), new SimpleContainerData(GeneratorBlockEntity.DATA_SIZE));
    }
    public GeneratorMenu(int id, Inventory player, Container inventory, ContainerData data) {
        super(Init.GENERATOR_MENU.get(), id);
        this.inventory = inventory; this.data = data;
        checkContainerSize(inventory, GeneratorBlockEntity.SIZE);
        checkContainerDataCount(data, GeneratorBlockEntity.DATA_SIZE);
        addDataSlots(data);
        addSlot(new Slot(inventory, 0, 12, 80) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.WATER_BUCKET); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inventory, 1, 44, 80) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.LAVA_BUCKET); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            addSlot(new Slot(inventory, 2 + row * 3 + col, 120 + col * 18, 44 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addStandardInventorySlots(player, 8, 122);
    }
    public int progress() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public MachinePhase phase() { return MachinePhase.values()[Math.clamp(data.get(2), 0, 3)]; }
    public boolean enabled() { return data.get(3) != 0; }
    public int stored() { return data.get(4); }
    public boolean hasWater() { return inventory.getItem(0).is(Items.WATER_BUCKET); }
    public boolean hasLava() { return inventory.getItem(1).is(Items.LAVA_BUCKET); }
    @Override public boolean stillValid(Player player) { return inventory.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !stillValid(player) || !(inventory instanceof GeneratorBlockEntity be)) return false;
        be.control(button); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index < GeneratorBlockEntity.SIZE) moved = moveItemStackTo(stack, GeneratorBlockEntity.SIZE, slots.size(), true);
        else if (stack.is(Items.WATER_BUCKET)) moved = moveItemStackTo(stack, 0, 1, false);
        else if (stack.is(Items.LAVA_BUCKET)) moved = moveItemStackTo(stack, 1, 2, false);
        else if (index < GeneratorBlockEntity.SIZE + 27) moved = moveItemStackTo(stack, GeneratorBlockEntity.SIZE + 27, slots.size(), false);
        else moved = moveItemStackTo(stack, GeneratorBlockEntity.SIZE, GeneratorBlockEntity.SIZE + 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
