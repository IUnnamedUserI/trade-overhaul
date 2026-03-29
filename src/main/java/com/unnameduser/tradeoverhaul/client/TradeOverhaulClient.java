package com.unnameduser.tradeoverhaul.client;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreen;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.network.VillagerInventorySyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class TradeOverhaulClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Регистрируем экран с помощью ванильного метода
		HandledScreens.register(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, VillagerTradeScreen::new);
		
		// Регистрируем обработчик синхронизации инвентаря жителя
		ClientPlayNetworking.registerGlobalReceiver(VillagerInventorySyncPayload.ID, (client, handler, buf, responseSender) -> {
			VillagerInventorySyncPayload payload = VillagerInventorySyncPayload.read(buf);
			client.execute(() -> {
				TradeOverhaulMod.LOGGER.info("Received inventory sync: syncId={}, inventory size={}", 
					payload.syncId(), payload.inventory().length);
				
				if (client.player != null && client.player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
						&& tradeHandler.syncId == payload.syncId()) {
					// Обновляем инвентарь жителя
					for (int i = 0; i < payload.inventory().length; i++) {
						ItemStack stack = payload.inventory()[i];
						if (!stack.isEmpty()) {
							TradeOverhaulMod.LOGGER.info("  Setting slot {}: {} x{}", i, stack.getItem().getTranslationKey(), stack.getCount());
						}
						tradeHandler.getVillagerInventory().setStack(i, stack);
					}
					TradeOverhaulMod.LOGGER.info("Inventory sync complete");
				} else {
					TradeOverhaulMod.LOGGER.warn("Inventory sync failed: player={}, handler={}, syncId match={}", 
						client.player != null, 
						client.player.currentScreenHandler instanceof VillagerTradeScreenHandler,
						client.player != null && client.player.currentScreenHandler instanceof VillagerTradeScreenHandler ? 
							((VillagerTradeScreenHandler)client.player.currentScreenHandler).syncId == payload.syncId() : "N/A");
				}
			});
		});
	}
}