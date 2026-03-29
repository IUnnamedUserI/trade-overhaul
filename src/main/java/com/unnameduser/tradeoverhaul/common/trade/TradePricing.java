package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class TradePricing {
	private TradePricing() {}

	/**
	 * Возвращает количество предметов за 1 бронзовую монету при покупке у жителя.
	 */
	public static int getBuyQuantity(ItemStack stack, VillagerEntity villager, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return 1;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Static pool
		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					return e.buyQuantity != null && e.buyQuantity > 0 ? e.buyQuantity : 1;
				}
			}
		}

		// Weapon pool
		if (profession.weaponPool != null && !profession.weaponPool.isEmpty()) {
			for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
				if (w.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(w.tag);
				if (tag != null && stack.isIn(tag)) {
					return w.buyQuantity != null && w.buyQuantity > 0 ? w.buyQuantity : 1;
				}
			}
		}

		// Tool pool
		if (profession.toolPool != null && !profession.toolPool.isEmpty()) {
			for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
				if (t.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(t.tag);
				if (tag != null && stack.isIn(tag)) {
					return t.buyQuantity != null && t.buyQuantity > 0 ? t.buyQuantity : 1;
				}
			}
		}

		// General pool
		if (profession.generalPool != null && !profession.generalPool.isEmpty()) {
			for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
				if (g.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(g.tag);
				if (tag != null && stack.isIn(tag)) {
					return g.buyQuantity != null && g.buyQuantity > 0 ? g.buyQuantity : 1;
				}
			}
		}

		return 1;
	}

	/**
	 * Возвращает количество предметов за 1 бронзовую монету при продаже.
	 */
	public static int getSellQuantity(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return 1;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Static pool
		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					return e.sellQuantity != null && e.sellQuantity > 0 ? e.sellQuantity : 1;
				}
			}
		}

		// Weapon pool
		if (profession.weaponPool != null && !profession.weaponPool.isEmpty()) {
			for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
				if (w.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(w.tag);
				if (tag != null && stack.isIn(tag)) {
					return w.sellQuantity != null && w.sellQuantity > 0 ? w.sellQuantity : 1;
				}
			}
		}

		// Tool pool
		if (profession.toolPool != null && !profession.toolPool.isEmpty()) {
			for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
				if (t.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(t.tag);
				if (tag != null && stack.isIn(tag)) {
					return t.sellQuantity != null && t.sellQuantity > 0 ? t.sellQuantity : 1;
				}
			}
		}

		// General pool
		if (profession.generalPool != null && !profession.generalPool.isEmpty()) {
			for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
				if (g.tag == null) continue;
				TagKey<net.minecraft.item.Item> tag = getTag(g.tag);
				if (tag != null && stack.isIn(tag)) {
					return g.sellQuantity != null && g.sellQuantity > 0 ? g.sellQuantity : 1;
				}
			}
		}

		return 1;
	}

	/**
	 * Проверяет, может ли житель купить этот предмет у игрока.
	 */
	public static boolean canVillagerBuyItem(ItemStack stack, VillagerEntity villager, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return false;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}
		if (profession.weaponPool != null && !profession.weaponPool.isEmpty()) {
			for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
				if (w.tag != null && stack.isIn(getTag(w.tag))) return true;
			}
		}
		if (profession.toolPool != null && !profession.toolPool.isEmpty()) {
			for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
				if (t.tag != null && stack.isIn(getTag(t.tag))) return true;
			}
		}
		if (profession.generalPool != null && !profession.generalPool.isEmpty()) {
			for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
				if (g.tag != null && stack.isIn(getTag(g.tag))) return true;
			}
		}
		if (profession.buyPool != null && !profession.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
				if (b.tag != null && stack.isIn(getTag(b.tag))) return true;
			}
		}
		return false;
	}

	/**
	 * Получает цену покупки предмета в бронзовых монетах.
	 */
	public static int getBuyPrice(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return 0;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					return Math.max(1, e.buy);
				}
			}
		}
		return 0;
	}

	/**
	 * Получает цену продажи предмета в бронзовых монетах.
	 */
	public static int getSellPrice(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return 0;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					int buyPrice = Math.max(1, e.buy);
					return Math.max(1, Math.min(e.sell, buyPrice - 1));
				}
			}
		}
		return 0;
	}

	private static TagKey<net.minecraft.item.Item> getTag(String tagString) {
		Identifier tagId = Identifier.tryParse(tagString);
		if (tagId == null) return null;
		return TagKey.of(RegistryKeys.ITEM, tagId);
	}
}
