package com.unnameduser.tradeoverhaul.dto;

import net.minecraft.item.ItemStack;

import java.util.List;

public class DisassemblyRecipe {
    private String id;
    private int required_level;
    private ItemStack target_item;           // Какой предмет разбираем
    private List<YieldTier> yield_tiers;     // Что даём в зависимости от прочности
    private int cost;

    public DisassemblyRecipe(String id, int required_level, ItemStack target_item,
                             List<YieldTier> yield_tiers, int cost) {
        this.id = id;
        this.required_level = required_level;
        this.target_item = target_item;
        this.yield_tiers = yield_tiers;
        this.cost = cost;
    }

    public String getId() { return id; }
    public int getRequiredLevel() { return required_level; }
    public ItemStack getTargetItem() { return target_item; }
    public List<YieldTier> getYieldTiers() { return yield_tiers; }
    public int getCost() { return cost; }
}