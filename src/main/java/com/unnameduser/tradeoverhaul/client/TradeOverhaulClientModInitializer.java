package com.unnameduser.tradeoverhaul.client;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.network.AvailableRecipesPayload;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.screen.VillagerInteractionScreen; // ✅ Правильный импорт
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TradeOverhaulClientModInitializer implements ClientModInitializer {

    private static final Identifier AVAILABLE_RECIPES_PACKET_ID =
            new Identifier("tradeoverhaul", "available_recipes_full"); // ДОЛЖНО СОВПАДАТЬ с AvailableRecipesPayload.ID

    private boolean recipesLoaded = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier("tradeoverhaul", "available_recipes_full"),
                (client, handler, buf, responseSender) -> {
                    int count = buf.readVarInt();
                    List<CraftRecipe> recipes = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        recipes.add(readRecipeFromBuf(buf));
                    }

                    System.out.println("[TradeOverhaul] CLIENT: Received " + count + " recipes, recipesLoaded=" + recipesLoaded);

                    // Загружаем рецепты только один раз
                    if (!recipesLoaded) {
                        recipesLoaded = true;
                        RecipeManager.getInstance().setAllServerRecipes(recipes);
                        System.out.println("[TradeOverhaul] CLIENT: Recipes saved to manager");

                        // ✅ ВАЖНО: Задержка для инициализации GUI
                        client.execute(() -> {
                            System.out.println("[TradeOverhaul] CLIENT: Checking screen, currentScreen=" + client.currentScreen);
                            if (client.currentScreen instanceof VillagerInteractionScreen screen) {
                                System.out.println("[TradeOverhaul] CLIENT: Calling refreshRecipesForCurrentVillager");
                                screen.refreshRecipesForCurrentVillager();
                            } else {
                                System.out.println("[TradeOverhaul] CLIENT: Screen is not VillagerInteractionScreen");
                            }
                        });
                    }
                }
        );
    }

    private CraftRecipe readRecipeFromBuf(PacketByteBuf buf) {
        // Защита от некорректных строк
        String id = sanitizeString(buf.readString());
        int requiredLevel = buf.readInt();
        int cost = buf.readInt();
        boolean copyNbt = buf.readBoolean();
        int uniqueIndex = buf.readInt();

        String profession = null;
        if (buf.readBoolean()) {
            profession = sanitizeString(buf.readString());
        }

        int ingCount = buf.readVarInt();
        List<Ingredient> ingredients = new ArrayList<>(ingCount);
        for (int i = 0; i < ingCount; i++) {
            NbtCompound stackNbt = buf.readNbt();
            ItemStack item = ItemStack.fromNbt(stackNbt);
            int count = buf.readInt();
            ingredients.add(new Ingredient(item, count));
        }

        NbtCompound resultNbt = buf.readNbt();
        ItemStack result = ItemStack.fromNbt(resultNbt);

        return new CraftRecipe(id, requiredLevel, ingredients, result, cost, copyNbt, uniqueIndex, profession);
    }

    // Добавьте этот метод для очистки строк
    private String sanitizeString(String input) {
        if (input == null) return "";
        // Удаляем все недопустимые символы (оставляем буквы, цифры, подчёркивания, дефисы, слеши, точки и двоеточия)
        String cleaned = input.replaceAll("[^a-zA-Z0-9_/.:-]", "");
        // Дополнительная защита от нулевых символов
        cleaned = cleaned.replaceAll("\\u0000", "");
        return cleaned;
    }
}