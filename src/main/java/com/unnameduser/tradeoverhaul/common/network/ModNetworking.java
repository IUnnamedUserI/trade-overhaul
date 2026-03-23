package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(TradePayload.ID, (server, player, networkHandler, buf, responseSender) -> {
			TradePayload payload = TradePayload.read(buf);
			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
						&& tradeHandler.syncId == payload.syncId()) {
					if (payload.isBuying()) {
						tradeHandler.handleBuyOnServer(payload.slot(), player);
					} else {
						tradeHandler.handleSellOnServer(payload.slot(), player, payload.sellWholeStack());
					}
				}
			});
		});
	}
}
