package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.minecraft.item.ItemStack;

public final class ItemDurabilityPricing {
	private ItemDurabilityPricing() {}

	/**
	 * Возвращает модификатор цены на основе текущей прочности предмета.
	 * При 100% прочности возвращает 100, за каждые missingDurabilityStep% 
	 * прочности уменьшает на durabilityPriceReductionPerStep.
	 */
	public static int getDurabilityPricePercent(ItemStack stack, TradeOverhaulSettings settings) {
		if (!stack.isDamageable()) {
			return 100;
		}
		
		int maxDamage = stack.getMaxDamage();
		if (maxDamage <= 0) {
			return 100;
		}
		
		int damage = stack.getDamage();
		int durabilityPercent = (int) Math.round(((double) (maxDamage - damage) / maxDamage) * 100.0);
		
		int step = settings.durabilityPriceStep;
		int reductionPerStep = settings.durabilityPriceReductionPerStep;
		
		int missingPercent = 100 - durabilityPercent;
		int stepsMissing = missingPercent / step;
		int totalReduction = stepsMissing * reductionPerStep;
		
		return Math.max(0, 100 - totalReduction);
	}

	/**
	 * Применяет модификатор прочности к цене.
	 */
	public static int applyDurabilityModifier(int basePrice, ItemStack stack, TradeOverhaulSettings settings) {
		if (basePrice <= 0) return 0;
		int percent = getDurabilityPricePercent(stack, settings);
		return (basePrice * percent) / 100;
	}
}
