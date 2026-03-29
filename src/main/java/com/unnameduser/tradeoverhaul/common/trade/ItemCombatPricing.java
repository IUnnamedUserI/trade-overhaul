package com.unnameduser.tradeoverhaul.common.trade;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;

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

	/**
	 * Возвращает "уровень" предмета на основе его материала.
	 * Используется для инструментов, оружия и брони.
	 * 
	 * @return 0 для деревянного/золотого, 1 для каменного, 2 для железного,
	 *         3 для алмазного, 4 для незеритового, -1 если не определено
	 */
	public static int getMaterialTier(ItemStack stack) {
		if (stack.isEmpty()) return -1;
		
		// Проверка на незерит по названию предмета
		String itemId = Registries.ITEM.getId(stack.getItem()).getPath();
		if (itemId.contains("netherite")) {
			return 4;
		}
		
		// Для инструментов с ToolMaterial (включая мотыги)
		if (stack.getItem() instanceof net.minecraft.item.ToolItem toolItem) {
			ToolMaterial mat = toolItem.getMaterial();
			return getTierFromMaterial(mat);
		}
		
		// Для мечей
		if (stack.getItem() instanceof net.minecraft.item.SwordItem swordItem) {
			ToolMaterial mat = swordItem.getMaterial();
			return getTierFromMaterial(mat);
		}
		
		// Для брони
		if (stack.getItem() instanceof ArmorItem armorItem) {
			ArmorMaterial mat = armorItem.getMaterial();
			String matName = mat.getName();
			if (matName.contains("netherite")) return 4;
			if (matName.contains("diamond")) return 3;
			if (matName.contains("iron")) return 2;
			if (matName.contains("chain") || matName.contains("gold")) return 0;
			if (matName.contains("leather")) return 0;
		}
		
		// Для мотыг (HoeItem может не быть ToolItem в некоторых версиях)
		if (itemId.contains("hoe")) {
			// Определяем по префиксу названия
			if (itemId.contains("diamond_hoe")) return 3;
			if (itemId.contains("iron_hoe")) return 2;
			if (itemId.contains("stone_hoe")) return 1;
			if (itemId.contains("golden_hoe")) return 0;
			if (itemId.contains("wooden_hoe")) return 0;
		}
		
		// По умолчанию - 0 (деревянный/золотой)
		return 0;
	}

	private static int getTierFromMaterial(ToolMaterial mat) {
		// Определяем уровень по прочности и эффективности
		int durability = mat.getDurability();
		float efficiency = mat.getMiningSpeedMultiplier();
		
		// Незерит: 2031 прочность, 9.0 эффективность
		if (durability >= 2000 && efficiency >= 9.0f) return 4;
		// Алмаз: 1561 прочность, 8.0 эффективность
		if (durability >= 1500 && efficiency >= 8.0f) return 3;
		// Железо: 250 прочность, 6.0 эффективность
		if (durability >= 250 && efficiency >= 6.0f) return 2;
		// Камень: 131 прочность, 4.0 эффективность
		if (durability >= 130 && efficiency >= 4.0f) return 1;
		// Дерево/золото: 59-60 прочность, 2.0-12.0 эффективность
		return 0;
	}
}
