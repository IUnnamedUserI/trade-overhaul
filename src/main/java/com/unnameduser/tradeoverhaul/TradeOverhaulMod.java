package com.unnameduser.tradeoverhaul;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.command.DisassembleCommand;
import com.unnameduser.tradeoverhaul.common.command.TradeOverhaulCommand;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.ConfigSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.ModNetworking;
import com.unnameduser.tradeoverhaul.common.network.TradeRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.trade.GlobalRestockTimer;
import com.unnameduser.tradeoverhaul.screen.VillagerCraftingScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

public class TradeOverhaulMod implements ModInitializer {
	public static final String MOD_ID = "tradeoverhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// ✓ ОСТАВЛЯЕМ регистрацию для совместимости со старыми файлами
	// (но не используем эту архитектуру в новом коде)
	public static final ScreenHandlerType<VillagerTradeScreenHandler> VILLAGER_TRADE_SCREEN_HANDLER =
			new ExtendedScreenHandlerType<>((int syncId, PlayerInventory inv, PacketByteBuf buf) -> {
				return new VillagerTradeScreenHandler(syncId, inv, buf);
			});

	public static final ScreenHandlerType<VillagerCraftingScreenHandler> VILLAGER_CRAFTING_SCREEN_HANDLER =
			new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
					new VillagerCraftingScreenHandler(syncId, inventory, buf)
			);

	// ID для пакетов
	public static final Identifier AVAILABLE_RECIPES_PACKET_ID =
			new Identifier(MOD_ID, "available_recipes");

	// ✅ ID для пакета торгового запроса
	public static final Identifier TRADE_REQUEST_ID =
			new Identifier(MOD_ID, "trade_request");

	@Override
	public void onInitialize() {
		TradeConfigLoader.load(LOGGER);
		LOGGER.info("Trade Overhaul mod initialized!");

		LOGGER.info("Registering screen handlers...");
		// ✓ Регистрируем ОБА хендлера, чтобы старый код компилировался
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_trade"), VILLAGER_TRADE_SCREEN_HANDLER);
		Registry.register(Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "villager_crafting"), VILLAGER_CRAFTING_SCREEN_HANDLER);

		LOGGER.info("Registering networking...");
		ModNetworking.register();

		// Обработчик разборки
		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "disassemble_request"), (server, player, handler, buf, responseSender) -> {
			int syncId = buf.readInt();
			int slotIndex = buf.readInt();
			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler c && c.syncId == syncId) {
					c.handleDisassembleRequest(player, slotIndex);
				}
			});
		});

		// ✅ Обработчик торгового запроса
		ServerPlayNetworking.registerGlobalReceiver(TRADE_REQUEST_ID, (server, player, handler, buf, responseSender) -> {
			TradeRequestC2SPacket packet = TradeRequestC2SPacket.decode(buf);
			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler screenHandler &&
						screenHandler.syncId == packet.syncId) {
					screenHandler.handleTradeRequest(packet.slotIndex, packet.amount, packet.buying, player);
				}
			});
		});

		LOGGER.info("Registering commands...");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					literal("tradeoverhaul")
							.requires(src -> src.hasPermissionLevel(2))
							.then(literal("refresh")
									.executes(ctx -> {
										// Получаем сервер
										net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();

										// Перебираем все миры
										for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
											// Ищем всех жителей
											for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
												if (entity instanceof VillagerEntity villager) {
													// Проверяем, что у жителя есть профессия
													var profession = villager.getVillagerData().getProfession();
													if (profession != null &&
															profession != net.minecraft.village.VillagerProfession.NONE &&
															profession != net.minecraft.village.VillagerProfession.NITWIT) {

														// Выполняем ресток
														Identifier profId = Registries.VILLAGER_PROFESSION.getId(profession);
														var professionFile = com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader.getProfession(profId);
														if (professionFile != null) {
															com.unnameduser.tradeoverhaul.common.trade.TradeRestock.forceRestock(
																	villager,
																	professionFile,
																	com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader.getSettings()
															);
														}
													}
												}
											}
										}

										ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§aAll villagers have been restocked!"), true);
										TradeOverhaulMod.LOGGER.info("Force restock completed for all villagers");
										return 1;
									})
							)
							// ✅ Новые подкоманды цепляются к тому же корневому literal
							.then(literal("generate_disassembly_whitelist")
									.executes(ctx -> DisassembleCommand.generateList(true, ctx.getSource()))
							)
							.then(literal("generate_repair_whitelist")
									.executes(ctx -> DisassembleCommand.generateList(false, ctx.getSource()))
							)
			);
		});

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

		// Регистрация обработчика торговли (старый)
		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "trade_action"), (server, player, handler, buf, responseSender) -> {
			int syncId = buf.readInt();
			int slotIndex = buf.readInt();
			boolean buying = buf.readBoolean();
			boolean wholeStack = buf.readBoolean();
			boolean buyWholeStack = buf.readBoolean();
			boolean buyTen = buf.readBoolean();

			server.execute(() -> {
				if (player.currentScreenHandler instanceof VillagerCraftingScreenHandler craftingHandler && craftingHandler.syncId == syncId) {
					if (buying) {
						craftingHandler.handleTradePurchase(player, slotIndex, buyWholeStack, buyTen);
					} else {
						craftingHandler.handleTradeSell(player, slotIndex, wholeStack, buyTen);
					}
				}
			});
		});

		LOGGER.info("Trade Overhaul initialization complete!");
	}
}