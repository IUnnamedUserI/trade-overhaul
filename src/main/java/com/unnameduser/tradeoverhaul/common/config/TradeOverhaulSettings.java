package com.unnameduser.tradeoverhaul.common.config;

public class TradeOverhaulSettings {
	/**
	 * Интервал глобального рестокa в игровых днях.
	 * По умолчанию 2 игровых дня = 40 минут реального времени.
	 * Ресток происходит для всех жителей одновременно.
	 */
	public int restockIntervalGameDays = 2;

	/**
	 * Количество жителей, обрабатываемых за один тик во время рестокa.
	 * Позволяет распределить нагрузку и избежать лагов.
	 * 0 = все сразу (старое поведение), >0 = порциями.
	 * Рекомендуемое: 5-10 жителей/тик.
	 */
	public int restockBatchSize = 5;

	/**
	 * Устаревшее поле, больше не используется.
	 * Оставлено для обратной совместимости конфигов.
	 * @deprecated Используйте restockIntervalGameDays
	 */
	@Deprecated
	public int restockDelayGameDays = 2;

	public int walletAfterRestockMin = 20;
	public int walletAfterRestockMax = 40;
	public int maxStockDefault = 16;
	
	// Weapon pricing
	public int weaponBuyPerDamage = 4;
	public int weaponSellPerDamage = 2;
	public int weaponBuyBase = 1;
	public int weaponSellBase = 0;
	public boolean weaponUseDurability = true;
	
	// Tool pricing
	public int toolBuyPerEfficiency = 3;
	public int toolSellPerEfficiency = 1;
	public int toolBuyBase = 1;
	public int toolSellBase = 0;
	public boolean toolUseDurability = true;
	
	// Durability pricing (price reduction per missing durability percentage step)
	public int durabilityPriceStep = 10;
	public int durabilityPriceReductionPerStep = 1;
	
	// Buy-only discount factor (default discount for items not in sell pool)
	public int buyOnlyDiscountDefault = 50;
	
	// Batch trading settings
	public boolean batchTradingEnabled = true;
	public boolean enforceQuantityMultiples = true;

	// Damage reputation settings
	/**
	 * Процент повышения цены за 1 единицу урона, нанесённого игроком.
	 * Значение применяется к каждой единице урона (0.5 ХП = полсердца).
	 * Например: 5.0 = +5% за каждое полсердца урона.
	 * Штраф накапливается и сбрасывается при рестокe.
	 */
	public double damageReputationPercentPerHP = 5.0;

	/**
	 * Максимальный процент повышения цены от репутации урона.
	 * Ограничивает суммарный штраф, чтобы цены не уходили в бесконечность.
	 */
	public double damageReputationMaxPercent = 200.0;
}
