package com.unnameduser.tradeoverhaul.common.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация множителей опыта для разных предметов
 */
public class VillagerXpConfig {
	
	// Множители XP для конкретных предметов (по умолчанию)
	public static final Map<String, Float> DEFAULT_XP_MULTIPLIERS = new HashMap<>();
	
	static {
		// Сельскохозяйственные предметы (низкий XP)
		DEFAULT_XP_MULTIPLIERS.put("minecraft:wheat_seeds", 0.01f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:beetroot_seeds", 0.01f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:melon_seeds", 0.01f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:pumpkin_seeds", 0.01f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:wheat", 0.05f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:carrot", 0.05f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:potato", 0.05f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:beetroot", 0.05f);
		
		// Обычные предметы (стандартный XP)
		DEFAULT_XP_MULTIPLIERS.put("minecraft:book", 1.0f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:paper", 0.5f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:leather", 0.5f);
		
		// Редкие предметы (повышенный XP)
		DEFAULT_XP_MULTIPLIERS.put("minecraft:emerald", 2.0f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:diamond", 5.0f);
		DEFAULT_XP_MULTIPLIERS.put("minecraft:nether_star", 10.0f);
	}
	
	/**
	 * Получает множитель XP для предмета
	 */
	public static float getXpMultiplier(String itemId) {
		return DEFAULT_XP_MULTIPLIERS.getOrDefault(itemId, 1.0f);
	}
	
	/**
	 * Рассчитывает XP для предмета
	 * @param baseXp Базовый XP (обычно 1)
	 * @param itemId ID предмета
	 * @param timesSold Сколько раз этот предмет уже продавали (больше не используется)
	 * @return Итоговый XP
	 */
	public static float calculateXpWithDiminishingReturns(float baseXp, String itemId, int timesSold) {
		float multiplier = getXpMultiplier(itemId);
		return baseXp * multiplier;
	}
}
