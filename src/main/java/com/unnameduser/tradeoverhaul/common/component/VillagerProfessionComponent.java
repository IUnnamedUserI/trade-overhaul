package com.unnameduser.tradeoverhaul.common.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для хранения данных о мастерстве жителя:
 * - Уровень (1-5)
 * - Опыт (для повышения уровня)
 * - Количество совершённых сделок
 * - Трекинг проданных предметов для diminishing returns
 */
public class VillagerProfessionComponent {
	// Уровни жителя (как в ваниле)
	public static final int NOVICE = 1;      // Новичок
	public static final int APPRENTICE = 2;  // Подмастерье
	public static final int JOURNEYMAN = 3;  // Ремесленник
	public static final int EXPERT = 4;      // Эксперт
	public static final int MASTER = 5;      // Мастер
	
	public static final int MAX_LEVEL = 5;
	
	// Опыт, необходимый для каждого уровня
	public static final int[] XP_REQUIRED = {
		0,      // Уровень 1 (новичок) - старт
		10,     // Уровень 2 (подмастерье) - 10 сделок
		30,     // Уровень 3 (ремесленник) - 30 сделок
		60,     // Уровень 4 (эксперт) - 60 сделок
		100     // Уровень 5 (мастер) - 100 сделок
	};
	
	private int level = NOVICE;
	private int experience = 0;
	private int tradesCompleted = 0;
	
	// Трекинг проданных предметов для diminishing returns
	// Ключ: itemId, Значение: сколько раз продано
	public Map<String, Integer> soldItemsTracker = new HashMap<>();
	
	// Накопитель дробного XP (для предметов с низким XP)
	private float fractionalXpAccumulator = 0f;
	
	public VillagerProfessionComponent() {
	}
	
	/**
	 * Получает текущий уровень жителя (1-5)
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Получает название уровня
	 */
	public String getLevelName() {
		return switch (level) {
			case APPRENTICE -> "apprentice";
			case JOURNEYMAN -> "journeyman";
			case EXPERT -> "expert";
			case MASTER -> "master";
			default -> "novice";
		};
	}
	
	/**
	 * Получает текущий опыт
	 */
	public int getExperience() {
		return experience;
	}
	
	/**
	 * Получает опыт, необходимый для следующего уровня
	 */
	public int getXpForNextLevel() {
		if (level >= MAX_LEVEL) return 0;
		// XP_REQUIRED = {0, 10, 30, 60, 100} для уровней 1-5
		// Для уровня 1 нужно 10 XP, для уровня 2 нужно 30 XP, и т.д.
		return XP_REQUIRED[level];
	}
	
	/**
	 * Получает прогресс до следующего уровня (0.0 - 1.0)
	 */
	public float getLevelProgress() {
		if (level >= MAX_LEVEL) return 1.0f;
		int required = XP_REQUIRED[level];
		if (required <= 0) return 1.0f;
		return Math.min(1.0f, (float) experience / required);
	}
	
	/**
	 * Проверяет, достиг ли житель максимального уровня
	 */
	public boolean isMaxLevel() {
		return level >= MAX_LEVEL;
	}
	
	/**
	 * Добавляет опыт за совершённую сделку
	 * @param amount Количество опыта (обычно 1 за сделку)
	 * @return true, если уровень повысился
	 */
	public boolean addExperience(int amount) {
		if (isMaxLevel()) return false;
		
		experience += amount;
		tradesCompleted++;
		
		// Проверяем повышение уровня
		int required = getXpForNextLevel();
		if (experience >= required) {
			levelUp();
			return true;
		}
		return false;
	}
	
	/**
	 * Добавляет опыт с плавающей точкой (для предметов с низким XP)
	 * Использует накопитель для дробного XP.
	 * @param amount Количество опыта (может быть дробным)
	 * @return true, если уровень повысился
	 */
	public boolean addExperienceFloat(float amount) {
		if (isMaxLevel()) return false;
		if (amount <= 0) return false;
		
		boolean leveledUp = false;
		
		// Добавляем XP к накопителю
		fractionalXpAccumulator += amount;
		
		// Если накопилось >= 1 XP, добавляем целый XP
		int xpToAdd = (int) fractionalXpAccumulator;
		if (xpToAdd > 0) {
			fractionalXpAccumulator -= xpToAdd;
			experience += xpToAdd;
			tradesCompleted++;
			
			// Проверяем и применяем повышение уровня (с переносом излишка)
			while (experience >= getXpForNextLevel() && !isMaxLevel()) {
				levelUp();
				leveledUp = true;
			}
		}
		
		return leveledUp;
	}
	
	/**
	 * Получает текущий накопленный дробный XP (для отладки)
	 */
	public float getFractionalXpAccumulator() {
		return fractionalXpAccumulator;
	}
	
	/**
	 * Повышает уровень жителя с переносом излишка опыта
	 */
	private void levelUp() {
		if (level < MAX_LEVEL) {
			int required = XP_REQUIRED[level];  // Опыт, требуемый для текущего уровня (до повышения)
			int excess = experience - required;  // Излишек опыта
			
			level++;
			experience = Math.max(0, excess);  // Сохраняем излишек для следующего уровня
			
			// Если после повышения всё ещё достаточно опыта для следующего уровня, повышаем ещё раз
			if (experience >= XP_REQUIRED[level] && level < MAX_LEVEL) {
				levelUp();  // Рекурсивное повышение
			}
		}
	}

	/**
	 * Получает количество совершённых сделок
	 */
	public int getTradesCompleted() {
		return tradesCompleted;
	}
	
	/**
	 * Получает, сколько раз данный предмет уже продавали
	 */
	public int getTimesSold(String itemId) {
		return soldItemsTracker.getOrDefault(itemId, 0);
	}
	
	/**
	 * Увеличивает счётчик продаж предмета
	 */
	public void incrementSoldCount(String itemId, int amount) {
		soldItemsTracker.put(itemId, getTimesSold(itemId) + amount);
	}

	/**
	 * Сбрасывает счётчик продаж предмета (при рестокe)
	 */
	public void resetSoldCount(String itemId) {
		soldItemsTracker.remove(itemId);
	}

	/**
	 * Применяет опыт и обновляет трекинг предметов
	 * @param itemId ID предмета
	 * @param amount Количество предметов
	 * @return true, если уровень повысился
	 */
	public boolean applyXpFromSale(String itemId, int amount) {
		// Рассчитываем XP: multiplier × amount
		// multiplier уже содержит XP за 1 предмет (семена: 0.01, пшеница: 0.05, книги: 1.0)
		float multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
		float xp = multiplier * amount;
		
		// Обновляем трекинг
		incrementSoldCount(itemId, amount);
		
		// Добавляем XP
		return addExperienceFloat(xp);
	}
	
	/**
	 * Читывает данные из NBT
	 */
	public void readNbt(NbtCompound nbt) {
		if (nbt.contains("ProfessionLevel")) {
			level = nbt.getInt("ProfessionLevel");
		}
		if (nbt.contains("ProfessionExperience")) {
			experience = nbt.getInt("ProfessionExperience");
		}
		if (nbt.contains("TradesCompleted")) {
			tradesCompleted = nbt.getInt("TradesCompleted");
		}
		if (nbt.contains("FractionalXpAccumulator")) {
			fractionalXpAccumulator = nbt.getFloat("FractionalXpAccumulator");
		}
		
		// Читаем трекинг проданных предметов
		if (nbt.contains("SoldItemsTracker")) {
			soldItemsTracker.clear();
			NbtList trackerList = nbt.getList("SoldItemsTracker", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < trackerList.size(); i++) {
				NbtCompound itemNbt = trackerList.getCompound(i);
				String itemId = itemNbt.getString("itemId");
				int count = itemNbt.getInt("count");
				soldItemsTracker.put(itemId, count);
			}
		}
	}
	
	/**
	 * Записывает данные в NBT
	 */
	public void writeNbt(NbtCompound nbt) {
		nbt.putInt("ProfessionLevel", level);
		nbt.putInt("ProfessionExperience", experience);
		nbt.putInt("TradesCompleted", tradesCompleted);
		nbt.putFloat("FractionalXpAccumulator", fractionalXpAccumulator);
		
		// Записываем трекинг проданных предметов
		NbtList trackerList = new NbtList();
		for (Map.Entry<String, Integer> entry : soldItemsTracker.entrySet()) {
			NbtCompound itemNbt = new NbtCompound();
			itemNbt.putString("itemId", entry.getKey());
			itemNbt.putInt("count", entry.getValue());
			trackerList.add(itemNbt);
		}
		nbt.put("SoldItemsTracker", trackerList);
	}
	
	/**
	 * Устанавливает уровень (для синхронизации с ванильным уровнем)
	 */
	public void setLevel(int level) {
		this.level = Math.max(1, Math.min(MAX_LEVEL, level));
		this.experience = 0;
	}
}
