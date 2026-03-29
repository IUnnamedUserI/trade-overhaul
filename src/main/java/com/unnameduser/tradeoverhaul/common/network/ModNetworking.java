package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;

public class ModNetworking {

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(TradePayload.ID, (server, player, networkHandler, buf, responseSender) -> {
			TradePayload payload = TradePayload.read(buf);
			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
						&& tradeHandler.syncId == payload.syncId()) {
					if (payload.isBuying()) {
						tradeHandler.handleBuyOnServer(payload.slot(), player, payload.buyWholeStack());
					} else {
						tradeHandler.handleSellOnServer(payload.slot(), player, payload.sellWholeStack());
					}
				}
			});
		});
	}

	/**
	 * Отправляет клиенту синхронизацию инвентаря жителя.
	 */
	public static void sendInventorySync(net.minecraft.server.network.ServerPlayerEntity player, int syncId, net.minecraft.inventory.Inventory inventory) {
		if (inventory == null) return;
		
		ItemStack[] stacks = new ItemStack[inventory.size()];
		for (int i = 0; i < inventory.size(); i++) {
			stacks[i] = inventory.getStack(i);
		}
		
		VillagerInventorySyncPayload payload = new VillagerInventorySyncPayload(syncId, stacks);
		var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		payload.write(buf);
		ServerPlayNetworking.send(player, VillagerInventorySyncPayload.ID, buf);
	}
}
