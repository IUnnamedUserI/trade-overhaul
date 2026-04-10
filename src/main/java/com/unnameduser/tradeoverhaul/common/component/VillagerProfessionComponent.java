package com.unnameduser.tradeoverhaul.common.component;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
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

	// Флаг: торговал ли когда-либо игрок с этим жителем
	private boolean hasEverTraded = false;

	// Флаг: потерял ли житель свой рабочий блок (но остался с профессией т.к. торговали)
	private boolean workstationLost = false;

	// Трекинг урона, нанесённого игроками (UUID -> суммарный урон в полсердцах)
	// Ключ: строковое представление UUID игрока
	// Значение: накопленный урон (1 единица = 1 полсердца = 1 ХП)
	public Map<String, Float> damageReputation = new HashMap<>();
	
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
	 * Отмечает, что с жителем была совершена сделка.
	 * После этого он не потеряет профессию при сломанном блоке.
	 */
	public void markAsTraded() {
		this.hasEverTraded = true;
	}

	/**
	 * Проверяет, торговал ли когда-либо игрок с этим жителем.
	 */
	public boolean hasEverTraded() {
		return hasEverTraded;
	}

	/**
	 * Устанавливает флаг потери рабочего блока.
	 */
	public void setWorkstationLost(boolean lost) {
		this.workstationLost = lost;
	}

	/**
	 * Проверяет, потерял ли житель рабочий блок.
	 */
	public boolean hasWorkstationLost() {
		return workstationLost;
	}

	/**
	 * Проверяет, должен ли житель полностью потерять профессию
	 * (уровень 1 и опыт 0 при потерянном рабочем блоке).
	 */
	public boolean shouldLoseProfession() {
		return workstationLost && hasEverTraded && level <= NOVICE && experience <= 0;
	}

	/**
	 * Полностью сбрасывает компонент профессии.
	 * Вызывается, когда житель теряет профессию.
	 */
	public void resetProfession() {
		level = NOVICE;
		experience = 0;
		tradesCompleted = 0;
		hasEverTraded = false;
		workstationLost = false;
		fractionalXpAccumulator = 0f;
		soldItemsTracker.clear();
		damageReputation.clear();
	}

	/**
	 * Добавляет урон, нанесённый игроком. Используется для расчёта репутации.
	 * @param playerId UUID игрока
	 * @param damageAmount Количество урона (в ХП, 1 = полсердца)
	 */
	public void addDamageReputation(String playerId, float damageAmount) {
		if (damageAmount <= 0) return;
		damageReputation.merge(playerId, damageAmount, Float::sum);
	}

	/**
	 * Получает суммарный урон, нанесённый игроком.
	 */
	public float getDamageReputation(String playerId) {
		return damageReputation.getOrDefault(playerId, 0f);
	}

	/**
	 * Рассчитывает процент повышения цены для игрока на основе нанесённого урона.
	 * @param playerId UUID игрока
	 * @param settings Настройки мода
	 * @return Процент повышения цены (0.0 = нет повышения)
	 */
	public double getDamageReputationPercent(String playerId, com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings settings) {
		float totalDamage = getDamageReputation(playerId);
		if (totalDamage <= 0) return 0.0;
		double percent = totalDamage * settings.damageReputationPercentPerHP;
		return Math.min(percent, settings.damageReputationMaxPercent);
	}

	/**
	 * Сбрасывает репутацию урона (вызывается при рестокe).
	 */
	public void resetDamageReputation() {
		damageReputation.clear();
	}

	/**
	 * Применяет деградацию опыта при рестокe, если рабочий блок потерян.
	 * Теряется 25% от XP_REQUIRED текущего уровня (для перехода на следующий).
	 * Если опыт был на максимуме — сразу теряется 1 уровень с 75% XP нового.
	 * Если опыт падает ниже 0 — снижается уровень с 75% XP нового уровня.
	 * Если достигнут 1-й уровень и опыт <= 0 — теряется профессия.
	 */
	public void applyWorkstationDecay() {
		if (!workstationLost || !hasEverTraded) return;

		// Житель 1-го уровня без опыта — теряем профессию
		if (level <= NOVICE && experience <= 0) {
			TradeOverhaulMod.LOGGER.info("Villager lost profession entirely (level=1, exp<=0, workstation lost)");
			experience = 0;
			return;
		}

		// XP_REQUIRED для перехода с текущего уровня на следующий
		int xpRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];

		// Если опыт на максимуме (только что перешёл на этот уровень) — сразу теряем уровень
		if (experience >= xpRequired) {
			level--;
			if (level <= NOVICE) {
				experience = 0;
				TradeOverhaulMod.LOGGER.info("Workstation decay: level=1, experience=0 (immediate drop from max)");
				return;
			}
			int newRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];
			experience = (int) Math.floor(newRequired * 0.75f);
			TradeOverhaulMod.LOGGER.info("Workstation decay: level={}, experience={} (immediate drop from max)", level, experience);
		} else {
			// Теряем 25% от XP_REQUIRED текущего уровня
			int decay = (int) Math.ceil(xpRequired * 0.25f);
			if (decay <= 0) decay = 1;
			experience -= decay;

			// Если опыт стал отрицательным — понижаем уровень
			while (experience < 0 && level > NOVICE) {
				level--;
				if (level <= NOVICE) {
					experience = 0;
					TradeOverhaulMod.LOGGER.info("Workstation decay: level=1, experience=0");
					break;
				}
				int newRequired = (level >= MAX_LEVEL) ? XP_REQUIRED[MAX_LEVEL - 1] : XP_REQUIRED[level];
				experience = (int) Math.floor(newRequired * 0.75f);
			}
			TradeOverhaulMod.LOGGER.info("Workstation decay: level={}, experience={}, decay={}", level, experience, decay);
		}
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
		return applyXpFromSale(itemId, amount, null);
	}

	/**
	 * Применяет опыт и обновляет трекинг предметов
	 * @param itemId ID предмета
	 * @param amount Количество предметов
	 * @param professionFile Файл конфигурации профессии (для кастомных множителей XP)
	 * @return true, если уровень повысился
	 */
	public boolean applyXpFromSale(String itemId, int amount, com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile professionFile) {
		// Рассчитываем XP: multiplier × amount
		// Сначала проверяем множитель из пула предметов в конфигурации профессии
		float multiplier = 1.0f;
		if (professionFile != null) {
			Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
			if (poolMultiplier != null) {
				multiplier = poolMultiplier;
				TradeOverhaulMod.LOGGER.debug("XP from pool: item={}, multiplier={}", itemId, multiplier);
			} else {
				// Fallback на глобальный конфиг
				multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
				TradeOverhaulMod.LOGGER.debug("XP from global config: item={}, multiplier={}", itemId, multiplier);
			}
		} else {
			// Если файл профессии не передан, используем глобальный конфиг
			multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
			TradeOverhaulMod.LOGGER.warn("XP: professionFile is null, using global config for item={}", itemId);
		}
		float xp = multiplier * amount;

		TradeOverhaulMod.LOGGER.debug("XP applied: item={}, amount={}, multiplier={}, xp={}", itemId, amount, multiplier, xp);

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
		if (nbt.contains("HasEverTraded")) {
			hasEverTraded = nbt.getBoolean("HasEverTraded");
		}
		if (nbt.contains("WorkstationLost")) {
			workstationLost = nbt.getBoolean("WorkstationLost");
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

		// Читаем репутацию урона
		if (nbt.contains("DamageReputation")) {
			damageReputation.clear();
			NbtList damageList = nbt.getList("DamageReputation", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < damageList.size(); i++) {
				NbtCompound entryNbt = damageList.getCompound(i);
				String playerId = entryNbt.getString("playerId");
				float damage = entryNbt.getFloat("damage");
				damageReputation.put(playerId, damage);
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
		nbt.putBoolean("HasEverTraded", hasEverTraded);
		nbt.putBoolean("WorkstationLost", workstationLost);
		
		// Записываем трекинг проданных предметов
		NbtList trackerList = new NbtList();
		for (Map.Entry<String, Integer> entry : soldItemsTracker.entrySet()) {
			NbtCompound itemNbt = new NbtCompound();
			itemNbt.putString("itemId", entry.getKey());
			itemNbt.putInt("count", entry.getValue());
			trackerList.add(itemNbt);
		}
		nbt.put("SoldItemsTracker", trackerList);

		// Записываем репутацию урона
		NbtList damageList = new NbtList();
		for (Map.Entry<String, Float> entry : damageReputation.entrySet()) {
			NbtCompound entryNbt = new NbtCompound();
			entryNbt.putString("playerId", entry.getKey());
			entryNbt.putFloat("damage", entry.getValue());
			damageList.add(entryNbt);
		}
		nbt.put("DamageReputation", damageList);
	}
	
	/**
	 * Устанавливает уровень (для синхронизации с ванильным уровнем)
	 * Не сбрасывает опыт, чтобы не терять прогресс
	 */
	public void setLevel(int level) {
		int oldLevel = this.level;
		this.level = Math.max(1, Math.min(MAX_LEVEL, level));
		
		// Сбрасываем опыт только если уровень понижается
		if (this.level < oldLevel) {
			this.experience = 0;
		}
	}
}
