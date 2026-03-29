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
	public Map<String, Double> itemPriceMultipliers;

	public static class StaticPoolEntry {
		public String item;
		public int buy;  // Цена покупки (в изумрудах)
		public int sell; // Цена продажи (в изумрудах)
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке (по умолчанию 1)
		public Integer sellQuantity; // Количество предметов за 1 изумруд при продаже (по умолчанию 1)
		public Integer minStock;
		public Integer maxStock;
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
	}

	public static class GeneralPoolEntry {
		public String tag;
		public Integer minStock;
		public Integer maxStock;
		public Integer buyPrice;
		public Integer sellPrice;
		public Integer buyQuantity;  // Количество предметов за 1 изумруд при покупке
		public Integer sellQuantity; // Количество предметов за 1 изумруд при продаже
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
