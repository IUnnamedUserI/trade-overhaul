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
		return XP_REQUIRED[level];
	}
	
	/**
	 * Получает прогресс до следующего уровня (0.0 - 1.0)
	 */
	public float getLevelProgress() {
		if (level >= MAX_LEVEL) return 1.0f;
		int required = getXpForNextLevel();
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
	 * Добавляет опыт с плавающей точкой (для diminishing returns)
	 * @param amount Количество опыта (может быть дробным)
	 * @return true, если уровень повысился
	 */
	public boolean addExperienceFloat(float amount) {
		if (isMaxLevel()) return false;
		
		// Округляем до целого, но минимум 1 если amount > 0
		int xpToAdd = amount > 0 && amount < 1 ? 1 : (int) Math.floor(amount);
		experience += xpToAdd;
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
	 * Повышает уровень жителя
	 */
	private void levelUp() {
		if (level < MAX_LEVEL) {
			level++;
			experience = 0; // Сбрасываем опыт после повышения
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
	 * Рассчитывает XP для предмета с учётом diminishing returns
	 */
	public float calculateXpForItem(String itemId, int amount) {
		float totalXp = 0f;
		int timesSold = getTimesSold(itemId);
		
		for (int i = 0; i < amount; i++) {
			float xp = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.calculateXpWithDiminishingReturns(1.0f, itemId, timesSold + i);
			totalXp += xp;
		}
		
		return totalXp;
	}
	
	/**
	 * Рассчитывает ожидаемый XP для предмета (без применения)
	 * Используется для превью в GUI
	 */
	public float calculateExpectedXp(String itemId, int amount) {
		return calculateXpForItem(itemId, amount);
	}
	
	/**
	 * Применяет опыт и обновляет трекинг предметов
	 * @param itemId ID предмета
	 * @param amount Количество предметов
	 * @return true, если уровень повысился
	 */
	public boolean applyXpFromSale(String itemId, int amount) {
		float xp = calculateXpForItem(itemId, amount);
		incrementSoldCount(itemId, amount);
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
	 * Устанавливает уровень (для команд / командной строки)
	 */
	public void setLevel(int level) {
		this.level = Math.max(1, Math.min(MAX_LEVEL, level));
		this.experience = 0;
	}
}
