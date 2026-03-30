package com.unnameduser.tradeoverhaul.common.component;

import net.minecraft.nbt.NbtCompound;

/**
 * Простой компонент валюты для жителей (не зависит от Numismatic Overhaul).
 * Хранит монеты трёх типов: золотые, серебряные, медные.
 */
public class VillagerCurrencyComponent {
	private int gold = 0;
	private int silver = 0;
	private int copper = 0;
	
	/**
	 * Получает общее количество монет в медном эквиваленте.
	 * 1 золотая = 100 серебряных = 10000 медных
	 */
	public int getTotalCopper() {
		return gold * 10000 + silver * 100 + copper;
	}
	
	/**
	 * Устанавливает общее количество монет (конвертирует в золотые/серебряные/медные).
	 */
	public void setTotalCopper(int total) {
		gold = total / 10000;
		int remaining = total % 10000;
		silver = remaining / 100;
		copper = remaining % 100;
	}
	
	/**
	 * Добавляет монеты.
	 */
	public void addMoney(int amount) {
		setTotalCopper(getTotalCopper() + amount);
	}
	
	/**
	 * Снимает монеты. Возвращает true если успешно.
	 */
	public boolean removeMoney(int amount) {
		if (getTotalCopper() < amount) {
			return false;
		}
		setTotalCopper(getTotalCopper() - amount);
		return true;
	}
	
	/**
	 * Проверяет, достаточно ли монет.
	 */
	public boolean hasEnough(int amount) {
		return getTotalCopper() >= amount;
	}
	
	public int getGold() {
		return gold;
	}
	
	public int getSilver() {
		return silver;
	}
	
	public int getCopper() {
		return copper;
	}
	
	/**
	 * Форматирует деньги в строку для отображения в GUI.
	 * Формат: "ЗМ: X СМ: Y ММ: Z"
	 */
	public String formatMoneyVertical() {
		StringBuilder sb = new StringBuilder();
		sb.append("§6ЗМ: §f").append(gold).append(" ");
		sb.append("§fСМ: §f").append(silver).append(" ");
		sb.append("§eММ: §f").append(copper);
		return sb.toString();
	}
	
	/**
	 * Форматирует деньги в читаемую строку с падежами.
	 */
	public String formatMoney() {
		StringBuilder sb = new StringBuilder();
		
		if (gold > 0) {
			sb.append(gold).append(" ");
			sb.append(getCurrencyName(gold, "золот", "золотая", "золотых"));
		}
		if (silver > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(silver).append(" ");
			sb.append(getCurrencyName(silver, "серебрян", "серебряная", "серебряных"));
		}
		if (copper > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(copper).append(" ");
			sb.append(getCurrencyName(copper, "медн", "медная", "медных"));
		}
		if (sb.length() == 0) {
			sb.append("0 монет");
		}
		return sb.toString();
	}
	
	/**
	 * Возвращает название валюты в правильном падеже.
	 */
	private String getCurrencyName(int amount, String base, String singular, String plural) {
		int lastDigit = amount % 10;
		int lastTwoDigits = amount % 100;
		
		if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
			return base + "ых";
		}
		
		if (lastDigit == 1) {
			return singular;
		}
		
		if (lastDigit >= 2 && lastDigit <= 4) {
			return base + "ые";
		}
		
		return plural;
	}
	
	/**
	 * Читает из NBT.
	 */
	public void readNbt(NbtCompound nbt) {
		gold = nbt.getInt("VillagerGold");
		silver = nbt.getInt("VillagerSilver");
		copper = nbt.getInt("VillagerCopper");
	}
	
	/**
	 * Записывает в NBT.
	 */
	public void writeNbt(NbtCompound nbt) {
		nbt.putInt("VillagerGold", gold);
		nbt.putInt("VillagerSilver", silver);
		nbt.putInt("VillagerCopper", copper);
	}
}
