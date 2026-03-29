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
	 * Форматирует деньги в строку.
	 */
	public String formatMoney() {
		StringBuilder sb = new StringBuilder();
		if (gold > 0) {
			sb.append(gold).append(" золот");
			if (gold % 10 == 1 && gold % 100 != 11) sb.append("ая");
			else if (gold % 10 >= 2 && gold % 10 <= 4 && (gold % 100 < 10 || gold % 100 >= 20)) sb.append("ые");
			else sb.append("ых");
		}
		if (silver > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(silver).append(" серебрян");
			if (silver % 10 == 1 && silver % 100 != 11) sb.append("ая");
			else if (silver % 10 >= 2 && silver % 10 <= 4 && (silver % 100 < 10 || silver % 100 >= 20)) sb.append("ые");
			else sb.append("ых");
		}
		if (copper > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(copper).append(" медн");
			if (copper % 10 == 1 && copper % 100 != 11) sb.append("ая");
			else if (copper % 10 >= 2 && copper % 10 <= 4 && (copper % 100 < 10 || copper % 100 >= 20)) sb.append("ые");
			else sb.append("ых");
		}
		if (sb.length() == 0) {
			sb.append("0 монет");
		}
		return sb.toString();
	}
	
	/**
	 * Форматирует деньги в столбец.
	 */
	public String formatMoneyVertical() {
		StringBuilder sb = new StringBuilder();
		sb.append("§6Золотые: ").append(gold).append("\n");
		sb.append("§fСеребряные: ").append(silver).append("\n");
		sb.append("§7Медные: ").append(copper);
		return sb.toString();
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
