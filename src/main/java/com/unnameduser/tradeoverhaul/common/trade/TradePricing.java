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

import java.util.List;
import java.util.Map;

public final class TradePricing {
	private TradePricing() {}

	/**
	 * Применяет модификатор цены на основе репутации урона игрока.
	 * @param basePrice Базовая цена
	 * @param playerId UUID игрока
	 * @param profession Profession-компонент жителя
	 * @param settings Настройки мода
	 * @return Цена с учётом штрафа за урон
	 */
	public static int applyDamageReputation(int basePrice, String playerId,
			com.unnameduser.tradeoverhaul.common.component.VillagerProfessionComponent profession,
			TradeOverhaulSettings settings) {
		double repPercent = profession.getDamageReputationPercent(playerId, settings);
		TradeOverhaulMod.LOGGER.info("applyDamageReputation: basePrice={}, playerId={}, repPercent={}, totalDamage={}", 
			basePrice, playerId, repPercent, profession.getDamageReputation(playerId));
		if (repPercent <= 0.0) return basePrice;
		double multiplier = 1.0 + (repPercent / 100.0);
		int newPrice = (int) Math.ceil(basePrice * multiplier);
		TradeOverhaulMod.LOGGER.info("applyDamageReputation: newPrice={}", newPrice);
		return newPrice;
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

		// Level pools (проверяем все уровни)
		if (profession.level1Pool != null) {
			for (ProfessionTradeFile.LevelPoolEntry e : profession.level1Pool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}
		if (profession.level2Pool != null) {
			for (ProfessionTradeFile.LevelPoolEntry e : profession.level2Pool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}
		if (profession.level3Pool != null) {
			for (ProfessionTradeFile.LevelPoolEntry e : profession.level3Pool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}
		if (profession.level4Pool != null) {
			for (ProfessionTradeFile.LevelPoolEntry e : profession.level4Pool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}
		if (profession.level5Pool != null) {
			for (ProfessionTradeFile.LevelPoolEntry e : profession.level5Pool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) return true;
			}
		}

		// Buy pool
		if (profession.buyPool != null && !profession.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
				if (b.tag != null) {
					boolean isInTag = stack.isIn(getTag(b.tag));
					TradeOverhaulMod.LOGGER.info("canVillagerBuyItem: buyPool tag={}, item={}, isInTag={}", b.tag, Registries.ITEM.getId(stack.getItem()), isInTag);
					if (isInTag) return true;
				}
				// Проверяем конкретный предмет
				if (b.item != null && id.equals(Identifier.tryParse(b.item))) {
					TradeOverhaulMod.LOGGER.info("canVillagerBuyItem: buyPool item={} matched!", b.item);
					return true;
				}
			}
		}
		// Player sell pool - предметы, которые игрок может продать жителю
		if (profession.playerSellPool != null && !profession.playerSellPool.isEmpty()) {
			for (ProfessionTradeFile.PlayerSellEntry e : profession.playerSellPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					TradeOverhaulMod.LOGGER.info("canVillagerBuyItem: playerSellPool item={} matched!", e.item);
					return true;
				}
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

		// Если предмет был продан игроком (тег PlayerSold), цена выкупа = sellPrice * 2
		if (com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isPlayerSold(stack)) {
			int sellPrice = getSellPrice(stack, profession);
			if (sellPrice > 0) {
				int buyPrice = sellPrice * 2;
				TradeOverhaulMod.LOGGER.info("getBuyPrice: PlayerSold item={}, sellPrice={}, buyPrice={}",
					Registries.ITEM.getId(stack.getItem()), sellPrice, buyPrice);
				return buyPrice;
			}
		}

		Identifier id = Registries.ITEM.getId(stack.getItem());

		// Level pools (проверяем все уровни)
		// Сначала ищем запись, точно соответствующую предмету (с зачарованием или без)
		if (profession.level1Pool != null) {
			Integer price = findPriceInPool(profession.level1Pool, stack, id, true);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getBuyPrice level1Pool: item={}, price={}", id, price);
				return price;
			}
		}
		if (profession.level2Pool != null) {
			Integer price = findPriceInPool(profession.level2Pool, stack, id, true);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getBuyPrice level2Pool: item={}, price={}", id, price);
				return price;
			}
		}
		if (profession.level3Pool != null) {
			Integer price = findPriceInPool(profession.level3Pool, stack, id, true);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getBuyPrice level3Pool: item={}, price={}", id, price);
				return price;
			}
		}
		if (profession.level4Pool != null) {
			Integer price = findPriceInPool(profession.level4Pool, stack, id, true);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getBuyPrice level4Pool: item={}, price={}", id, price);
				return price;
			}
		}
		if (profession.level5Pool != null) {
			Integer price = findPriceInPool(profession.level5Pool, stack, id, true);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getBuyPrice level5Pool: item={}, price={}", id, price);
				return price;
			}
		}
		
		TradeOverhaulMod.LOGGER.warn("getBuyPrice: no price found for item={} in profession={}", id, profession.profession);

		// Проверяем buyPool - предметы, которые житель хочет купить у игрока
		if (profession.buyPool != null && !profession.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
				if (b.tag != null) {
					boolean isInTag = stack.isIn(getTag(b.tag));
					TradeOverhaulMod.LOGGER.info("getSellPrice: buyPool tag={}, item={}, isInTag={}, buyPrice={}", b.tag, Registries.ITEM.getId(stack.getItem()), isInTag, b.buyPrice);
					if (isInTag) {
						int price = Math.max(1, b.buyPrice != null ? b.buyPrice : 0);
						TradeOverhaulMod.LOGGER.info("getSellPrice: returning buyPrice={}", price);
						return price;
					}
				}
				// Проверяем конкретный предмет
				if (b.item != null && id.equals(Identifier.tryParse(b.item))) {
					int price = Math.max(1, b.buyPrice != null ? b.buyPrice : 0);
					TradeOverhaulMod.LOGGER.info("getSellPrice: buyPool item={} matched, returning buyPrice={}", b.item, price);
					return price;
				}
			}
		}

		return 0;
	}

	/**
	 * Применяет модификатор цены на основе зачарований предмета.
	 * Формула: basePrice × (1 + 0.25 × numEnchants + 0.15 × totalEnchantLevels)
	 * - +25% за каждое зачарование
	 * - +15% за каждый уровень зачарования
	 */
	private static int applyEnchantmentPriceModifier(int basePrice, ItemStack stack, ProfessionTradeFile profession) {
		if (!stack.hasEnchantments()) return basePrice;

		net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
		if (nbt == null) return basePrice;

		net.minecraft.nbt.NbtList enchantList = nbt.getList("Enchantments", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
		if (enchantList.isEmpty()) return basePrice;

		int numEnchants = enchantList.size();
		int totalLevels = 0;

		for (int i = 0; i < enchantList.size(); i++) {
			net.minecraft.nbt.NbtCompound enchantNbt = enchantList.getCompound(i);
			totalLevels += enchantNbt.getShort("lvl");
		}

		// Формула: basePrice × (1 + 0.25 × numEnchants + 0.15 × totalEnchantLevels)
		double modifier = 1.0 + (0.25 * numEnchants) + (0.15 * totalLevels);
		int finalPrice = (int) Math.round(basePrice * modifier);

		TradeOverhaulMod.LOGGER.debug("applyEnchantmentPriceModifier: base={}, numEnchants={}, totalLevels={}, modifier={}, final={}",
			basePrice, numEnchants, totalLevels, modifier, finalPrice);

		return Math.max(1, finalPrice);
	}

	/**
	 * Ищет цену в пуле, учитывая зачарования предмета.
	 * @param useBuyPrice если true - использует buy цену, если false - sell цену
	 */
	private static Integer findPriceInPool(List<ProfessionTradeFile.LevelPoolEntry> pool, ItemStack stack, Identifier itemId, boolean useBuyPrice) {
		ProfessionTradeFile.LevelPoolEntry fallbackEntry = null;
		
		for (ProfessionTradeFile.LevelPoolEntry e : pool) {
			if (e.item != null && itemId.equals(Identifier.tryParse(e.item))) {
				// Если у записи нет зачарования - это fallback вариант
				if (e.enchantment == null) {
					fallbackEntry = e;
					continue;
				}
				
				// Если у записи есть зачарование, проверяем совпадение
				if (stack.hasEnchantments()) {
					// Получаем зачарование из предмета
					net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
					if (nbt != null) {
						net.minecraft.nbt.NbtList enchantList = nbt.getList("Enchantments", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
						if (!enchantList.isEmpty()) {
							net.minecraft.nbt.NbtCompound firstEnchant = enchantList.getCompound(0);
							String itemEnchantId = firstEnchant.getString("id");
							
							// Проверяем совпадение зачарования
							if (e.enchantment.equals(itemEnchantId)) {
								// Точное совпадение - используем цену из entry
								int price = useBuyPrice 
									? (e.buy != null ? e.buy : e.buyPrice != null ? e.buyPrice : 0)
									: (e.sell != null ? e.sell : e.sellPrice != null ? e.sellPrice : 0);
								return Math.max(1, price);
							}
						}
					}
				}
			}
		}
		
		// Не нашли точного совпадения - используем fallback (запись без зачарования)
		if (fallbackEntry != null) {
			int price = useBuyPrice 
				? (fallbackEntry.buy != null ? fallbackEntry.buy : fallbackEntry.buyPrice != null ? fallbackEntry.buyPrice : 0)
				: (fallbackEntry.sell != null ? fallbackEntry.sell : fallbackEntry.sellPrice != null ? fallbackEntry.sellPrice : 0);
			return Math.max(1, price);
		}
		
		return null;
	}

	/**
	 * Рассчитывает цену зачарованного предмета (меч, инструмент и т.д.)
	 * Формула: base_price + (enchantment_level * bonus_per_level)
	 * Если в entry есть фиксированная цена, используется она + бонус за зачарование.
	 */
	private static int getEnchantedItemPrice(ItemStack stack, ProfessionTradeFile.LevelPoolEntry entry) {
		int basePrice = entry.buy != null ? entry.buy : (entry.buyPrice != null ? entry.buyPrice : 0);
		
		// Получаем уровень зачарования из NBT предмета
		int enchantLevel = getEnchantmentLevelFromItem(stack);
		
		// Бонус за зачарование: 20% от базовой цены за уровень
		int enchantBonus = (int) (basePrice * 0.2 * enchantLevel);
		
		int finalPrice = basePrice + enchantBonus;
		return Math.max(1, finalPrice);
	}

	/**
	 * Получает уровень зачарования из NBT предмета
	 */
	private static int getEnchantmentLevelFromItem(ItemStack stack) {
		net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
		if (nbt == null) return 1;
		
		net.minecraft.nbt.NbtList enchantList = nbt.getList("Enchantments", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
		if (enchantList.isEmpty()) {
			// Проверяем альтернативный формат
			enchantList = nbt.getList("StoredEnchantments", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
		}
		
		if (enchantList.isEmpty()) return 1;
		
		// Суммируем уровни всех зачарований для простоты
		int totalLevel = 0;
		for (int i = 0; i < enchantList.size(); i++) {
			net.minecraft.nbt.NbtCompound enchantNbt = enchantList.getCompound(i);
			totalLevel += enchantNbt.getShort("lvl");
		}
		
		return Math.max(1, totalLevel);
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
		
		// Отладка для бронника
		if (profession.profession != null && profession.profession.equals("minecraft:armourer")) {
			TradeOverhaulMod.LOGGER.info("getSellPrice called for armourer: item={}, level1Pool={}, level2Pool={}", 
				id, 
				profession.level1Pool != null ? profession.level1Pool.size() : "null",
				profession.level2Pool != null ? profession.level2Pool.size() : "null");
		}

		// Зачарованные книги можно продавать (50% от цены покупки, минимум 2 серебряных = 200 медных)
		if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK && profession.enchantments != null) {
			int buyPrice = getEnchantedBookPrice(stack, profession);
			return Math.max(200, buyPrice / 2); // Минимум 2 серебряных монеты
		}

		// Level pools (проверяем все уровни)
		if (profession.level1Pool != null) {
			Integer price = findPriceInPool(profession.level1Pool, stack, id, false);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				TradeOverhaulMod.LOGGER.debug("getSellPrice found in level1Pool: {} -> sell={}", id, price);
				return price;
			}
		}
		if (profession.level2Pool != null) {
			Integer price = findPriceInPool(profession.level2Pool, stack, id, false);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				return price;
			}
		}
		if (profession.level3Pool != null) {
			Integer price = findPriceInPool(profession.level3Pool, stack, id, false);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				return price;
			}
		}
		if (profession.level4Pool != null) {
			Integer price = findPriceInPool(profession.level4Pool, stack, id, false);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				return price;
			}
		}
		if (profession.level5Pool != null) {
			Integer price = findPriceInPool(profession.level5Pool, stack, id, false);
			if (price != null) {
				price = applyEnchantmentPriceModifier(price, stack, profession);
				return price;
			}
		}

		// Проверяем buyPool - предметы, которые житель хочет купить у игрока
		if (profession.buyPool != null && !profession.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : profession.buyPool) {
				if (b.tag != null && stack.isIn(getTag(b.tag))) {
					return Math.max(1, b.buyPrice != null ? b.buyPrice : 0);
				}
				// Проверяем конкретный предмет
				if (b.item != null && id.equals(Identifier.tryParse(b.item))) {
					return Math.max(1, b.buyPrice != null ? b.buyPrice : 0);
				}
			}
		}

		// Проверяем playerSellPool - предметы, которые игрок может продать жителю
		if (profession.playerSellPool != null && !profession.playerSellPool.isEmpty()) {
			for (ProfessionTradeFile.PlayerSellEntry e : profession.playerSellPool) {
				if (e.item != null && id.equals(Identifier.tryParse(e.item))) {
					return Math.max(1, e.buyPrice != null ? e.buyPrice : 0);
				}
			}
		}

		return 0;
	}

	/**
	 * Применяет модификатор прочности к цене предмета.
	 * Для неповреждённых предметов возвращает базовую цену.
	 * Для повреждённых предметов снижает цену пропорционально потере прочности.
	 * 
	 * @param basePrice Базовая цена предмета
	 * @param stack Предмет для проверки прочности
	 * @param settings Настройки мода
	 * @return Цена с учётом прочности
	 */
	public static int applyDurabilityPriceModifier(int basePrice, ItemStack stack, TradeOverhaulSettings settings) {
		if (basePrice <= 0) return 0;
		return ItemDurabilityPricing.applyDurabilityModifier(basePrice, stack, settings);
	}

	private static TagKey<net.minecraft.item.Item> getTag(String tagString) {
		Identifier tagId = Identifier.tryParse(tagString);
		if (tagId == null) return null;
		return TagKey.of(RegistryKeys.ITEM, tagId);
	}
}
