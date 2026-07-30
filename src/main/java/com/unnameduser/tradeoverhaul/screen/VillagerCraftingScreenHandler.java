package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.config.DisassemblyConfig;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.RepairAllSyncS2CPacket;
import com.unnameduser.tradeoverhaul.common.network.RepairSyncS2CPacket;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.common.trade.TradePricing;
import com.unnameduser.tradeoverhaul.common.util.ItemTagHelper;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VillagerCraftingScreenHandler extends ScreenHandler {

    // === КОНСТАНТЫ СЛОТОВ ===
    public static final int ARMOR_SLOT_COUNT = 5;
    public static final int GRID_COLS = 6;
    public static final int GRID_ROWS = 6;
    public static final int PLAYER_GRID_SLOTS = GRID_COLS * GRID_ROWS;

    public static final int FIRST_MAIN_GRID_SLOT_INDEX = ARMOR_SLOT_COUNT;
    public static final int FIRST_VILLAGER_TRADE_SLOT = FIRST_MAIN_GRID_SLOT_INDEX + PLAYER_GRID_SLOTS;
    public static final int VILLAGER_TRADE_SLOTS = PLAYER_GRID_SLOTS;

    private final int villagerLevel;
    private final String professionId;
    private final int villagerEntityId;
    private VillagerEntity villager;

    // === Торговая логика ===
    private final VillagerInventoryComponent villagerInventory;
    private final PlayerInventory playerInventory;
    private final ProfessionTradeFile professionFile;
    private final int[] clientWalletHolder = new int[1];

    // Данные о профессии для клиента
    private int clientProfessionLevel = 1;
    private int clientProfessionExperience = 0;
    private int clientProfessionTradesCompleted = 0;
    private float clientFractionalXp = 0f;
    private Map<String, Integer> clientSoldItemsTracker = new HashMap<>();
    private Map<String, Float> clientDamageReputation = new HashMap<>();

    private int syncedWallet = 0;

    // === Клиентский конструктор ===
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villager = null;

        // 1. ID жителя
        this.villagerEntityId = buf.readInt();

        // 2. Профессия
        this.professionId = buf.readString();

        // 3. Уровень (ванильный)
        this.villagerLevel = buf.readInt();

        // 4. Читаем данные опыта
        this.clientProfessionLevel = buf.readInt();
        this.clientProfessionExperience = buf.readInt();
        this.clientProfessionTradesCompleted = buf.readInt();
        this.clientFractionalXp = buf.readFloat();

        // Читаем трекер проданных предметов
        NbtCompound soldTracker = buf.readNbt();
        if (soldTracker != null) {
            this.clientSoldItemsTracker.clear();
            for (String key : soldTracker.getKeys()) {
                this.clientSoldItemsTracker.put(key, soldTracker.getInt(key));
            }
        }

        // Читаем репутацию урона
        NbtCompound damageRep = buf.readNbt();
        if (damageRep != null) {
            this.clientDamageReputation.clear();
            for (String key : damageRep.getKeys()) {
                this.clientDamageReputation.put(key, damageRep.getFloat(key));
            }
        }

        // 5. Инвентарь жителя
        this.villagerInventory = new VillagerInventoryComponent();
        int inventorySize = buf.readInt();
        for (int i = 0; i < inventorySize; i++) {
            villagerInventory.setStack(i, buf.readItemStack());
        }

        this.clientWalletHolder[0] = 0;

        Identifier pid = Identifier.tryParse(professionId);
        this.professionFile = pid != null ? TradeConfigLoader.getProfession(pid) : null;

        addPlayerInventory(playerInventory);
        addVillagerTradeSlots();
        initPropertyDelegate();
    }

    // === Серверный конструктор ===
    public VillagerCraftingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(TradeOverhaulMod.VILLAGER_CRAFTING_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.villager = villager;
        this.villagerLevel = villager.getVillagerData().getLevel();
        this.professionId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString();
        this.villagerEntityId = villager.getId();

        Identifier pid = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
        this.professionFile = TradeConfigLoader.getProfession(pid);

        if (villager instanceof VillagerTradeData data) {
            this.villagerInventory = data.tradeOverhaul$getInventory();
            this.clientWalletHolder[0] = data.tradeOverhaul$getCurrency().getTotalCopper();
            this.clientProfessionLevel = data.tradeOverhaul$getProfession().getLevel();
            this.clientProfessionExperience = data.tradeOverhaul$getProfession().getExperience();
            this.clientProfessionTradesCompleted = data.tradeOverhaul$getProfession().getTradesCompleted();
            this.clientFractionalXp = data.tradeOverhaul$getProfession().getFractionalXpAccumulator();
            this.clientSoldItemsTracker = new HashMap<>(data.tradeOverhaul$getProfession().soldItemsTracker);
            this.clientDamageReputation = new HashMap<>(data.tradeOverhaul$getProfession().damageReputation);
        } else {
            this.villagerInventory = new VillagerInventoryComponent();
            this.clientWalletHolder[0] = 0;
        }

        addPlayerInventory(playerInventory);
        addVillagerTradeSlots();
        initPropertyDelegate();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return villager == null || player.squaredDistanceTo(villager) <= 64.0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    // === СЛОТЫ ===
    private void addPlayerInventory(PlayerInventory playerInventory) {
        int[] armorSlots = {39, 38, 37, 36, 40};
        for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
            this.addSlot(new Slot(playerInventory, armorSlots[i], 0, 0));
        }
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int invIndex = row * GRID_COLS + col;
                if (invIndex < PLAYER_GRID_SLOTS) {
                    this.addSlot(new Slot(playerInventory, invIndex, 0, 0));
                }
            }
        }
    }

    private void addVillagerTradeSlots() {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int invIndex = row * GRID_COLS + col;
                this.addSlot(new com.unnameduser.tradeoverhaul.client.gui.VillagerReadOnlySlot(
                        villagerInventory, invIndex, 0, 0));
            }
        }
    }

    private void initPropertyDelegate() {
        this.addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index == 0) {
                    if (villager instanceof VillagerTradeData data) {
                        return data.tradeOverhaul$getCurrency().getTotalCopper();
                    }
                    return clientWalletHolder[0];
                }
                return 0;
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) clientWalletHolder[0] = value;
            }
            @Override
            public int size() { return 1; }
        });
    }

    // === ГЕТТЕРЫ ===
    public int getVillagerLevel() { return villagerLevel; }
    public String getProfessionId() { return professionId; }
    public int getVillagerEntityId() { return villagerEntityId; }

    public VillagerEntity getVillagerFromWorld(MinecraftClient client) {
        if (client.world != null) {
            var entity = client.world.getEntityById(villagerEntityId);
            return entity instanceof VillagerEntity v ? v : null;
        }
        return null;
    }

    public void setVillager(VillagerEntity villager) { this.villager = villager; }

    public int getSyncedWallet() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getCurrency().getTotalCopper();
        }
        return clientWalletHolder[0];
    }

    public int getProfessionLevel() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getProfession().getLevel();
        }
        return clientProfessionLevel; // ← теперь здесь правильное значение
    }

    public String getProfessionLevelName() {
        int level = getProfessionLevel();
        return switch (level) {
            case 2 -> "apprentice";
            case 3 -> "journeyman";
            case 4 -> "expert";
            case 5 -> "master";
            default -> "novice";
        };
    }

    public int getProfessionExperience() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getProfession().getExperience();
        }
        return clientProfessionExperience; // ← теперь здесь правильное значение
    }

    public int getProfessionTradesCompleted() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getProfession().getTradesCompleted();
        }
        return clientProfessionTradesCompleted;
    }

    public float getProfessionFractionalXp() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getProfession().getFractionalXpAccumulator();
        }
        return clientFractionalXp;
    }

    public int getXpForNextLevel() {
        int level = getProfessionLevel();
        if (level >= 5) return 0;
        int[] xpRequired = {0, 10, 30, 60, 100};
        return xpRequired[level];
    }

    public float getClientFractionalXp() {
        if (villager instanceof VillagerTradeData data) {
            return data.tradeOverhaul$getProfession().getFractionalXpAccumulator();
        }
        return clientFractionalXp;
    }

    public int getClientBuyPrice(int screenSlot) {
        Slot sl = getSlot(screenSlot);
        if (sl == null || sl.getIndex() < 0 || sl.getIndex() >= villagerInventory.size() || professionFile == null) return 0;
        ItemStack stack = villagerInventory.getStack(sl.getIndex());
        if (stack.isEmpty()) return 0;

        int price = TradePricing.getBuyPrice(stack, professionFile);

        if (playerInventory.player != null) {
            String playerId = playerInventory.player.getUuidAsString();
            float totalDamage = clientDamageReputation.getOrDefault(playerId, 0f);
            if (totalDamage > 0) {
                var settings = TradeConfigLoader.getSettings();
                double repPercent = Math.min(totalDamage * settings.damageReputationPercentPerHP, settings.damageReputationMaxPercent);
                if (repPercent > 0) {
                    price = (int) Math.ceil(price * (1.0 + repPercent / 100.0));
                }
            }
        }
        return price;
    }

    public int getClientSellPrice(ItemStack stack) {
        if (stack.isEmpty() || professionFile == null) return 0;
        int price = TradePricing.getSellPrice(stack, professionFile);
        return TradePricing.applyDurabilityPriceModifier(price, stack, TradeConfigLoader.getSettings());
    }

    public boolean canVillagerBuyItem(ItemStack stack) {
        if (stack.isEmpty() || professionFile == null) return false;
        if (villager == null) {
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            return professionFile.isItemSoldByVillager(itemId);
        }
        return TradePricing.canVillagerBuyItem(stack, villager, professionFile);
    }

    public boolean clientHasInventorySpaceForStack(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) return true;
        var inv = playerInventory.player.getInventory();
        int remaining = count;
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack slotStack = inv.getStack(i);
            if (ItemStack.canCombine(slotStack, stack)) {
                int space = slotStack.getMaxCount() - slotStack.getCount();
                remaining -= Math.min(space, remaining);
            }
        }
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            if (inv.getStack(i).isEmpty()) {
                remaining -= Math.min(stack.getMaxCount(), remaining);
            }
        }
        return remaining <= 0;
    }

    public float getExpectedXpForBuy(int screenSlot, int amount) {
        Slot sl = getSlot(screenSlot);
        if (sl == null || sl.getIndex() < 0 || sl.getIndex() >= villagerInventory.size()) return 0f;
        ItemStack stack = villagerInventory.getStack(sl.getIndex());
        if (stack.isEmpty() || professionFile == null) return 0f;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (ItemTagHelper.isPlayerSold(stack)) return 0f;

        Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
        float multiplier = (poolMultiplier != null) ? poolMultiplier : com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
        return multiplier * amount;
    }

    public float getExpectedXpForSell(ItemStack stack, int amount) {
        if (stack.isEmpty() || professionFile == null) return 0f;
        if (ItemTagHelper.isVillagerSold(stack)) return 0f;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (!professionFile.isItemSoldByVillager(itemId)) return 0f;

        Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
        float multiplier = (poolMultiplier != null) ? poolMultiplier : com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
        return multiplier * amount;
    }

    // === ЛОГИКА КРАФТА ===
    public void handleCraftRequest(ServerPlayerEntity player, String recipeId, int selectedSlot) {
        CraftRecipe recipe = RecipeManager.getInstance().getCraftRecipeById(recipeId);
        if (recipe == null) { System.out.println("[TradeOverhaul] Recipe not found: " + recipeId); return; }
        if (recipe.getRequiredLevel() > villagerLevel) { System.out.println("[TradeOverhaul] Level too low"); return; }

        PlayerInventory inv = player.getInventory();
        ItemStack uniqueSource = null;
        NbtCompound savedNbt = null;

        if (recipe.shouldCopyNbt() && recipe.getUniqueIngredientIndex() >= 0) {
            int uniqueIdx = recipe.getUniqueIngredientIndex();
            if (uniqueIdx < recipe.getIngredients().size()) {
                Ingredient uniqueIng = recipe.getIngredients().get(uniqueIdx);
                ItemStack required = uniqueIng.getItem();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.getItem() == required.getItem() && stack.getCount() >= uniqueIng.getCount()) {
                        if (stack.isDamageable() && stack.getDamage() > 0) {
                            player.sendMessage(Text.literal("§c[TradeOverhaul] Cannot craft with damaged item!"), false);
                            return;
                        }
                        uniqueSource = stack;
                        if (uniqueSource.hasNbt()) savedNbt = uniqueSource.getNbt().copy();
                        break;
                    }
                }
                if (uniqueSource == null) { player.sendMessage(Text.literal("§c[TradeOverhaul] Required unique item not found!"), false); return; }
            }
        }
        if (!hasIngredients(player, recipe)) { player.sendMessage(Text.literal("§c[TradeOverhaul] Missing ingredients!"), false); return; }

        int cost = recipe.getCost();
        if (cost > 0) {
            long playerMoney = NumismaticHelper.getTotalMoney(player);
            if (playerMoney < cost) { player.sendMessage(Text.literal("§c[TradeOverhaul] Not enough money!"), false); return; }
            NumismaticHelper.removeMoney(player, cost);
        }
        consumeIngredients(player, recipe);
        ItemStack result = recipe.getResult().copy();
        if (savedNbt != null) { result.setNbt(savedNbt); if (result.getNbt().contains("display")) result.getNbt().getCompound("display").remove("Name"); }
        if (!player.getInventory().insertStack(result)) player.dropItem(result, false);
        player.currentScreenHandler.sendContentUpdates();
    }

    private boolean hasIngredients(PlayerEntity player, CraftRecipe recipe) {
        PlayerInventory inv = player.getInventory();
        for (Ingredient ing : recipe.getIngredients()) {
            int needed = ing.getCount(), found = 0;
            ItemStack required = ing.getItem();
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

    // === ЛОГИКА РЕМОНТА ===
    public void handleRepairRequest(ServerPlayerEntity player, int slotIndex) {
        PlayerInventory inv = player.getInventory();
        if (slotIndex < 0 || slotIndex >= inv.size()) return;
        ItemStack stack = inv.getStack(slotIndex);
        if (stack.isEmpty() || !stack.isDamageable() || stack.getDamage() <= 0) return;
        int repairCost = stack.getDamage() * 2;
        if (NumismaticHelper.getTotalMoney(player) < repairCost) return;
        NumismaticHelper.removeMoney(player, repairCost);
        stack.setDamage(0);
        player.currentScreenHandler.sendContentUpdates();
        RepairSyncS2CPacket response = new RepairSyncS2CPacket(this.syncId, slotIndex);
        PacketByteBuf buf = PacketByteBufs.create();
        RepairSyncS2CPacket.encode(response, buf);
        ServerPlayNetworking.send(player, new Identifier(TradeOverhaulMod.MOD_ID, "repair_sync"), buf);
    }

    public void handleRepairAllRequest(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<Integer> slots = new ArrayList<>(); int totalCost = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > 0) {
                totalCost += stack.getDamage() * 2;
                slots.add(i);
            }
        }
        if (slots.isEmpty() || NumismaticHelper.getTotalMoney(player) < totalCost) return;
        NumismaticHelper.removeMoney(player, totalCost);
        for (int slot : slots) { ItemStack stack = inv.getStack(slot); if (!stack.isEmpty() && stack.isDamageable()) stack.setDamage(0); }
        player.currentScreenHandler.sendContentUpdates();
        RepairAllSyncS2CPacket response = new RepairAllSyncS2CPacket(this.syncId);
        PacketByteBuf buf = PacketByteBufs.create();
        RepairAllSyncS2CPacket.encode(response, buf);
        ServerPlayNetworking.send(player, new Identifier(TradeOverhaulMod.MOD_ID, "repair_all_sync"), buf);
    }

    // === ЛОГИКА ПОКУПКИ ===
    public void handleTradePurchase(ServerPlayerEntity player, int tradeSlotIndex, boolean buyWholeStack, boolean buyTen) {
        Slot sl = getSlot(tradeSlotIndex);
        if (sl == null || sl.getIndex() < 0 || sl.getIndex() >= villagerInventory.size() || professionFile == null) return;

        ItemStack villagerStack = villagerInventory.getStack(sl.getIndex());
        if (villagerStack.isEmpty()) return;

        String itemId = Registries.ITEM.getId(villagerStack.getItem()).toString();
        boolean wasPlayerSold = ItemTagHelper.isPlayerSold(villagerStack);

        int price = TradePricing.getBuyPrice(villagerStack, professionFile);
        if (villager instanceof VillagerTradeData data) {
            price = TradePricing.applyDamageReputation(price, player.getUuidAsString(), data.tradeOverhaul$getProfession(), TradeConfigLoader.getSettings());
        }
        if (price <= 0) price = 1;

        int wantToBuy = buyWholeStack ? villagerStack.getCount() : (buyTen ? Math.min(10, villagerStack.getCount()) : 1);
        int maxCanBuy = NumismaticHelper.getTotalMoney(player) / price;
        int toBuy = Math.min(wantToBuy, maxCanBuy);
        if (toBuy <= 0) return;

        int maxFit = maxInventorySpaceForStack(player, villagerStack, toBuy);
        if (maxFit <= 0) return;
        if (maxFit < toBuy) { toBuy = maxFit; }

        ItemStack copy = villagerStack.copy();
        copy.setCount(toBuy);
        ItemTagHelper.markAsVillagerSold(copy);
        if (!player.getInventory().insertStack(copy)) return;

        int totalCost = toBuy * price;
        NumismaticHelper.removeMoney(player, totalCost);

        villagerStack.decrement(toBuy);

        if (villager instanceof VillagerTradeData data) {
            data.tradeOverhaul$getCurrency().addMoney(totalCost);
            data.tradeOverhaul$getProfession().markAsTraded();

            TradeOverhaulMod.LOGGER.info("[XP DEBUG] Purchase: itemId={}, amount={}, wasPlayerSold={}, professionFile={}",
                    itemId, toBuy, wasPlayerSold, professionFile != null ? professionFile.profession : "null");

            if (!wasPlayerSold) {
                data.tradeOverhaul$getProfession().applyXpFromSale(itemId, toBuy, professionFile);
                int modLevel = data.tradeOverhaul$getProfession().getLevel();
                if (modLevel > villager.getVillagerData().getLevel()) {
                    villager.setVillagerData(villager.getVillagerData().withLevel(modLevel));
                }
            }
            NbtCompound soldItemsTracker = new NbtCompound();
            for (Map.Entry<String, Integer> e : data.tradeOverhaul$getProfession().soldItemsTracker.entrySet()) {
                soldItemsTracker.putInt(e.getKey(), e.getValue());
            }
            com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
                    player, this.syncId, data.tradeOverhaul$getProfession().getLevel(),
                    data.tradeOverhaul$getProfession().getExperience(), data.tradeOverhaul$getProfession().getTradesCompleted(),
                    data.tradeOverhaul$getProfession().getFractionalXpAccumulator(), soldItemsTracker);
        }
        sendContentUpdates();
        com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendInventorySync(player, this.syncId, villagerInventory);
    }

    // === ЛОГИКА ПРОДАЖИ ===
    public void handleTradeSell(ServerPlayerEntity player, int playerSlotIndex, boolean sellWholeStack, boolean sellTen) {
        Slot sl = getSlot(playerSlotIndex);
        if (sl == null || sl.inventory != playerInventory || !(villager instanceof VillagerTradeData data) || professionFile == null) return;

        ItemStack item = sl.getStack();
        if (item.isEmpty() || !TradePricing.canVillagerBuyItem(item, villager, professionFile)) return;

        String itemId = Registries.ITEM.getId(item.getItem()).toString();
        boolean wasVillagerSold = ItemTagHelper.isVillagerSold(item);
        boolean isSoldByVillager = professionFile.isItemSoldByVillager(itemId);

        int sellPrice = TradePricing.applyDurabilityPriceModifier(TradePricing.getSellPrice(item, professionFile), item, TradeConfigLoader.getSettings());
        if (sellPrice <= 0) sellPrice = 1;

        int wantToSell = sellWholeStack ? item.getCount() : (sellTen ? Math.min(10, item.getCount()) : 1);
        int villagerMoney = data.tradeOverhaul$getCurrency().getTotalCopper();
        int maxCanBuy = villagerMoney / sellPrice;
        int toSell = Math.min(wantToSell, maxCanBuy);
        if (toSell <= 0) return;

        int maxFit = maxItemsVillagerCanAccept(item, toSell);
        if (maxFit < toSell) { toSell = maxFit; }
        if (toSell <= 0) return;

        if (!insertItemCountIntoVillager(item, toSell)) return;
        item.decrement(toSell);

        int totalEarned = toSell * sellPrice;
        if (!data.tradeOverhaul$getCurrency().removeMoney(totalEarned)) return;
        data.tradeOverhaul$getProfession().markAsTraded();

        TradeOverhaulMod.LOGGER.info("[XP DEBUG] Sell: itemId={}, amount={}, wasVillagerSold={}, isSoldByVillager={}",
                itemId, toSell, wasVillagerSold, isSoldByVillager);

        if (!wasVillagerSold && isSoldByVillager) {
            data.tradeOverhaul$getProfession().applyXpFromSale(itemId, toSell, professionFile);
            NbtCompound soldItemsTracker = new NbtCompound();
            for (Map.Entry<String, Integer> e : data.tradeOverhaul$getProfession().soldItemsTracker.entrySet()) {
                soldItemsTracker.putInt(e.getKey(), e.getValue());
            }
            com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
                    player, this.syncId, data.tradeOverhaul$getProfession().getLevel(),
                    data.tradeOverhaul$getProfession().getExperience(), data.tradeOverhaul$getProfession().getTradesCompleted(),
                    data.tradeOverhaul$getProfession().getFractionalXpAccumulator(), soldItemsTracker);
        }
        NumismaticHelper.addMoney(player, totalEarned);
        sendContentUpdates();
        com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendInventorySync(player, this.syncId, villagerInventory);
    }

    public void handleDisassembleRequest(ServerPlayerEntity player, int handlerSlotIndex) {
        if (!DisassemblyConfig.isLoaded()) DisassemblyConfig.load();

        int playerInvIndex;
        if (handlerSlotIndex < 5) {
            int[] armorMap = {39, 38, 37, 36, 40};
            playerInvIndex = armorMap[handlerSlotIndex];
        } else {
            playerInvIndex = handlerSlotIndex - 5;
        }

        if (playerInvIndex < 0 || playerInvIndex >= player.getInventory().size()) return;

        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getStack(playerInvIndex);
        if (item.isEmpty()) return;
        if (!DisassemblyConfig.isDisassemblyAllowed(item.getItem())) return;

        CraftingRecipe recipe = findCraftingRecipe(item, player.getWorld());
        if (recipe == null) return;

        List<ItemStack> components = new ArrayList<>();
        for (net.minecraft.recipe.Ingredient ing : recipe.getIngredients()) {
            ItemStack[] matches = ing.getMatchingStacks();
            if (matches != null && matches.length > 0) components.add(matches[0].copy());
        }

        int cost = components.size() * 5;
        if (NumismaticHelper.getTotalMoney(player) < cost) return;

        float dropChance = 1.0f;
        if (item.isDamageable() && item.getMaxDamage() > 0) {
            dropChance = 1.0f - ((float) item.getDamage() / item.getMaxDamage());
            dropChance = net.minecraft.util.math.MathHelper.clamp(dropChance, 0.0f, 1.0f);
        }

        NumismaticHelper.removeMoney(player, cost);
        item.decrement(1);

        for (ItemStack comp : components) {
            if (Math.random() < dropChance) {
                ItemStack drop = comp.copy();
                drop.setCount(1);
                if (!player.getInventory().insertStack(drop)) player.dropItem(drop, false);
            }
        }

        if (villager instanceof VillagerTradeData data) {
            data.tradeOverhaul$getCurrency().addMoney(cost);
        }
        sendContentUpdates();
    }

    private CraftingRecipe findCraftingRecipe(ItemStack item, World world) {
        Item targetItem = item.getItem();
        for (var recipe : world.getRecipeManager().values()) {
            if (recipe instanceof CraftingRecipe craftingRecipe) {
                ItemStack output = craftingRecipe.getOutput(world.getRegistryManager());
                if (output.getItem() == targetItem && output.getCount() == 1) {
                    return craftingRecipe;
                }
            }
        }
        return null;
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    private boolean insertItemCountIntoVillager(ItemStack template, int count) {
        ItemStack remaining = template.copy(); remaining.setCount(count);
        ItemTagHelper.markAsPlayerSold(remaining);
        int maxStack = getMaxStackSizeForItem(template);

        for (int i = 0; i < villagerInventory.size() && !remaining.isEmpty(); i++) {
            ItemStack slotStack = villagerInventory.getStack(i);
            if (ItemStack.areItemsEqual(slotStack, remaining) && slotStack.getCount() < maxStack && canStackItems(slotStack, remaining)) {
                int put = Math.min(remaining.getCount(), maxStack - slotStack.getCount());
                slotStack.increment(put); remaining.decrement(put);
            }
        }
        while (!remaining.isEmpty()) {
            boolean moved = false;
            for (int i = 0; i < villagerInventory.size(); i++) {
                if (villagerInventory.getStack(i).isEmpty()) {
                    int put = Math.min(remaining.getCount(), maxStack);
                    villagerInventory.setStack(i, remaining.copyWithCount(put));
                    remaining.decrement(put); moved = true; break;
                }
            }
            if (!moved) return false;
        }
        return true;
    }

    private int maxInventorySpaceForStack(PlayerEntity player, ItemStack template, int want) {
        int maxStack = template.getMaxCount(), space = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) space += maxStack;
            else if (ItemStack.areItemsEqual(s, template)) space += maxStack - s.getCount();
            if (space >= want) return want;
        }
        return space;
    }

    private int maxItemsVillagerCanAccept(ItemStack template, int want) {
        int maxStack = template.getMaxCount(), space = 0;
        for (int i = 0; i < villagerInventory.size(); i++) {
            ItemStack s = villagerInventory.getStack(i);
            if (s.isEmpty()) space += maxStack;
            else if (ItemStack.areItemsEqual(s, template)) space += maxStack - s.getCount();
            if (space >= want) return want;
        }
        return space;
    }

    private int getMaxStackSizeForItem(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxCount() <= 1) return 1;
        if (stack.hasEnchantments() || stack.hasCustomName() || stack.isDamaged()) return 1;
        return stack.getMaxCount();
    }

    private boolean canStackItems(ItemStack s1, ItemStack s2) {
        if (!ItemStack.areItemsEqual(s1, s2)) return false;
        if (s1.hasEnchantments() || s2.hasEnchantments() || s1.hasCustomName() || s2.hasCustomName() || s1.isDamaged() || s2.isDamaged()) return false;
        return ItemTagHelper.isPlayerSold(s1) == ItemTagHelper.isPlayerSold(s2);
    }

    public void updateClientProfessionData(int level, int experience, int tradesCompleted, float fractionalXp, NbtCompound soldItemsTracker) {
        this.clientProfessionLevel = level;
        this.clientProfessionExperience = experience;
        this.clientProfessionTradesCompleted = tradesCompleted;
        this.clientFractionalXp = fractionalXp;

        if (soldItemsTracker != null) {
            this.clientSoldItemsTracker.clear();
            for (String key : soldItemsTracker.getKeys()) {
                this.clientSoldItemsTracker.put(key, soldItemsTracker.getInt(key));
            }
        }
        this.sendContentUpdates();
    }

    public boolean isItemInBuyPool(ItemStack stack) {
        if (stack.isEmpty() || professionFile == null) return false;
        String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
        return professionFile.isItemSoldByVillager(itemId);
    }

    public void handleCraftPanelBuyRequest(ServerPlayerEntity player, String itemId, int amount) {
        if (!(this.villager instanceof VillagerTradeData data) || professionFile == null) return;

        Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
        if (item == null) return;

        int slotIndex = -1;
        for (int i = 0; i < villagerInventory.size(); i++) {
            if (ItemStack.areItemsEqual(villagerInventory.getStack(i), new ItemStack(item))) {
                slotIndex = i;
                break;
            }
        }
        if (slotIndex == -1 || villagerInventory.getStack(slotIndex).getCount() < amount) return;

        ItemStack template = villagerInventory.getStack(slotIndex);
        int price = TradePricing.getBuyPrice(template, professionFile);
        if (price <= 0 || NumismaticHelper.getTotalMoney(player) < price) return;

        TradeOverhaulMod.LOGGER.info("[CraftBuy] Player {} requested {} x{} for {} copper",
                player.getName().getString(), itemId, amount, price);

        ItemStack toGive = template.copy();
        toGive.setCount(amount);
        ItemTagHelper.markAsVillagerSold(toGive);
        if (!player.getInventory().insertStack(toGive)) return;

        villagerInventory.getStack(slotIndex).decrement(amount);
        NumismaticHelper.removeMoney(player, price);
        data.tradeOverhaul$getCurrency().addMoney(price);
        data.tradeOverhaul$getProfession().markAsTraded();

        boolean wasPlayerSold = ItemTagHelper.isPlayerSold(toGive);
        if (!wasPlayerSold) {
            data.tradeOverhaul$getProfession().applyXpFromSale(itemId, amount, professionFile);
            int modLevel = data.tradeOverhaul$getProfession().getLevel();
            if (modLevel > this.villager.getVillagerData().getLevel()) {
                this.villager.setVillagerData(this.villager.getVillagerData().withLevel(modLevel));
            }
            com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
                    player, this.syncId, data.tradeOverhaul$getProfession().getLevel(),
                    data.tradeOverhaul$getProfession().getExperience(), data.tradeOverhaul$getProfession().getTradesCompleted(),
                    data.tradeOverhaul$getProfession().getFractionalXpAccumulator(), null);
        }

        sendContentUpdates();
        com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendInventorySync(player, this.syncId, villagerInventory);
    }

    public boolean hasItemInTradeInventory(ItemStack template) {
        if (template.isEmpty()) return false;
        for (int i = FIRST_VILLAGER_TRADE_SLOT; i < slots.size(); i++) {
            Slot sl = slots.get(i);
            if (ItemStack.areItemsEqual(sl.getStack(), template) && sl.getStack().getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public VillagerEntity getVillager() { return villager; }

    public VillagerInventoryComponent getVillagerInventory() {
        return villagerInventory;
    }

    public void handleTradeRequest(int slotIndex, int amount, boolean buying, ServerPlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return;

        Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        if (!(villager instanceof VillagerTradeData data)) return;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();

        if (buying) {
            // ============ ПОКУПКА у жителя ============
            int price = getClientBuyPrice(slotIndex);
            if (price <= 0) return;

            int totalPrice = price * amount;
            int playerMoney = NumismaticHelper.getTotalMoney(player);
            if (playerMoney < totalPrice) return;

            // Проверяем место в инвентаре игрока
            ItemStack copyToInsert = stack.copyWithCount(amount);
            if (!player.getInventory().insertStack(copyToInsert)) {
                player.sendMessage(Text.literal("§cNo space in inventory!"), false);
                return;
            }

            // Снимаем деньги с игрока
            NumismaticHelper.removeMoney(player, totalPrice);

            // Уменьшаем количество в слоте жителя
            stack.decrement(amount);

            // Добавляем деньги жителю
            data.tradeOverhaul$getCurrency().addMoney(totalPrice);
            data.tradeOverhaul$getProfession().markAsTraded();

            // Начисляем опыт
            boolean wasPlayerSold = ItemTagHelper.isPlayerSold(stack);
            if (!wasPlayerSold) {
                data.tradeOverhaul$getProfession().applyXpFromSale(itemId, amount, professionFile);
            }

        } else {
            // ============ ПРОДАЖА жителю ============
            int price = getClientSellPrice(stack);
            if (price <= 0) {
                return;
            }

            int totalPrice = price * amount;
            int villagerMoney = data.tradeOverhaul$getCurrency().getTotalCopper();
            if (villagerMoney < totalPrice) {
                return;
            }

            // Проверяем, есть ли место в инвентаре жителя
            if (!hasVillagerSpaceForStack(stack, amount)) {
                return;
            }

            // ✅ Проверяем, что у игрока есть столько предметов
            // Находим слот с этим предметом в инвентаре игрока
            int playerSlotIndex = -1;
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (!s.isEmpty() && ItemStack.areItemsEqual(s, stack) && s.getCount() >= amount) {
                    playerSlotIndex = i;
                    break;
                }
            }

            if (playerSlotIndex == -1) {
                return;
            }

            // Убираем предметы из инвентаря игрока
            ItemStack playerStack = inv.getStack(playerSlotIndex);
            if (playerStack.getCount() < amount) return;
            playerStack.decrement(amount);

            // ✅ Добавляем предметы жителю с правильной NBT-меткой
            ItemStack toAdd = stack.copyWithCount(amount);
            ItemTagHelper.markAsPlayerSold(toAdd); // Помечаем как проданный игроком
            addToVillagerInventory(toAdd);

            // ✅ Снимаем деньги с жителя
            data.tradeOverhaul$getCurrency().removeMoney(totalPrice);

            // ✅ Добавляем деньги игроку
            NumismaticHelper.addMoney(player, totalPrice);

            data.tradeOverhaul$getProfession().markAsTraded();

            // ✅ Начисляем опыт за продажу
            boolean wasVillagerSold = ItemTagHelper.isVillagerSold(stack);
            boolean isSoldByVillager = professionFile != null && professionFile.isItemSoldByVillager(itemId);
            if (!wasVillagerSold && isSoldByVillager) {
                data.tradeOverhaul$getProfession().applyXpFromSale(itemId, amount, professionFile);
            }
        }

        // ✅ Синхронизируем уровень жителя с ванильным
        int modLevel = data.tradeOverhaul$getProfession().getLevel();
        if (modLevel > villager.getVillagerData().getLevel()) {
            villager.setVillagerData(villager.getVillagerData().withLevel(modLevel));
        }

        // ✅ Синхронизируем с клиентом
        NbtCompound soldItemsTracker = new NbtCompound();
        for (Map.Entry<String, Integer> e : data.tradeOverhaul$getProfession().soldItemsTracker.entrySet()) {
            soldItemsTracker.putInt(e.getKey(), e.getValue());
        }

        com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
                player, this.syncId,
                data.tradeOverhaul$getProfession().getLevel(),
                data.tradeOverhaul$getProfession().getExperience(),
                data.tradeOverhaul$getProfession().getTradesCompleted(),
                data.tradeOverhaul$getProfession().getFractionalXpAccumulator(),
                soldItemsTracker
        );

        sendContentUpdates();
        com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendInventorySync(player, this.syncId, villagerInventory);
    }

    // Проверка, есть ли место в инвентаре жителя
    private boolean hasVillagerSpaceForStack(ItemStack stack, int amount) {
        int remaining = amount;
        for (int i = 0; i < villagerInventory.size(); i++) {
            ItemStack slotStack = villagerInventory.getStack(i);
            if (slotStack.isEmpty()) {
                remaining -= stack.getMaxCount();
            } else if (ItemStack.areItemsEqual(slotStack, stack) && slotStack.getCount() < slotStack.getMaxCount()) {
                remaining -= (slotStack.getMaxCount() - slotStack.getCount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private void addToVillagerInventory(ItemStack stack) {
        if (stack.isEmpty()) return;

        TradeOverhaulMod.LOGGER.info("addToVillagerInventory called: {} x{}",
                stack.getName().getString(), stack.getCount());

        int remaining = stack.getCount();
        int maxStack = stack.getMaxCount();

        for (int i = 0; i < villagerInventory.size() && remaining > 0; i++) {
            ItemStack slotStack = villagerInventory.getStack(i);
            if (!slotStack.isEmpty() && ItemStack.areItemsEqual(slotStack, stack) && slotStack.getCount() < maxStack) {
                int space = maxStack - slotStack.getCount();
                int add = Math.min(space, remaining);
                slotStack.increment(add);
                remaining -= add;
                TradeOverhaulMod.LOGGER.info("  Added {} to existing slot {}, remaining: {}", add, i, remaining);
            }
        }

        for (int i = 0; i < villagerInventory.size() && remaining > 0; i++) {
            if (villagerInventory.getStack(i).isEmpty()) {
                int add = Math.min(remaining, maxStack);
                villagerInventory.setStack(i, stack.copyWithCount(add));
                remaining -= add;
                TradeOverhaulMod.LOGGER.info("  Added {} to empty slot {}, remaining: {}", add, i, remaining);
            }
        }

        if (remaining > 0) {
            TradeOverhaulMod.LOGGER.warn("Not enough space in villager inventory for {}", stack.getName().getString());
        }
    }
}