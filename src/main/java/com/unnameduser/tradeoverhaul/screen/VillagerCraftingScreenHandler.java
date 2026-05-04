package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class VillagerCraftingScreenHandler extends ScreenHandler {

    public static final int GRID_COLS = 6;
    public static final int GRID_ROWS = 6;
    public static final int PLAYER_GRID_SLOTS = GRID_COLS * GRID_ROWS;
    public static final int ARMOR_SLOT_COUNT = 5;

    public static final int FIRST_ARMOR_SLOT_INDEX = 0;
    public static final int FIRST_MAIN_GRID_SLOT_INDEX = ARMOR_SLOT_COUNT;
    public static final int TOTAL_SLOTS = FIRST_MAIN_GRID_SLOT_INDEX + PLAYER_GRID_SLOTS;

    private final PlayerInventory playerInventory;

    // Данные о жителе и профессии
    private int villagerEntityId;
    private String professionId;
    private int villagerLevel;

    // Конструктор для сервера
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villagerEntityId = villager.getId();
        this.professionId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString();
        this.villagerLevel = villager.getVillagerData().getLevel();
        setupSlots();
    }

    // Конструктор для клиента
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villagerEntityId = buf.readInt();
        this.professionId = buf.readString();
        this.villagerLevel = buf.readInt();
        setupSlots();
    }

    private void setupSlots() {
        int[] armorSlots = {39, 38, 37, 36, 40};
        for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
            this.addSlot(new Slot(playerInventory, armorSlots[i], 0, 0));
        }

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int invIndex = row * GRID_COLS + col;
                if (invIndex < 36) {
                    this.addSlot(new Slot(playerInventory, invIndex, 0, 0));
                }
            }
        }
    }

    public void handleCraftRequest(ServerPlayerEntity player, String recipeId, int selectedSlot) {
        System.out.println("[TradeOverhaul] handleCraftRequest called: recipeId=" + recipeId + ", selectedSlot=" + selectedSlot);

        CraftRecipe recipe = RecipeManager.getInstance().getCraftRecipeById(recipeId);
        if (recipe == null) {
            System.out.println("[TradeOverhaul] Recipe not found: " + recipeId);
            return;
        }

        System.out.println("[TradeOverhaul] Recipe found: " + recipe.getId() + ", unique_index=" + recipe.getUniqueIngredientIndex());

        if (recipe.getRequiredLevel() > villagerLevel) {
            System.out.println("[TradeOverhaul] Villager level too low: " + villagerLevel + " < " + recipe.getRequiredLevel());
            return;
        }

        PlayerInventory inv = player.getInventory();
        List<Ingredient> ingredients = recipe.getIngredients();
        int uniqueIndex = recipe.getUniqueIngredientIndex();

        if (ingredients.isEmpty()) {
            System.out.println("[TradeOverhaul] Recipe has no ingredients");
            return;
        }

        // Поиск уникального предмета в инвентаре (если есть уникальный ингредиент)
        ItemStack uniqueStack = null;
        int uniqueFoundSlot = -1;

        if (uniqueIndex >= 0) {
            Ingredient uniqueIngredient = ingredients.get(uniqueIndex);
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.getItem() == uniqueIngredient.getItem().getItem() && stack.getCount() >= uniqueIngredient.getCount()) {
                    uniqueStack = stack;
                    uniqueFoundSlot = i;
                    break;
                }
            }

            if (uniqueStack == null) {
                System.out.println("[TradeOverhaul] No unique item found in inventory: " +
                        uniqueIngredient.getItem().getItem().getName().getString());
                return;
            }

            System.out.println("[TradeOverhaul] Found unique item: " + uniqueStack.getItem().getName().getString() +
                    " in slot " + uniqueFoundSlot);
        }

        // Проверяем наличие всех ингредиентов
        int[] requiredCounts = new int[ingredients.size()];
        for (int i = 0; i < ingredients.size(); i++) {
            requiredCounts[i] = ingredients.get(i).getCount();
        }

        // Если есть уникальный предмет, уменьшаем его требуемое количество
        if (uniqueIndex >= 0 && uniqueStack != null) {
            requiredCounts[uniqueIndex] = Math.max(0, requiredCounts[uniqueIndex] - uniqueStack.getCount());
        }

        // Проверяем наличие расходников
        for (int i = 0; i < ingredients.size(); i++) {
            if (i == uniqueIndex) continue;
            if (requiredCounts[i] <= 0) continue;

            Ingredient ing = ingredients.get(i);
            int found = 0;
            for (int slot = 0; slot < inv.size(); slot++) {
                if (slot == uniqueFoundSlot) continue;
                ItemStack stack = inv.getStack(slot);
                if (stack.getItem() == ing.getItem().getItem()) {
                    found += stack.getCount();
                    if (found >= requiredCounts[i]) break;
                }
            }
            if (found < requiredCounts[i]) {
                System.out.println("[TradeOverhaul] Missing ingredients for: " + ing.getItem().getItem().getName().getString());
                return;
            }
        }

        // Проверка денег
        int playerMoney = NumismaticHelper.getTotalMoney(player);
        if (playerMoney < recipe.getCost()) {
            System.out.println("[TradeOverhaul] Not enough money: " + playerMoney + " < " + recipe.getCost());
            return;
        }

        // ВЫПОЛНЯЕМ КРАФТ
        System.out.println("[TradeOverhaul] Crafting...");

        // Снимаем деньги
        NumismaticHelper.removeMoney(player, recipe.getCost());

        // Удаляем уникальный предмет (если есть)
        if (uniqueIndex >= 0 && uniqueFoundSlot >= 0) {
            Ingredient uniqueIngredient = ingredients.get(uniqueIndex);
            inv.getStack(uniqueFoundSlot).decrement(uniqueIngredient.getCount());
        }

        // Удаляем расходники
        for (int i = 0; i < ingredients.size(); i++) {
            if (i == uniqueIndex) continue;
            Ingredient ing = ingredients.get(i);
            int toRemove = ing.getCount();
            for (int slot = 0; slot < inv.size() && toRemove > 0; slot++) {
                if (slot == uniqueFoundSlot) continue;
                ItemStack stack = inv.getStack(slot);
                if (stack.getItem() == ing.getItem().getItem()) {
                    int remove = Math.min(toRemove, stack.getCount());
                    stack.decrement(remove);
                    toRemove -= remove;
                }
            }
        }

        // Создаём результат
        ItemStack result = recipe.getResult().copy();
        if (recipe.shouldCopyNbt() && uniqueStack != null && uniqueStack.hasNbt()) {
            result.setNbt(uniqueStack.getNbt().copy());
            if (result.getNbt().contains("display")) {
                result.getNbt().getCompound("display").remove("Name");
            }
        }

        // Добавляем результат
        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }

        System.out.println("[TradeOverhaul] Craft completed! Recipe: " + recipeId);
    }

    public int getVillagerEntityId() { return villagerEntityId; }
    public String getProfessionId() { return professionId; }
    public int getVillagerLevel() { return villagerLevel; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
    }
}