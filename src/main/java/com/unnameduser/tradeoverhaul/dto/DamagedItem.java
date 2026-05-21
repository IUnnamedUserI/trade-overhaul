package com.unnameduser.tradeoverhaul.dto;

import net.minecraft.item.ItemStack;

public class DamagedItem {
    private final ItemStack stack;
    private final int slotIndex;
    private final int currentDamage;
    private final int maxDamage;

    public DamagedItem(ItemStack stack, int slotIndex) {
        this.stack = stack;
        this.slotIndex = slotIndex;
        this.currentDamage = stack.getDamage();
        this.maxDamage = stack.getMaxDamage();
    }

    public ItemStack getStack() { return stack; }
    public int getSlotIndex() { return slotIndex; }
    public int getCurrentDamage() { return currentDamage; }
    public int getMaxDamage() { return maxDamage; }
    public int getMissingDurability() { return maxDamage - currentDamage; }
    public int getRepairCost() { return currentDamage * 2; } // 2 монеты за 1 прочность
    public float getDamagePercent() { return (float) currentDamage / maxDamage * 100; }
}