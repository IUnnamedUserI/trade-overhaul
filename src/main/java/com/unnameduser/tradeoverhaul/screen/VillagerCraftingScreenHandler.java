package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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
    private final VillagerEntity villager;

    // Данные о жителе и профессии
    private int villagerEntityId;
    private String professionId;
    private int villagerLevel;

    // Конструктор для сервера
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villager = villager;
        this.villagerEntityId = villager.getId();
        this.professionId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString();
        this.villagerLevel = villager.getVillagerData().getLevel();
        setupSlots();
    }

    // Конструктор для клиента
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villager = null;
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
        // ... (твой существующий код, без изменений)
    }

    public int getVillagerEntityId() { return villagerEntityId; }
    public String getProfessionId() { return professionId; }
    public int getVillagerLevel() { return villagerLevel; }
    public VillagerEntity getVillager() { return villager; }

    public VillagerEntity getVillagerFromWorld(MinecraftClient client) {
        if (client.world != null) {
            Entity entity = client.world.getEntityById(villagerEntityId);
            if (entity instanceof VillagerEntity) {
                return (VillagerEntity) entity;
            }
        }
        return null;
    }

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