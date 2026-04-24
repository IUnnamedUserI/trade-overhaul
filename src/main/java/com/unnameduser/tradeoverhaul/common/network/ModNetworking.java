package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import com.unnameduser.tradeoverhaul.common.network.ConfigSyncPayload;

public class ModNetworking {

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(TradePayload.ID, (server, player, networkHandler, buf, responseSender) -> {
			TradePayload payload = TradePayload.read(buf);
			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
						&& tradeHandler.syncId == payload.syncId()) {
					if (payload.isBuying()) {
						tradeHandler.handleBuyOnServer(payload.slot(), player, payload.buyWholeStack(), payload.buyTen());
					} else {
						tradeHandler.handleSellOnServer(payload.slot(), player, payload.sellWholeStack(), payload.buyTen());
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

	/**
	 * Отправляет клиенту синхронизацию уровня профессии жителя.
	 */
	public static void sendProfessionLevelSync(net.minecraft.server.network.ServerPlayerEntity player, int syncId, int level, int experience, int tradesCompleted, float fractionalXp, net.minecraft.nbt.NbtCompound soldItemsTracker) {
		ProfessionLevelSyncPayload payload = new ProfessionLevelSyncPayload(syncId, level, experience, tradesCompleted, fractionalXp, soldItemsTracker);
		var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		payload.write(buf);
		ServerPlayNetworking.send(player, ProfessionLevelSyncPayload.ID, buf);
	}

	/**
	 * Отправляет клиенту синхронизацию репутации урона.
	 */
	public static void sendDamageReputationSync(net.minecraft.server.network.ServerPlayerEntity player, int syncId,
			java.util.Map<String, Float> damageReputation) {
		DamageReputationSyncPayload payload = new DamageReputationSyncPayload(syncId, damageReputation);
		var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		payload.write(buf);
		ServerPlayNetworking.send(player, DamageReputationSyncPayload.ID, buf);
	}
}
