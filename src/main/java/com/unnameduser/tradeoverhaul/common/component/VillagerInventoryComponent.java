package com.unnameduser.tradeoverhaul.common.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

public class VillagerInventoryComponent implements Inventory {
    private final DefaultedList<ItemStack> items;
    private final int size;

    public VillagerInventoryComponent() {
        this.size = 36;
        this.items = DefaultedList.ofSize(size, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = items.get(slot);
        if (!stack.isEmpty()) {
            if (stack.getCount() <= amount) {
                items.set(slot, ItemStack.EMPTY);
                return stack;
            } else {
                return stack.split(amount);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
    }

    public void readNbt(NbtCompound nbt) {
        items.clear();
        Inventories.readNbt(nbt, items);
    }

    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
    }
}