package com.unnameduser.tradeoverhaul.common.config;

import java.util.List;
import java.util.Map;

public class ProfessionTradeFile {
	public String profession;
	public Integer offersCount;
	public List<StaticPoolEntry> staticPool;
	public List<WeaponPoolEntry> weaponPool;
	public List<ToolPoolEntry> toolPool;
	public List<GeneralPoolEntry> generalPool;
	public List<BuyOnlyEntry> buyPool;
	public List<EnchantmentEntry> enchantments; // Зачарования для библиотекаря
	public Map<String, Double> itemPriceMultipliers;
	
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
		public Integer buyQuantity;      // Количество за 1 монету при покупке
		public Integer sellQuantity;     // Количество за 1 монету при продаже
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

	public static class StaticPoolEntry {
		public String item;
		public int buy;  // Цена покупки (в изумрудах)
		public int sell; // Цена продажи (в изумрудах)
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке (по умолчанию 1)
		public Integer sellQuantity; // Количество предметов за 1 изумруд при продаже (по умолчанию 1)
		public Integer minStock;
		public Integer maxStock;
		public String enchantment;        // ID зачарования (для зачарованных книг)
		public Integer enchantment_level; // Уровень зачарования
	}

	public static class WeaponPoolEntry {
		public String tag;
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPerDamage;
		public Integer sellPerDamage;
		public Integer buyBase;
		public Integer sellBase;
		public Boolean useDurability;
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке (по умолчанию 1)
		public Integer sellQuantity; // Количество предметов за 1 изумруд при продаже (по умолчанию 1)
	}

	public static class ToolPoolEntry {
		public String tag;
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPerEfficiency;
		public Integer sellPerEfficiency;
		public Integer buyBase;
		public Integer sellBase;
		public Boolean useDurability;
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке (по умолчанию 1)
		public Integer sellQuantity; // Количество предметов за 1 изумруд при продаже (по умолчанию 1)
		public Integer buyPrice;     // Фиксированная цена покупки (для мотыг)
		public Integer sellPrice;    // Фиксированная цена продажи (для мотыг)
	}

	public static class GeneralPoolEntry {
		public String tag;
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPrice;
		public Integer sellPrice;
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке
		public Integer sellQuantity; // Количество предметов за 1 монету при продаже
		public Boolean useDurability;
		public String durabilityFactor;
	}

	public static class BuyOnlyEntry {
		public String tag;
		public Integer buyPrice;
		public Integer buyQuantity;  // Количество предметов за 1 изумруд
		public Integer buyPricePerDamage;
		public Integer buyPricePerEfficiency;
		public Integer buyBase;
		public Boolean useDurability;
		public Integer discountFactor;
	}
}
