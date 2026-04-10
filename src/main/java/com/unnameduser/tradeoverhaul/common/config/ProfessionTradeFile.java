package com.unnameduser.tradeoverhaul.common.config;

import java.util.List;
import java.util.Map;

public class ProfessionTradeFile {
	public String profession;
	public Integer offersCount;
	public List<BuyOnlyEntry> buyPool;
	public List<PlayerSellEntry> playerSellPool; // Предметы, которые игрок может продать жителю (не продаются жителем)
	public List<EnchantmentEntry> enchantments; // Зачарования для библиотекаря

	// Глобальные списки зачарований по типам (для случайных зачарований на предметах)
	// Поддерживает два формата: строка ("minecraft:sharpness") или объект {"id": "minecraft:sharpness", "max_level": 3}
	public List<EnchantmentSpec> weaponEnchantments;   // Зачарования для оружия
	public List<EnchantmentSpec> toolEnchantments;     // Зачарования для инструментов
	public List<EnchantmentSpec> armorEnchantments;    // Зачарования для брони (общие для всех слотов)
	
	// Специфичные зачарования для слотов брони (приоритетнее armorEnchantments)
	public List<EnchantmentSpec> helmetEnchantments;   // Зачарования для шлемов
	public List<EnchantmentSpec> chestplateEnchantments; // Зачарования для нагрудников
	public List<EnchantmentSpec> leggingsEnchantments; // Зачарования для понож
	public List<EnchantmentSpec> bootsEnchantments;    // Зачарования для ботинок
	
	// Зачарования для дистанционного оружия (луки и арбалеты)
	public List<EnchantmentSpec> bowEnchantments;      // Зачарования для луков
	public List<EnchantmentSpec> crossbowEnchantments; // Зачарования для арбалетов
	
	// Пулы торговли по уровням мастерства (1-5)
	public List<LevelPoolEntry> level1Pool; // Новичок
	public List<LevelPoolEntry> level2Pool; // Подмастерье
	public List<LevelPoolEntry> level3Pool; // Ремесленник
	public List<LevelPoolEntry> level4Pool; // Эксперт
	public List<LevelPoolEntry> level5Pool; // Мастер
	
	// Настройки денег по уровням
	public LevelMoneySettings levelMoneySettings;

	/**
	 * Запись для пула товаров определённого уровня
	 */
	public static class LevelPoolEntry {
		public String item;              // ID предмета (для staticPool)
		public String tag;               // ID тега (для generalPool)
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPrice;         // Цена покупки жителем
		public Integer sellPrice;        // Цена продажи жителем
		public Integer buy;              // Альтернативное название цены покупки
		public Integer sell;             // Альтернативное название цены продажи
		public String enchantment;       // ID зачарования (для книг)
		public Integer enchantment_level;// Уровень зачарования
		public Boolean useDurability;    // Использовать ли прочность
		public String durabilityFactor;
		public Integer buyPerDamage;
		public Integer sellPerDamage;
		public Integer buyBase;
		public Integer sellBase;
		public Integer buyPerEfficiency;
		public Integer sellPerEfficiency;
		public Boolean enchant;          // Если true — предмет получает случайные зачарования из глобального списка
		public Integer enchantMinLevel;  // Минимальный уровень зачарования (по умолчанию 1)
		public Integer enchantMaxLevel;  // Максимальный уровень зачарования (по умолчанию = уровень жителя)
		public Integer enchantChance;    // Шанс зачарования в процентах (по умолчанию 70)
		public Integer enchantMaxCount;  // Максимальное кол-во зачарований (по умолчанию 3)
		public Float xpMultiplier;       // Множитель XP за продажу этого предмета жителю
	}
	
	/**
	 * Настройки денег для каждого уровня жителя
	 */
	public static class LevelMoneySettings {
		public Integer level1StartMoney; // Стартовые деньги новичка (медные)
		public Integer level2StartMoney; // Стартовые деньги подмастерья
		public Integer level3StartMoney; // Стартовые деньги ремесленника
		public Integer level4StartMoney; // Стартовые деньги эксперта
		public Integer level5StartMoney; // Стартовые деньги мастера
		
		public Integer level1RestockMoney; // Деньги при рестокe новичка
		public Integer level2RestockMoney; // Деньги при рестокe подмастерья
		public Integer level3RestockMoney; // Деньги при рестокe ремесленника
		public Integer level4RestockMoney; // Деньги при рестокe эксперта
		public Integer level5RestockMoney; // Деньги при рестокe мастера
	}

	public static class EnchantmentEntry {
		public String enchantment;      // ID зачарования
		public Integer min_level;       // Минимальный уровень
		public Integer max_level;       // Максимальный уровень
		public Integer base_price;      // Базовая цена
		public Integer price_per_level; // Цена за уровень
	}

	/**
	 * Спецификация зачарования для оружия/инструментов/брони.
	 * Поддерживает формат JSON как строку, так и объект:
	 *   "minecraft:sharpness"                          — max_level берётся из реестра
	 *   {"id": "minecraft:sharpness", "max_level": 3}  — явный max_level
	 */
	public static class EnchantmentSpec {
		public String id;           // ID зачарования (обязательно)
		public Integer max_level;   // Максимальный уровень (null = берётся из реестра)

		/** Получить max_level, используя значение из реестра как fallback */
		public int getEffectiveMaxLevel(net.minecraft.enchantment.Enchantment enchant) {
			if (max_level != null && max_level > 0) {
				return max_level;
			}
			return enchant.getMaxLevel();
		}
	}

	public static class BuyOnlyEntry {
		public String tag;           // ID тега (для групп предметов)
		public String item;          // ID конкретного предмета
		public Integer buyPrice;
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPricePerDamage;
		public Integer buyPricePerEfficiency;
		public Integer buyBase;
		public Boolean useDurability;
		public Integer discountFactor;
	}

	/**
	 * Запись для пула предметов, которые игрок может продать жителю.
	 * Эти предметы НЕ продаются жителем и НЕ добавляются в инвентарь при рестокe.
	 * Используется только для определения цены продажи.
	 */
	public static class PlayerSellEntry {
		public String item;          // ID конкретного предмета (обязательно)
		public Integer buyPrice;     // Цена, которую житель заплатит игроку (в медных монетах)
	}

	/**
	 * Проверяет, есть ли предмет в пулах продажи жителя (level1Pool-level5Pool).
	 * @param itemId ID предмета
	 * @return true, если предмет продаётся жителем
	 */
	public boolean isItemSoldByVillager(String itemId) {
		List<List<LevelPoolEntry>> allPools = new java.util.ArrayList<>();
		if (level1Pool != null) allPools.add(level1Pool);
		if (level2Pool != null) allPools.add(level2Pool);
		if (level3Pool != null) allPools.add(level3Pool);
		if (level4Pool != null) allPools.add(level4Pool);
		if (level5Pool != null) allPools.add(level5Pool);

		for (List<LevelPoolEntry> pool : allPools) {
			for (LevelPoolEntry entry : pool) {
				if (entry.item != null && entry.item.equals(itemId)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Ищет множитель XP для предмета во всех пулах уровней.
	 * @param itemId ID предмета
	 * @return Множитель XP или null, если не найден
	 */
	public Float findXpMultiplierForItem(String itemId) {
		// Проверяем все пулы уровней
		List<List<LevelPoolEntry>> allPools = new java.util.ArrayList<>();
		if (level1Pool != null) allPools.add(level1Pool);
		if (level2Pool != null) allPools.add(level2Pool);
		if (level3Pool != null) allPools.add(level3Pool);
		if (level4Pool != null) allPools.add(level4Pool);
		if (level5Pool != null) allPools.add(level5Pool);

		for (List<LevelPoolEntry> pool : allPools) {
			for (LevelPoolEntry entry : pool) {
				if (entry.item != null && entry.item.equals(itemId)) {
					if (entry.xpMultiplier != null) {
						return entry.xpMultiplier;
					}
				}
			}
		}
		return null;
	}
}
