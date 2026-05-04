package com.unnameduser.tradeoverhaul;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.command.TradeOverhaulCommand;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.ConfigSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.ModNetworking;
import com.unnameduser.tradeoverhaul.common.trade.GlobalRestockTimer;
import com.unnameduser.tradeoverhaul.screen.VillagerCraftingScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeOverhaulMod implements ModInitializer {
	public static final String MOD_ID = "tradeoverhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ScreenHandlerType<VillagerTradeScreenHandler> VILLAGER_TRADE_SCREEN_HANDLER =
			new ExtendedScreenHandlerType<>((int syncId, PlayerInventory inv, PacketByteBuf buf) -> {
				return new VillagerTradeScreenHandler(syncId, inv, buf);
			});

	public static final ScreenHandlerType<VillagerCraftingScreenHandler> VILLAGER_CRAFTING_SCREEN_HANDLER =
			new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
					new VillagerCraftingScreenHandler(syncId, inventory, buf)
			);

	@Override
	public void onInitialize() {
		TradeConfigLoader.load(LOGGER);
		LOGGER.info("Trade Overhaul mod initialized!");

		LOGGER.info("Registering screen handlers...");
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_trade"), VILLAGER_TRADE_SCREEN_HANDLER);
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_crafting"), VILLAGER_CRAFTING_SCREEN_HANDLER);

		LOGGER.info("Registering networking...");
		ModNetworking.register();

		LOGGER.info("Registering commands...");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				TradeOverhaulCommand.register(dispatcher)
		);

		LOGGER.info("Registering global restock timer...");
		GlobalRestockTimer.register();

		LOGGER.info("Loading recipes...");
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			RecipeManager.getInstance().loadRecipes(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ConfigSyncPayload payload = ConfigSyncPayload.fromServerConfigs();
			var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
			payload.write(buf);
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player, ConfigSyncPayload.ID, buf);
			TradeOverhaulMod.LOGGER.debug("Sent config sync to player {}", handler.player.getName().getString());
		});

		LOGGER.info("Trade Overhaul initialization complete!");
	}
}