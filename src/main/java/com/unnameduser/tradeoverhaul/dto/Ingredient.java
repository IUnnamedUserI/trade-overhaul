package com.unnameduser.tradeoverhaul.dto;

import net.minecraft.item.ItemStack;

public class Ingredient {
    private ItemStack item;    // Что требуется (с возможными тегами)
    private int count;         // Сколько нужно

    public Ingredient(ItemStack item, int count) {
        this.item = item;
        this.count = count;
    }

    public ItemStack getItem() { return item; }
    public int getCount() { return count; }
}