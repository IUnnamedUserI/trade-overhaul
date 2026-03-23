package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class TradePricing {
	private TradePricing() {}

	public static int buyPriceForStack(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty()) return 0;
		TradeOverhaulSettings s = TradeConfigLoader.getSettings();
		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Static pool - exact item match
		for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
			if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
				int price = Math.max(1, e.buy);
				price = applyItemMultiplier(price, id, profession);
				return applyDurabilityIfEnabled(price, stack, s, false, false);
			}
		}

		// Weapon pool - by tag
		for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
			if (w.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(w.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				float dmg = ItemCombatPricing.attackDamageContribution(stack);
				int bpd = w.buyPerDamage != null ? w.buyPerDamage : s.weaponBuyPerDamage;
				int bb = w.buyBase != null ? w.buyBase : s.weaponBuyBase;
				boolean useDurability = w.useDurability != null ? w.useDurability : s.weaponUseDurability;
				int price = Math.max(1, bb + Math.round(dmg * bpd));
				price = applyItemMultiplier(price, id, profession);
				return applyDurabilityIfEnabled(price, stack, s, useDurability, false);
			}
		}

		// Tool pool - by tag
		for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
			if (t.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(t.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				float eff = ItemCombatPricing.efficiencyContribution(stack);
				int bpe = t.buyPerEfficiency != null ? t.buyPerEfficiency : s.toolBuyPerEfficiency;
				int bb = t.buyBase != null ? t.buyBase : s.toolBuyBase;
				boolean useDurability = t.useDurability != null ? t.useDurability : s.toolUseDurability;
				int price = Math.max(1, bb + Math.round(eff * bpe));
				price = applyItemMultiplier(price, id, profession);
				return applyDurabilityIfEnabled(price, stack, s, useDurability, false);
			}
		}

		// General pool - by tag
		for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
			if (g.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(g.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				int price = g.buyPrice != null ? g.buyPrice : 1;
				price = applyItemMultiplier(price, id, profession);
				boolean useDurability = g.useDurability != null ? g.useDurability : false;
				return applyDurabilityIfEnabled(price, stack, s, useDurability, false);
			}
		}

		// Buy-only pool - items villager buys but doesn't sell
		for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
			if (b.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(b.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				int discount = b.discountFactor != null ? b.discountFactor : s.buyOnlyDiscountDefault;
				int basePrice = b.buyPrice != null ? b.buyPrice : 1;
				int buyBase = b.buyBase != null ? b.buyBase : 0;
				
				// Calculate price based on damage or efficiency
				int price = buyBase;
				if (b.buyPricePerDamage != null && b.buyPricePerDamage > 0) {
					float dmg = ItemCombatPricing.attackDamageContribution(stack);
					price += Math.round(dmg * b.buyPricePerDamage);
				} else if (b.buyPricePerEfficiency != null && b.buyPricePerEfficiency > 0) {
					float eff = ItemCombatPricing.efficiencyContribution(stack);
					price += Math.round(eff * b.buyPricePerEfficiency);
				} else {
					price += basePrice;
				}
				
				// Apply discount (lower price for items not in sell pool)
				price = (price * discount) / 100;
				price = applyItemMultiplier(price, id, profession);
				
				boolean useDurability = b.useDurability != null ? b.useDurability : false;
				return applyDurabilityIfEnabled(Math.max(1, price), stack, s, useDurability, false);
			}
		}

		return 0;
	}

	public static int sellPriceForStack(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty()) return 0;
		TradeOverhaulSettings s = TradeConfigLoader.getSettings();
		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Static pool - exact item match
		for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
			if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
				int buyPrice = Math.max(1, e.buy);
				buyPrice = applyItemMultiplier(buyPrice, id, profession);
				int sellPrice = Math.max(0, Math.min(e.sell, buyPrice - 1));
				return applyDurabilityIfEnabled(sellPrice, stack, s, false, false);
			}
		}

		// Weapon pool - by tag
		for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
			if (w.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(w.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				float dmg = ItemCombatPricing.attackDamageContribution(stack);
				int spd = w.sellPerDamage != null ? w.sellPerDamage : s.weaponSellPerDamage;
				int sb = w.sellBase != null ? w.sellBase : s.weaponSellBase;
				boolean useDurability = w.useDurability != null ? w.useDurability : s.weaponUseDurability;
				int buy = buyPriceForStack(stack, profession);
				int sell = Math.max(0, sb + Math.round(dmg * spd));
				sell = Math.min(sell, Math.max(0, buy - 1));
				return applyDurabilityIfEnabled(sell, stack, s, useDurability, true);
			}
		}

		// Tool pool - by tag
		for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
			if (t.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(t.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				float eff = ItemCombatPricing.efficiencyContribution(stack);
				int spe = t.sellPerEfficiency != null ? t.sellPerEfficiency : s.toolSellPerEfficiency;
				int sb = t.sellBase != null ? t.sellBase : s.toolSellBase;
				boolean useDurability = t.useDurability != null ? t.useDurability : s.toolUseDurability;
				int buy = buyPriceForStack(stack, profession);
				int sell = Math.max(0, sb + Math.round(eff * spe));
				sell = Math.min(sell, Math.max(0, buy - 1));
				return applyDurabilityIfEnabled(sell, stack, s, useDurability, true);
			}
		}

		// General pool - by tag
		for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
			if (g.tag == null) continue;
			TagKey<net.minecraft.item.Item> tag = getTag(g.tag);
			if (tag == null) continue;
			if (stack.isIn(tag)) {
				int buy = buyPriceForStack(stack, profession);
				int sell = g.sellPrice != null ? g.sellPrice : 1;
				sell = Math.min(sell, Math.max(0, buy - 1));
				boolean useDurability = g.useDurability != null ? g.useDurability : false;
				return applyDurabilityIfEnabled(sell, stack, s, useDurability, true);
			}
		}

		// Buy-only pool - villagers don't sell these, only buy
		return 0;
	}

	private static int applyItemMultiplier(int price, Identifier itemId, ProfessionTradeFile profession) {
		if (profession.itemPriceMultipliers == null || profession.itemPriceMultipliers.isEmpty()) {
			return price;
		}
		// Try exact item match first
		String itemKey = itemId.toString();
		if (profession.itemPriceMultipliers.containsKey(itemKey)) {
			double multiplier = profession.itemPriceMultipliers.get(itemKey);
			return (int) Math.round(price * multiplier);
		}
		// Try namespace match (e.g., "minecraft:diamond_sword" matches "minecraft:diamond_*")
		for (Map.Entry<String, Double> entry : profession.itemPriceMultipliers.entrySet()) {
			String pattern = entry.getKey();
			if (pattern.endsWith("*")) {
				String prefix = pattern.substring(0, pattern.length() - 1);
				if (itemKey.startsWith(prefix)) {
					return (int) Math.round(price * entry.getValue());
				}
			}
		}
		return price;
	}

	private static TagKey<net.minecraft.item.Item> getTag(String tagString) {
		Identifier tagId = Identifier.tryParse(tagString);
		if (tagId == null) return null;
		return TagKey.of(RegistryKeys.ITEM, tagId);
	}

	private static int applyDurabilityIfEnabled(int price, ItemStack stack, TradeOverhaulSettings settings, 
			boolean useDurability, boolean isSellPrice) {
		if (!useDurability || price <= 0) return price;
		int modified = ItemDurabilityPricing.applyDurabilityModifier(price, stack, settings);
		// Ensure sell price is always at least 1 if buy price allows trading
		if (isSellPrice && modified <= 0 && price > 0) {
			return 1;
		}
		return modified;
	}
}
