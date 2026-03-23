package com.unnameduser.tradeoverhaul.client.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Слот витрины жителя: нельзя забирать предметы мышью и класть свои (только логика торговли по ПКМ).
 */
public class VillagerReadOnlySlot extends Slot {
	public VillagerReadOnlySlot(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public boolean canTakeItems(PlayerEntity playerEntity) {
		return false;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return false;
	}
}
