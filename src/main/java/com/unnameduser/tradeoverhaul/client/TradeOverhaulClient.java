package com.unnameduser.tradeoverhaul.client;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreen;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import com.unnameduser.tradeoverhaul.common.network.ConfigSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.DamageReputationSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.ProfessionLevelSyncPayload;
import com.unnameduser.tradeoverhaul.common.network.VillagerInventorySyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class TradeOverhaulClient implements ClientModInitializer {

    // Хранилище для восстановления локальных конфигов после отключения от сервера
    private static final Map<Identifier, ProfessionTradeFile> LOCAL_PROFESSIONS_BACKUP = new HashMap<>();
    private static String localSettingsBackup = null;
    private static boolean configsSynced = false;

    @Override
    public void onInitializeClient() {
        // Регистрируем экран с помощью ванильного метода
        HandledScreens.register(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, VillagerTradeScreen::new);

        // ========== СУЩЕСТВУЮЩИЕ ОБРАБОТЧИКИ ПАКЕТОВ ==========

        // Регистрируем обработчик синхронизации инвентаря жителя
        ClientPlayNetworking.registerGlobalReceiver(VillagerInventorySyncPayload.ID, (client, handler, buf, responseSender) -> {
            VillagerInventorySyncPayload payload = VillagerInventorySyncPayload.read(buf);
            client.execute(() -> {
                TradeOverhaulMod.LOGGER.info("Received inventory sync: syncId={}, inventory size={}",
                        payload.syncId(), payload.inventory().length);
                if (client.player != null && client.player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
                        && tradeHandler.syncId == payload.syncId()) {
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
                                    ((VillagerTradeScreenHandler) client.player.currentScreenHandler).syncId == payload.syncId() : "N/A");
                }
            });
        });

        // Регистрируем обработчик синхронизации уровня профессии
        ClientPlayNetworking.registerGlobalReceiver(ProfessionLevelSyncPayload.ID, (client, handler, buf, responseSender) -> {
            ProfessionLevelSyncPayload payload = ProfessionLevelSyncPayload.read(buf);
            client.execute(() -> {
                if (client.player != null && client.player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
                        && tradeHandler.syncId == payload.syncId()) {
                    tradeHandler.updateProfessionLevel(payload.level(), payload.experience(), payload.tradesCompleted(), payload.fractionalXp(), payload.soldItemsTracker());
                    TradeOverhaulMod.LOGGER.debug("Received profession level sync: level={}, exp={}, fractionalXp={}",
                            payload.level(), payload.experience(), payload.fractionalXp());
                }
            });
        });

        // Регистрируем обработчик синхронизации репутации урона
        ClientPlayNetworking.registerGlobalReceiver(DamageReputationSyncPayload.ID, (client, handler, buf, responseSender) -> {
            DamageReputationSyncPayload payload = DamageReputationSyncPayload.read(buf);
            client.execute(() -> {
                if (client.player != null && client.player.currentScreenHandler instanceof VillagerTradeScreenHandler tradeHandler
                        && tradeHandler.syncId == payload.syncId()) {
                    tradeHandler.updateDamageReputation(payload.damageReputation());
                    TradeOverhaulMod.LOGGER.debug("Received damage reputation sync: {} entries", payload.damageReputation().size());
                }
            });
        });

        // ========== НОВЫЙ: ОБРАБОТЧИК СИНХРОНИЗАЦИИ КОНФИГОВ ==========

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (client, handler, buf, responseSender) -> {
            ConfigSyncPayload payload = ConfigSyncPayload.read(buf);

            client.execute(() -> {
                // При первой синхронизации сохраняем локальные конфиги
                if (!configsSynced) {
                    LOCAL_PROFESSIONS_BACKUP.clear();
                    for (var entry : TradeConfigLoader.getAllProfessions().entrySet()) {
                        // Глубокая копия через JSON, чтобы избежать модификации оригинала
                        String json = TradeConfigLoader.professionToJson(entry.getValue());
                        LOCAL_PROFESSIONS_BACKUP.put(entry.getKey(), TradeConfigLoader.professionFromJson(json));
                    }
                    localSettingsBackup = TradeConfigLoader.settingsToJson();
                    configsSynced = true;
                    TradeOverhaulMod.LOGGER.info("TradeOverhaul: backed up local configs for server sync");
                }

                // Применяем серверные конфиги
                for (var entry : payload.professionJsons().entrySet()) {
                    var prof = TradeConfigLoader.professionFromJson(entry.getValue());
                    if (prof != null) {
                        TradeConfigLoader.setProfessionTemp(entry.getKey(), prof);
                    }
                }

                // Применяем настройки
                var settings = TradeConfigLoader.settingsFromJson(payload.settingsJson());
                if (settings != null) {
                    TradeConfigLoader.setSettings(settings);
                }

                // Уведомление игрока (только если хеши не совпадали)
                if (client.player != null && !payload.configsHash().equals(TradeConfigLoader.getConfigsHash())) {
                    // Используем полное имя класса ClickEvent, чтобы избежать ошибок импорта
                    net.minecraft.text.ClickEvent clickEvent = new net.minecraft.text.ClickEvent(
                            net.minecraft.text.ClickEvent.Action.OPEN_URL,
                            "https://modrinth.com/mod/tradeoverhaul"
                    );

                    Text message = Text.literal("§a[TradeOverhaul] Конфигурация синхронизирована с сервером")
                            .styled(s -> s.withClickEvent(clickEvent));

                    client.player.sendMessage(message, true);
                }
            });
        });

        // Восстановление локальных конфигов при отключении от сервера
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (configsSynced) {
                client.execute(() -> {
                    // Восстанавливаем профессии
                    for (var entry : LOCAL_PROFESSIONS_BACKUP.entrySet()) {
                        TradeConfigLoader.restoreProfession(entry.getKey(), entry.getValue());
                    }
                    // Восстанавливаем настройки
                    if (localSettingsBackup != null) {
                        TradeConfigLoader.setSettings(TradeConfigLoader.settingsFromJson(localSettingsBackup));
                    }
                    LOCAL_PROFESSIONS_BACKUP.clear();
                    localSettingsBackup = null;
                    configsSynced = false;
                    TradeOverhaulMod.LOGGER.info("TradeOverhaul: restored local configs");
                });
            }
        });
    }
}