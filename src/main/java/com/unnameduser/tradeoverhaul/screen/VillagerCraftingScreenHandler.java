package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class VillagerCraftingScreenHandler extends ScreenHandler {

    public static final int FIRST_MAIN_GRID_SLOT_INDEX = 0;

    private final int villagerLevel;
    private final String professionId;
    private final int villagerEntityId;
    private VillagerEntity villager;

    // === Клиентский конструктор (читает из буфера) ===
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.villagerLevel = buf.readVarInt();
        this.professionId = buf.readString();
        this.villagerEntityId = buf.readVarInt();
        this.villager = null;
        addPlayerInventory(playerInventory);
    }

    // === Серверный конструктор (принимает сущность) ===
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.villager = villager;
        this.villagerLevel = villager.getVillagerData().getLevel();
        this.professionId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString();
        this.villagerEntityId = villager.getId();
        addPlayerInventory(playerInventory);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return villager == null || player.squaredDistanceTo(villager) <= 64.0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    // === ОБРАБОТКА КРАФТА ===
    public void handleCraftRequest(ServerPlayerEntity player, String recipeId, int selectedSlot) {
        // ✓ ИСПРАВЛЕНО: правильный метод из твоего RecipeManager
        CraftRecipe recipe = RecipeManager.getInstance().getCraftRecipeById(recipeId);
        if (recipe == null) return;

        // Проверка уровня жителя
        if (recipe.getRequiredLevel() > villagerLevel) return;

        // Проверка ингредиентов
        if (!hasIngredients(player, recipe)) return;

        // Проверка и списание валюты (через getCost())
        int cost = recipe.getCost();
        if (cost > 0) {
            long playerMoney = NumismaticHelper.getTotalMoney(player);
            if (playerMoney < cost) return;
            NumismaticHelper.removeMoney(player, cost);
        }

        // Списание ингредиентов
        consumeIngredients(player, recipe);

        // Выдача результата
        ItemStack result = recipe.getResult().copy();
        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }

        // Синхронизация с клиентом
        player.currentScreenHandler.sendContentUpdates();
    }

    private boolean hasIngredients(PlayerEntity player, CraftRecipe recipe) {
        PlayerInventory inv = player.getInventory();
        for (Ingredient ing : recipe.getIngredients()) {
            int needed = ing.getCount(); // ✓ getCount(), не getAmount()
            ItemStack required = ing.getItem(); // ✓ getItem(), не getItemStack()
            int found = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, required)) {
                    found += stack.getCount();
                    if (found >= needed) break;
                }
            }
            if (found < needed) return false;
        }
        return true;
    }

    private void consumeIngredients(PlayerEntity player, CraftRecipe recipe) {
        PlayerInventory inv = player.getInventory();
        for (Ingredient ing : recipe.getIngredients()) {
            int remaining = ing.getCount();
            ItemStack required = ing.getItem();
            for (int i = 0; i < inv.size() && remaining > 0; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, required)) {
                    int take = Math.min(stack.getCount(), remaining);
                    stack.decrement(take);
                    remaining -= take;
                    inv.markDirty();
                }
            }
        }
    }

    // === ГЕТТЕРЫ ДЛЯ КЛИЕНТА ===
    public int getVillagerLevel() { return villagerLevel; }
    public String getProfessionId() { return professionId; }
    public int getVillagerEntityId() { return villagerEntityId; }

    // ✓ ИСПРАВЛЕНО: принимаем MinecraftClient, как ожидает VillagerInteractionScreen
    public VillagerEntity getVillagerFromWorld(MinecraftClient client) {
        if (client.world != null) {
            var entity = client.world.getEntityById(villagerEntityId);
            return entity instanceof VillagerEntity villager ? villager : null;
        }
        return null;
    }

    public void setVillager(VillagerEntity villager) { this.villager = villager; }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        // Main inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9; // +9 чтобы пропустить hotbar
                this.addSlot(new net.minecraft.screen.slot.Slot(playerInventory, index, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new net.minecraft.screen.slot.Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}