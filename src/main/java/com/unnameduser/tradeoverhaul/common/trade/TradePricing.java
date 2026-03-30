package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
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
		
		// Зачарованные книги можно продавать библиотекарю
		if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK && profession.enchantments != null && !profession.enchantments.isEmpty()) {
			return true;
		}
		
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
		
		// Обработка зачарованных книг
		if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
			if (profession.enchantments == null || profession.enchantments.isEmpty()) {
				TradeOverhaulMod.LOGGER.warn("Enchanted book but no enchantments in profession!");
				return 500;
			}
			return getEnchantedBookPrice(stack, profession);
		}

		Identifier id = Registries.ITEM.getId(stack.getItem());

		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					return Math.max(1, e.buy);
				}
			}
		}
		
		// Проверяем toolPool для мотыг с фиксированной ценой
		if (profession.toolPool != null && !profession.toolPool.isEmpty()) {
			for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
				if (t.tag != null && stack.isIn(getTag(t.tag))) {
					if (t.buyPrice != null && t.buyPrice > 0) {
						return t.buyPrice;
					}
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Рассчитывает цену зачарованной книги на основе зачарования.
	 * Формула: base_price × level
	 * Каждый уровень зачарования умножает базовую стоимость.
	 * Минимальная цена: 400 медных монет (4 серебряных).
	 */
	private static int getEnchantedBookPrice(ItemStack stack, ProfessionTradeFile profession) {
		if (profession.enchantments == null || profession.enchantments.isEmpty()) {
			TradeOverhaulMod.LOGGER.warn("Enchanted book but no enchantments in profession!");
			return 0;
		}

		// Читаем зачарование из NBT
		net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
		if (nbt == null) {
			TradeOverhaulMod.LOGGER.debug("Enchanted book has no NBT (using min price)!");
			return 400; // Минимальная цена (4 серебряных)
		}

		net.minecraft.nbt.NbtList enchantList = nbt.getList("StoredEnchantments", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
		if (enchantList.isEmpty()) {
			TradeOverhaulMod.LOGGER.debug("Enchanted book has no StoredEnchantments (using min price)!");
			return 400; // Минимальная цена (4 серебряных)
		}

		net.minecraft.nbt.NbtCompound enchantNbt = enchantList.getCompound(0);
		String enchantIdStr = enchantNbt.getString("id");
		int level = enchantNbt.getShort("lvl");

		// Ищем соответствующее зачарование в конфиге
		for (ProfessionTradeFile.EnchantmentEntry e : profession.enchantments) {
			if (e.enchantment != null) {
				// Проверяем точное совпадение
				if (e.enchantment.equals(enchantIdStr)) {
					int basePrice = e.base_price != null ? e.base_price : 400;
					int price = basePrice * level; // Каждый уровень умножает базовую стоимость
					return Math.max(400, price); // Минимум 4 серебряных (400 медных)
				}
				// Проверяем вариант без префикса (для случаев, когда NBT не содержит префикса)
				else if (enchantIdStr.startsWith("minecraft:") && e.enchantment.equals(enchantIdStr.substring(10))) {
					int basePrice = e.base_price != null ? e.base_price : 400;
					int price = basePrice * level; // Каждый уровень умножает базовую стоимость
					return Math.max(400, price); // Минимум 4 серебряных (400 медных)
				}
			}
		}

		// Если всё равно не нашли, возвращаем минимальную цену
		TradeOverhaulMod.LOGGER.debug("Enchantment {} not found in profession config (using min price)!", enchantIdStr);
		return 400; // 4 серебряных монеты
	}

	/**
	 * Получает цену продажи предмета в бронзовых монетах (сколько игрок получит при продаже жителю).
	 * Это цена, по которой житель ПОКУПАЕТ предмет у игрока (используется buy из конфига).
	 */
	public static int getSellPrice(ItemStack stack, ProfessionTradeFile profession) {
		if (stack.isEmpty() || profession == null) return 0;
		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Зачарованные книги можно продавать (50% от цены покупки, минимум 2 серебряных = 200 медных)
		if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK && profession.enchantments != null) {
			int buyPrice = getEnchantedBookPrice(stack, profession);
			return Math.max(200, buyPrice / 2); // Минимум 2 серебряных монеты
		}

		if (profession.staticPool != null && !profession.staticPool.isEmpty()) {
			for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					// Возвращаем buy цену, т.к. житель покупает у игрока
					return Math.max(1, e.buy);
				}
			}
		}
		
		// Проверяем toolPool для мотыг с фиксированной ценой
		if (profession.toolPool != null && !profession.toolPool.isEmpty()) {
			for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
				if (t.tag != null && stack.isIn(getTag(t.tag))) {
					if (t.buyPrice != null && t.buyPrice > 0) {
						return t.buyPrice;
					}
				}
			}
		}
		
		// Проверяем generalPool
		if (profession.generalPool != null && !profession.generalPool.isEmpty()) {
			for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
				if (g.tag != null && stack.isIn(getTag(g.tag))) {
					if (g.buyPrice != null && g.buyPrice > 0) {
						return g.buyPrice;
					}
				}
			}
		}
		
		// Проверяем buyPool
		if (profession.buyPool != null && !profession.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
				if (b.tag != null && stack.isIn(getTag(b.tag))) {
					if (b.buyPrice != null && b.buyPrice > 0) {
						return b.buyPrice;
					}
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
