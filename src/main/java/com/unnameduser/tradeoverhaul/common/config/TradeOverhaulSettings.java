package com.unnameduser.tradeoverhaul.common.config;

public class TradeOverhaulSettings {
	public int restockDelayGameDays = 2;
	public int walletAfterRestock = 40;
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
}
