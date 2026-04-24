package com.unnameduser.tradeoverhaul;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.command.TradeOverhaulCommand;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.ConfigSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.ModNetworking;
import com.unnameduser.tradeoverhaul.common.trade.GlobalRestockTimer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;;;

public class TradeOverhaulMod implements ModInitializer {
	public static final String MOD_ID = "tradeoverhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ScreenHandlerType<VillagerTradeScreenHandler> VILLAGER_TRADE_SCREEN_HANDLER =
			new ExtendedScreenHandlerType<>((int syncId, PlayerInventory inv, PacketByteBuf buf) -> {
				// Этот конструктор используется ТОЛЬКО на клиенте
				// На сервере используется createMenu() из VillagerTradeScreenHandlerFactory
				return new VillagerTradeScreenHandler(syncId, inv, buf);
			});

	@Override
	public void onInitialize() {
		TradeConfigLoader.load(LOGGER);
		LOGGER.info("Trade Overhaul mod initialized!");
		LOGGER.info("Registering screen handler...");
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_trade"), VILLAGER_TRADE_SCREEN_HANDLER);
		LOGGER.info("Registering networking...");
		ModNetworking.register();
		LOGGER.info("Registering commands...");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			TradeOverhaulCommand.register(dispatcher)
		);
		LOGGER.info("Registering global restock timer...");
		GlobalRestockTimer.register();
		LOGGER.info("Trade Overhaul initialization complete!");

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ConfigSyncPayload payload = ConfigSyncPayload.fromServerConfigs();

			// Создаём буфер и записываем данные
			var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
			payload.write(buf);

			// Отправляем классическим способом для 1.20.1
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player, ConfigSyncPayload.ID, buf);

			TradeOverhaulMod.LOGGER.debug("Sent config sync to player {}", handler.player.getName().getString());
		});
	}
}
