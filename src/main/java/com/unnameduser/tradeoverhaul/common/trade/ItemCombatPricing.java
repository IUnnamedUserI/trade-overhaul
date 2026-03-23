package com.unnameduser.tradeoverhaul.common.trade;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

public final class ItemCombatPricing {
	private ItemCombatPricing() {}

	public static float attackDamageContribution(ItemStack stack) {
		float sum = 0;
		for (EntityAttributeModifier mod : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
			sum += (float) mod.getValue();
		}
		return Math.max(0, sum);
	}

	public static float miningSpeedContribution(ItemStack stack) {
		float sum = 0;
		for (EntityAttributeModifier mod : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
			sum += (float) mod.getValue();
		}
		return Math.max(0, sum);
	}

	public static float efficiencyContribution(ItemStack stack) {
		float baseMiningSpeed = stack.getItem().getMiningSpeedMultiplier(stack, Blocks.STONE.getDefaultState());
		return Math.max(0, baseMiningSpeed - 1.0f);
	}
}
