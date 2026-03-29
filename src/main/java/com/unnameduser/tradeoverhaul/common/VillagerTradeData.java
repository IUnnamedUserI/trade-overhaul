package com.unnameduser.tradeoverhaul.common;

import com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface VillagerTradeData {
	VillagerInventoryComponent tradeOverhaul$getInventory();

	int tradeOverhaul$getWalletEmeralds();

	void tradeOverhaul$setWalletEmeralds(int amount);

	VillagerCurrencyComponent tradeOverhaul$getCurrency();

	int[] tradeOverhaul$getOfferSlots();

	void tradeOverhaul$setOfferSlots(int[] slots);

	long tradeOverhaul$getEmptySinceTick();

	void tradeOverhaul$setEmptySinceTick(long worldTick);

	@Nullable
	PlayerEntity tradeOverhaul$getActiveTrader();

	void tradeOverhaul$setActiveTrader(@Nullable PlayerEntity player);
}
