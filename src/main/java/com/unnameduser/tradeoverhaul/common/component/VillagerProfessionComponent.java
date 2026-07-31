package com.unnameduser.tradeoverhaul.common.component;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;

import java.util.HashMap;
import java.util.Map;

public class VillagerProfessionComponent {
	public static final int NOVICE = 1;
	public static final int APPRENTICE = 2;
	public static final int JOURNEYMAN = 3;
	public static final int EXPERT = 4;
	public static final int MASTER = 5;

	public static final int MAX_LEVEL = 5;

	public static final int[] XP_REQUIRED = {0, 10, 30, 60, 100};

	private int level = NOVICE;
	private int experience = 0;
	private int tradesCompleted = 0;

	// Старый трекер (можно удалить позже, если он больше нигде не используется, но пока оставим для совместимости)
	public Map<String, Integer> soldItemsTracker = new HashMap<>();

	// Новые трекеры для защиты от дюпа опыта без тегов
	public Map<String, Integer> boughtTracker = new HashMap<>(); // Что игрок купил у жителя
	public Map<String, Integer> soldTracker = new HashMap<>();   // Что игрок продал жителю

	private float fractionalXpAccumulator = 0f;
	private boolean hasEverTraded = false;
	private boolean workstationLost = false;

	public Map<String, Float> damageReputation = new HashMap<>();

	public VillagerProfessionComponent() {
	}

	public int getLevel() { return level; }

	public String getLevelName() {
		return switch (level) {
			case APPRENTICE -> "apprentice";
			case JOURNEYMAN -> "journeyman";
			case EXPERT -> "expert";
			case MASTER -> "master";
			default -> "novice";
		};
	}

	public int getExperience() { return experience; }

	public int getXpForNextLevel() {
		if (level >= MAX_LEVEL) return 0;
		return XP_REQUIRED[level];
	}

	public float getLevelProgress() {
		if (level >= MAX_LEVEL) return 1.0f;
		int required = XP_REQUIRED[level];
		if (required <= 0) return 1.0f;
		return Math.min(1.0f, (float) experience / required);
	}

	public boolean isMaxLevel() { return level >= MAX_LEVEL; }

	public boolean addExperience(int amount) {
		if (isMaxLevel()) return false;
		experience += amount;
		tradesCompleted++;
		int required = getXpForNextLevel();
		if (experience >= required) {
			levelUp();
			return true;
		}
		return false;
	}

	public boolean addExperienceFloat(float amount) {
		if (isMaxLevel()) return false;
		if (amount <= 0) return false;

		boolean leveledUp = false;
		fractionalXpAccumulator += amount;
		int xpToAdd = (int) fractionalXpAccumulator;

		if (xpToAdd > 0) {
			fractionalXpAccumulator -= xpToAdd;
			experience += xpToAdd;
			tradesCompleted++;
			while (experience >= getXpForNextLevel() && !isMaxLevel()) {
				levelUp();
				leveledUp = true;
			}
		}
		return leveledUp;
	}

	public void markAsTraded() { this.hasEverTraded = true; }
	public boolean hasEverTraded() { return hasEverTraded; }
	public void setWorkstationLost(boolean lost) { this.workstationLost = lost; }
	public boolean hasWorkstationLost() { return workstationLost; }

	public boolean shouldLoseProfession() {
		return workstationLost && hasEverTraded && level <= NOVICE && experience <= 0;
	}

	public void resetProfession() {
		level = NOVICE;
		experience = 0;
		tradesCompleted = 0;
		hasEverTraded = false;
		workstationLost = false;
		fractionalXpAccumulator = 0f;
		soldItemsTracker.clear();
		boughtTracker.clear();
		soldTracker.clear();
		damageReputation.clear();
	}

	// Метод для очистки трекеров при рестокe
	public void resetTradeTrackers() {
		boughtTracker.clear();
		soldTracker.clear();
		soldItemsTracker.clear(); // Очищаем и старый трекер
	}

	public void addDamageReputation(String playerId, float damageAmount) {
		if (damageAmount <= 0) return;
		damageReputation.merge(playerId, damageAmount, Float::sum);
	}

	public float getDamageReputation(String playerId) {
		return damageReputation.getOrDefault(playerId, 0f);
	}

	public double getDamageReputationPercent(String playerId, com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings settings) {
		float totalDamage = getDamageReputation(playerId);
		if (totalDamage <= 0) return 0.0;
		double percent = totalDamage * settings.damageReputationPercentPerHP;
		return Math.min(percent, settings.damageReputationMaxPercent);
	}

	public void resetDamageReputation() { damageReputation.clear(); }

	public void applyWorkstationDecay() {
		if (!workstationLost || !hasEverTraded) return;
		if (level <= NOVICE && experience <= 0) {
			experience = 0;
			return;
		}
		int xpRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];
		if (experience >= xpRequired) {
			level--;
			if (level <= NOVICE) { experience = 0; return; }
			int newRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];
			experience = (int) Math.floor(newRequired * 0.75f);
		} else {
			int decay = (int) Math.ceil(xpRequired * 0.25f);
			if (decay <= 0) decay = 1;
			experience -= decay;
			while (experience < 0 && level > NOVICE) {
				level--;
				if (level <= NOVICE) { experience = 0; break; }
				int newRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];
				experience = (int) Math.floor(newRequired * 0.75f);
			}
		}
	}

	public float getFractionalXpAccumulator() { return fractionalXpAccumulator; }

	private void levelUp() {
		if (level < MAX_LEVEL) {
			int required = XP_REQUIRED[level];
			int excess = experience - required;
			level++;
			experience = Math.max(0, excess);
			if (experience >= XP_REQUIRED[level] && level < MAX_LEVEL) {
				levelUp();
			}
		}
	}

	public int getTradesCompleted() { return tradesCompleted; }

	public int getTimesSold(String itemId) { return soldItemsTracker.getOrDefault(itemId, 0); }

	public void incrementSoldCount(String itemId, int amount) {
		soldItemsTracker.put(itemId, getTimesSold(itemId) + amount);
	}

	public void resetSoldCount(String itemId) { soldItemsTracker.remove(itemId); }

	/**
	 * Главная логика расчета опыта без тегов.
	 * Возвращает количество предметов, за которые НУЖНО дать опыт.
	 */
	public int updateTrackerAndGetXpAmount(Map<String, Integer> tracker, String itemId, int amount) {
		int current = tracker.getOrDefault(itemId, 0);
		if (current >= amount) {
			tracker.put(itemId, current - amount);
			if (tracker.get(itemId) == 0) tracker.remove(itemId);
			return 0; // Опыт не даем
		} else {
			int remaining = amount - current;
			tracker.remove(itemId);
			return remaining; // Возвращаем количество для начисления опыта
		}
	}

	public boolean applyXpFromSale(String itemId, int amount, com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile professionFile) {
		float multiplier = 1.0f;
		if (professionFile != null) {
			Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
			if (poolMultiplier != null) {
				multiplier = poolMultiplier;
			} else {
				multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
			}
		} else {
			multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
		}
		float xp = multiplier * amount;
		return addExperienceFloat(xp);
	}

	public void readNbt(NbtCompound nbt) {
		if (nbt.contains("ProfessionLevel")) level = nbt.getInt("ProfessionLevel");
		if (nbt.contains("ProfessionExperience")) experience = nbt.getInt("ProfessionExperience");
		if (nbt.contains("TradesCompleted")) tradesCompleted = nbt.getInt("TradesCompleted");
		if (nbt.contains("FractionalXpAccumulator")) fractionalXpAccumulator = nbt.getFloat("FractionalXpAccumulator");
		if (nbt.contains("HasEverTraded")) hasEverTraded = nbt.getBoolean("HasEverTraded");
		if (nbt.contains("WorkstationLost")) workstationLost = nbt.getBoolean("WorkstationLost");

		if (nbt.contains("SoldItemsTracker")) {
			soldItemsTracker.clear();
			NbtList trackerList = nbt.getList("SoldItemsTracker", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < trackerList.size(); i++) {
				NbtCompound itemNbt = trackerList.getCompound(i);
				soldItemsTracker.put(itemNbt.getString("itemId"), itemNbt.getInt("count"));
			}
		}

		// Загрузка новых трекеров
		if (nbt.contains("BoughtTracker")) {
			boughtTracker.clear();
			NbtCompound boughtNbt = nbt.getCompound("BoughtTracker");
			for (String key : boughtNbt.getKeys()) {
				boughtTracker.put(key, boughtNbt.getInt(key));
			}
		}
		if (nbt.contains("SoldTracker")) {
			soldTracker.clear();
			NbtCompound soldNbt = nbt.getCompound("SoldTracker");
			for (String key : soldNbt.getKeys()) {
				soldTracker.put(key, soldNbt.getInt(key));
			}
		}

		if (nbt.contains("DamageReputation")) {
			damageReputation.clear();
			NbtList damageList = nbt.getList("DamageReputation", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < damageList.size(); i++) {
				NbtCompound entryNbt = damageList.getCompound(i);
				damageReputation.put(entryNbt.getString("playerId"), entryNbt.getFloat("damage"));
			}
		}
	}

	public void writeNbt(NbtCompound nbt) {
		nbt.putInt("ProfessionLevel", level);
		nbt.putInt("ProfessionExperience", experience);
		nbt.putInt("TradesCompleted", tradesCompleted);
		nbt.putFloat("FractionalXpAccumulator", fractionalXpAccumulator);
		nbt.putBoolean("HasEverTraded", hasEverTraded);
		nbt.putBoolean("WorkstationLost", workstationLost);

		NbtList trackerList = new NbtList();
		for (Map.Entry<String, Integer> entry : soldItemsTracker.entrySet()) {
			NbtCompound itemNbt = new NbtCompound();
			itemNbt.putString("itemId", entry.getKey());
			itemNbt.putInt("count", entry.getValue());
			trackerList.add(itemNbt);
		}
		nbt.put("SoldItemsTracker", trackerList);

		// Сохранение новых трекеров
		NbtCompound boughtNbt = new NbtCompound();
		for (Map.Entry<String, Integer> entry : boughtTracker.entrySet()) {
			boughtNbt.putInt(entry.getKey(), entry.getValue());
		}
		nbt.put("BoughtTracker", boughtNbt);

		NbtCompound soldNbt = new NbtCompound();
		for (Map.Entry<String, Integer> entry : soldTracker.entrySet()) {
			soldNbt.putInt(entry.getKey(), entry.getValue());
		}
		nbt.put("SoldTracker", soldNbt);

		NbtList damageList = new NbtList();
		for (Map.Entry<String, Float> entry : damageReputation.entrySet()) {
			NbtCompound entryNbt = new NbtCompound();
			entryNbt.putString("playerId", entry.getKey());
			entryNbt.putFloat("damage", entry.getValue());
			damageList.add(entryNbt);
		}
		nbt.put("DamageReputation", damageList);
	}

	public void setLevel(int level) {
		int oldLevel = this.level;
		this.level = Math.max(1, Math.min(MAX_LEVEL, level));
		if (this.level < oldLevel) this.experience = 0;
	}
}