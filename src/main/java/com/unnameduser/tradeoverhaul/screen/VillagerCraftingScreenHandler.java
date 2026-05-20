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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class VillagerCraftingScreenHandler extends ScreenHandler {

    public static final int FIRST_MAIN_GRID_SLOT_INDEX = 0;

    private final int villagerLevel;
    private final String professionId;
    private final int villagerEntityId;
    private VillagerEntity villager;

    public static final int GRID_COLS = 6;
    public static final int GRID_ROWS = 6;
    public static final int ARMOR_SLOT_COUNT = 5;

    // === Клиентский конструктор (читает из буфера) ===
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.villagerEntityId = buf.readInt();    // ← 1: ID жителя
        this.professionId = buf.readString();     // ← 2: профессия
        this.villagerLevel = buf.readInt();       // ← 3: уровень
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

    public void handleCraftRequest(ServerPlayerEntity player, String recipeId, int selectedSlot) {
        CraftRecipe recipe = RecipeManager.getInstance().getCraftRecipeById(recipeId);
        if (recipe == null) {
            System.out.println("[TradeOverhaul] Recipe not found: " + recipeId);
            return;
        }

        if (recipe.getRequiredLevel() > villagerLevel) {
            System.out.println("[TradeOverhaul] Level too low: required " + recipe.getRequiredLevel() + ", villager level " + villagerLevel);
            return;
        }

        PlayerInventory inv = player.getInventory();

        // === СОХРАНЯЕМ NBT ДО СПИСАНИЯ ===
        ItemStack uniqueSource = null;
        NbtCompound savedNbt = null;

        if (recipe.shouldCopyNbt() && recipe.getUniqueIngredientIndex() >= 0) {
            int uniqueIdx = recipe.getUniqueIngredientIndex();
            if (uniqueIdx < recipe.getIngredients().size()) {
                Ingredient uniqueIng = recipe.getIngredients().get(uniqueIdx);
                ItemStack required = uniqueIng.getItem();

                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.getItem() == required.getItem()) {
                        if (stack.getCount() >= uniqueIng.getCount()) {

                            // === НОВАЯ ПРОВЕРКА: предмет должен иметь 100% прочность ===
                            if (stack.isDamageable()) {
                                int maxDamage = stack.getMaxDamage();
                                int currentDamage = stack.getDamage();
                                if (currentDamage > 0) {
                                    // Отправляем сообщение игроку
                                    player.sendMessage(Text.literal("§c[TradeOverhaul] Cannot craft with damaged item! Item must have 100% durability."), false);
                                    System.out.println("[TradeOverhaul] Unique item is damaged (" + currentDamage + "/" + maxDamage + "), cannot craft");
                                    return;
                                }
                            }
                            // === КОНЕЦ ПРОВЕРКИ ===

                            uniqueSource = stack;
                            if (uniqueSource.hasNbt()) {
                                savedNbt = uniqueSource.getNbt().copy();
                            }
                            break;
                        }
                    }
                }

                if (uniqueSource == null) {
                    player.sendMessage(Text.literal("§c[TradeOverhaul] Required unique item not found!"), false);
                    return;
                }
            }
        }

        // Проверка ингредиентов
        if (!hasIngredients(player, recipe)) {
            player.sendMessage(Text.literal("§c[TradeOverhaul] Missing ingredients!"), false);
            return;
        }

        // Проверка валюты
        int cost = recipe.getCost();
        if (cost > 0) {
            long playerMoney = NumismaticHelper.getTotalMoney(player);
            if (playerMoney < cost) {
                player.sendMessage(Text.literal("§c[TradeOverhaul] Not enough money! Need " + cost + " copper."), false);
                return;
            }
            NumismaticHelper.removeMoney(player, cost);
        }

        // Списание ингредиентов
        consumeIngredients(player, recipe);

        // Выдача результата
        ItemStack result = recipe.getResult().copy();

        // Копируем сохранённый NBT
        if (savedNbt != null) {
            result.setNbt(savedNbt);
            if (result.getNbt().contains("display")) {
                result.getNbt().getCompound("display").remove("Name");
            }
            System.out.println("[TradeOverhaul] NBT copied from saved copy");
        }

        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }

        player.currentScreenHandler.sendContentUpdates();
        System.out.println("[TradeOverhaul] Craft completed successfully!");
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
        // Слоты брони (39, 38, 37, 36, 40)
        int[] armorSlots = {39, 38, 37, 36, 40};
        for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
            this.addSlot(new net.minecraft.screen.slot.Slot(playerInventory, armorSlots[i], 0, 0));
        }

        // Основной инвентарь (6x6 = 36 слотов, индексы 0-35)
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int invIndex = row * GRID_COLS + col;
                if (invIndex < 36) {
                    this.addSlot(new net.minecraft.screen.slot.Slot(playerInventory, invIndex, 0, 0));
                }
            }
        }
    }
}