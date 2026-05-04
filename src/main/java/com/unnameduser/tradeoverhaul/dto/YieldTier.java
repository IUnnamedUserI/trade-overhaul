package com.unnameduser.tradeoverhaul.dto;

import net.minecraft.item.ItemStack;
import java.util.List;

public class YieldTier {
    private double min_durability_pct;  // Минимальный % прочности для этого тира
    private List<ItemStack> drops;      // Что выпадает

    public YieldTier(double min_durability_pct, List<ItemStack> drops) {
        this.min_durability_pct = min_durability_pct;
        this.drops = drops;
    }

    public double getMinDurabilityPct() { return min_durability_pct; }
    public List<ItemStack> getDrops() { return drops; }
}