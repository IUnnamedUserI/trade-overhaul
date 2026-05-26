package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.screen.VillagerCraftingScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModNetworking {

	public static void register() {
		// Обработчик запроса на крафт
		ServerPlayNetworking.registerGlobalReceiver(
				new Identifier(TradeOverhaulMod.MOD_ID, "craft_request"),
				(server, player, handler, buf, responseSender) -> {
					CraftRequestC2SPacket packet = CraftRequestC2SPacket.decode(buf);
					server.execute(() -> {
						if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler craftingHandler) {
							craftingHandler.handleCraftRequest(player, packet.getRecipeId(), packet.getSelectedSlot());
						} else {
							System.out.println("[TradeOverhaul] Player doesn't have crafting screen open");
						}
					});
				}
		);

		// Отправка списка рецептов при подключении игрока
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			List<CraftRecipe> allRecipes = new ArrayList<>(RecipeManager.getInstance().getAllCraftRecipes().values());
			AvailableRecipesPayload payload = new AvailableRecipesPayload(allRecipes);
			PacketByteBuf buf = PacketByteBufs.create();
			payload.write(buf);
			sender.sendPacket(AvailableRecipesPayload.ID, buf);
		});

		// Обработчик запроса на покупку из панели крафта
		ServerPlayNetworking.registerGlobalReceiver(
				new Identifier(TradeOverhaulMod.MOD_ID, "craft_panel_buy_request"),
				(server, player, handler, buf, responseSender) -> {
					int syncId = buf.readVarInt();
					String itemId = buf.readString();
					int amount = buf.readByte();

					server.execute(() -> {
						if (player.currentScreenHandler.syncId == syncId &&
								player.currentScreenHandler instanceof VillagerCraftingScreenHandler h) {
							h.handleCraftPanelBuyRequest(player, itemId, amount);
						}
					});
				}
		);

		// Обработчик запроса на починку одного предмета
		ServerPlayNetworking.registerGlobalReceiver(
				new Identifier(TradeOverhaulMod.MOD_ID, "repair_request"),
				(server, player, handler, buf, responseSender) -> {
					RepairRequestC2SPacket packet = RepairRequestC2SPacket.decode(buf);
					server.execute(() -> {
						if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler craftingHandler) {
							craftingHandler.handleRepairRequest(player, packet.getSlotIndex());
						}
					});
				}
		);

		// Обработчик запроса на массовую починку
		ServerPlayNetworking.registerGlobalReceiver(
				new Identifier(TradeOverhaulMod.MOD_ID, "repair_all_request"),
				(server, player, handler, buf, responseSender) -> {
					RepairAllRequestC2SPacket packet = RepairAllRequestC2SPacket.decode(buf);
					server.execute(() -> {
						if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler craftingHandler) {
							craftingHandler.handleRepairAllRequest(player);
						}
					});
				}
		);
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
	public static void sendProfessionLevelSync(net.minecraft.server.network.ServerPlayerEntity player, int syncId,
											   int level, int experience, int tradesCompleted, float fractionalXp, net.minecraft.nbt.NbtCompound soldItemsTracker) {
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