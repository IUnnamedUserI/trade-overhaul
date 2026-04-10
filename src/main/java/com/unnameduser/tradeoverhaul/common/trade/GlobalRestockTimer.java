package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Глобальный таймер рестокa — обновляет всех жителей одновременно
 * через заданный интервал игровых дней.
 * 
 * Оптимизация: ресток разбивается на батчи по restockBatchSize жителей за тик,
 * чтобы избежать лагов при большом количестве жителей.
 */
public final class GlobalRestockTimer {
	private static final List<VillagerEntity> trackedVillagers = new CopyOnWriteArrayList<>();
	private static long lastRestockTick = 0;
	
	// Состояние поочерёдного рестокa
	private static List<VillagerEntity> pendingRestock = new ArrayList<>();
	private static int restockIndex = 0;
	private static long restockStartTime = 0;

	private GlobalRestockTimer() {}

	/**
	 * Регистрирует обработчики событий для глобального рестокa
	 */
	public static void register() {
		// Отслеживаем загрузку жителей
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof VillagerEntity villager) {
				trackedVillagers.add(villager);
			}
		});

		// Отслеживаем выгрузку/смерть жителей
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
			if (entity instanceof VillagerEntity) {
				trackedVillagers.remove(entity);
			}
		});

		// Регистрируем тик сервера для глобального рестокa
		ServerTickEvents.END_SERVER_TICK.register(GlobalRestockTimer::onServerTick);

		TradeOverhaulMod.LOGGER.info("Global restock timer registered (batch optimization enabled)");
	}

	private static void onServerTick(MinecraftServer server) {
		TradeOverhaulSettings settings = TradeConfigLoader.getSettings();
		int batchSize = settings.restockBatchSize;
		
		// Если есть отложенные жители — продолжаем поочерёдный ресток
		if (!pendingRestock.isEmpty()) {
			processRestockBatch(server, batchSize);
			return;
		}
		
		// Проверяем, пора ли начать новый ресток
		long currentTick = server.getOverworld().getTime();
		long intervalTicks = getRestockIntervalTicks();

		if (currentTick - lastRestockTick >= intervalTicks) {
			startGlobalRestock(server);
		}
	}

	/**
	 * Начинает глобальный ресток — собирает всех валидных жителей в список
	 */
	private static void startGlobalRestock(MinecraftServer server) {
		List<VillagerEntity> validVillagers = new ArrayList<>();
		
		for (VillagerEntity villager : trackedVillagers) {
			if (villager.isRemoved() || !villager.isAlive()) {
				trackedVillagers.remove(villager);
				continue;
			}
			
			Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
			ProfessionTradeFile file = TradeConfigLoader.getProfession(profId);
			
			if (file == null) continue;
			
			validVillagers.add(villager);
		}
		
		if (validVillagers.isEmpty()) {
			TradeOverhaulMod.LOGGER.debug("Global restock: no valid villagers found");
			return;
		}
		
		// Перемешиваем для fairness
		java.util.Collections.shuffle(validVillagers);
		
		pendingRestock = validVillagers;
		restockIndex = 0;
		restockStartTime = System.currentTimeMillis();
		
		TradeOverhaulSettings settings = TradeConfigLoader.getSettings();
		TradeOverhaulMod.LOGGER.info("Starting global restock: {} villagers, batch size: {}", 
			validVillagers.size(), 
			settings.restockBatchSize > 0 ? settings.restockBatchSize : "all");
	}

	/**
	 * Обрабатывает один батч жителей
	 */
	private static void processRestockBatch(MinecraftServer server, int batchSize) {
		if (pendingRestock.isEmpty()) return;
		
		int processed = 0;
		int batchSizeActual = batchSize > 0 ? batchSize : pendingRestock.size() - restockIndex;
		
		while (restockIndex < pendingRestock.size() && processed < batchSizeActual) {
			VillagerEntity villager = pendingRestock.get(restockIndex);
			restockIndex++;
			
			// Проверяем, что житель всё ещё жив
			if (villager.isRemoved() || !villager.isAlive()) {
				continue;
			}
			
			Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
			ProfessionTradeFile file = TradeConfigLoader.getProfession(profId);
			
			if (file == null) continue;
			
			TradeRestock.prepareRestock(villager);
			processed++;
		}
		
		// Проверяем, завершили ли мы ресток
		if (restockIndex >= pendingRestock.size()) {
			long elapsed = System.currentTimeMillis() - restockStartTime;
			TradeOverhaulMod.LOGGER.info("Global restock completed: {} villagers processed in {}ms", 
				pendingRestock.size(), elapsed);
			
			pendingRestock.clear();
			restockIndex = 0;
			lastRestockTick = server.getOverworld().getTime();
		}
	}

	/**
	 * Возвращает интервал рестокa в тиках.
	 * 1 игровой день = 24000 тиков (20 минут реального времени)
	 */
	private static long getRestockIntervalTicks() {
		int gameDays = TradeConfigLoader.getSettings().restockIntervalGameDays;
		return (long) gameDays * 24000L;
	}
}
