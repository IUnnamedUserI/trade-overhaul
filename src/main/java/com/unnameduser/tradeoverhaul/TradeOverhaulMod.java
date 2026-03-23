package com.unnameduser.tradeoverhaul;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.command.TradeOverhaulCommand;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
			new ExtendedScreenHandlerType<>((int syncId, PlayerInventory inv, PacketByteBuf buf) ->
					new VillagerTradeScreenHandler(syncId, inv, buf));

	@Override
	public void onInitialize() {
		TradeConfigLoader.load(LOGGER);
		LOGGER.info("Trade Overhaul mod initialized!");
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_trade"), VILLAGER_TRADE_SCREEN_HANDLER);
		ModNetworking.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
			TradeOverhaulCommand.register(dispatcher)
		);
	}
}
