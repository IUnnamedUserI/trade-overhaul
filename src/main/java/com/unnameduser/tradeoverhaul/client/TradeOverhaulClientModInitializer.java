package com.unnameduser.tradeoverhaul.client;

import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.network.AvailableRecipesPayload;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.screen.VillagerInteractionScreen; // ✅ Правильный импорт
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class TradeOverhaulClientModInitializer implements ClientModInitializer {

    private static final Identifier AVAILABLE_RECIPES_PACKET_ID =
            new Identifier("tradeoverhaul", "available_recipes_full"); // ДОЛЖНО СОВПАДАТЬ с AvailableRecipesPayload.ID

    @Override
    public void onInitializeClient() {
        // 1. Загрузка конфигов при подключении
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                RecipeManager.getInstance().loadRecipesClient()
        );

        // 2. Приём пакета (СТАРАЯ сеть)
        // В TradeOverhaulClientModInitializer.java
        ClientPlayNetworking.registerGlobalReceiver(
                AvailableRecipesPayload.ID,
                (client, handler, buf, responseSender) -> {
                    AvailableRecipesPayload payload = AvailableRecipesPayload.read(buf);

                    client.execute(() -> {
                        // Очищаем старые рецепты в RecipeManager
                        RecipeManager.getInstance().clearRecipes(); // Нужно добавить этот метод

                        // Добавляем полученные с сервера рецепты
                        for (CraftRecipe recipe : payload.getRecipes()) {
                            RecipeManager.getInstance().addCraftRecipe(recipe); // Нужно добавить этот метод
                        }

                        // Обновляем открытый экран
                        if (client.currentScreen instanceof VillagerInteractionScreen screen) {
                            screen.onAvailableRecipesReceived(payload.getRecipes());
                        }
                    });
                }
        );
    }
}