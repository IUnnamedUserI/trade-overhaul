package com.unnameduser.tradeoverhaul.client;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class TradeOverhaulClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Регистрируем экран с помощью ванильного метода
		HandledScreens.register(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, VillagerTradeScreen::new);
	}
}