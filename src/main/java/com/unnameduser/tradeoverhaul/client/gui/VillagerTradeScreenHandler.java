package com.unnameduser.tradeoverhaul.client.gui;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.network.ModNetworking;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.common.trade.ClientTradeView;
import com.unnameduser.tradeoverhaul.common.trade.TradePricing;
import com.unnameduser.tradeoverhaul.common.trade.TradeScreenSync;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class VillagerTradeScreenHandler extends ScreenHandler {

	public static final int GRID_COLS = 6;
	public static final int GRID_ROWS = 6;
	public static final int PLAYER_GRID_SLOTS = GRID_COLS * GRID_ROWS;
	public static final int ARMOR_SLOT_COUNT = 5;
	public static final int ARMOR_SLOT_WIDTH = 18;

	public static final int FIRST_ARMOR_SLOT_INDEX = 0;
	public static final int FIRST_MAIN_GRID_SLOT_INDEX = ARMOR_SLOT_COUNT;
	public static final int FIRST_VILLAGER_SLOT_INDEX = FIRST_MAIN_GRID_SLOT_INDEX + PLAYER_GRID_SLOTS;
	public static final int SLOT_COUNT = FIRST_VILLAGER_SLOT_INDEX + PLAYER_GRID_SLOTS;

	public static final int LEFT_MARGIN = 48;
	public static final int ARMOR_PANEL_X = LEFT_MARGIN;
	public static final int MAIN_GRID_X = ARMOR_PANEL_X + ARMOR_SLOT_WIDTH + 16;
	public static final int RIGHT_PANEL_X = MAIN_GRID_X + GRID_COLS * 18 + 64;
	public static final int PANEL_Y = 50;
	public static final int BOTTOM_MARGIN = 20;

	public static final int GUI_WIDTH = RIGHT_PANEL_X + GRID_COLS * 18 + 48;
	public static final int GUI_HEIGHT = PANEL_Y + GRID_ROWS * 18 + BOTTOM_MARGIN;

	private final VillagerInventoryComponent villagerInventory;
	private final PlayerInventory playerInventory;
	private VillagerEntity villager;
	private final ProfessionTradeFile professionFile;
	private final ClientTradeView clientView;
	private final int[] clientWalletHolder = new int[1];
	private final int[] clientBuyQuantities;
	private final int[] clientSellQuantities;

	public VillagerTradeScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
		super(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, syncId);
		this.playerInventory = playerInventory;
		this.villager = null;
		// Загружаем профессию на клиенте тоже (для tooltip)
		int villagerProfessionId = buf.readVarInt();
		if (villagerProfessionId >= 0 && villagerProfessionId < Registries.VILLAGER_PROFESSION.size()) {
			Identifier pid = Registries.VILLAGER_PROFESSION.getId(Registries.VILLAGER_PROFESSION.get(villagerProfessionId));
			this.professionFile = TradeConfigLoader.getProfession(pid);
		} else {
			this.professionFile = null;
		}
		this.clientWalletHolder[0] = buf.readVarInt();
		TradeScreenSync.SyncedInventory synced = TradeScreenSync.readInventory(buf);
		this.villagerInventory = synced.inventory;
		this.clientBuyQuantities = synced.buyQuantities;
		this.clientSellQuantities = synced.sellQuantities;
		this.clientView = ClientTradeView.read(buf);
		buildSlots();
		initPropertyDelegate();
	}

	public void setVillager(VillagerEntity villager) {
		this.villager = villager;
		// Обновляем кошелёк из нового компонента валюты
		if (villager instanceof VillagerTradeData data) {
			this.clientWalletHolder[0] = data.tradeOverhaul$getCurrency().getTotalCopper();
		}
	}

	public VillagerTradeScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
		super(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, syncId);
		this.playerInventory = playerInventory;
		this.villager = villager;
		Identifier pid = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		this.professionFile = TradeConfigLoader.getProfession(pid);
		this.clientView = null;
		if (villager instanceof VillagerTradeData data) {
			this.villagerInventory = data.tradeOverhaul$getInventory();
			// Инициализируем кошелёк из нового компонента валюты
			this.clientWalletHolder[0] = data.tradeOverhaul$getCurrency().getTotalCopper();
		} else {
			this.villagerInventory = new VillagerInventoryComponent();
			this.clientWalletHolder[0] = 0;
		}
		this.clientBuyQuantities = new int[36];
		this.clientSellQuantities = new int[36];
		updateClientQuantities();
		buildSlots();
		initPropertyDelegate();
	}

	private void initPropertyDelegate() {
		this.addProperties(new PropertyDelegate() {
			@Override
			public int get(int index) {
				if (index == 0) {
					// Возвращаем актуальный баланс из компонента валюты
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

	private void buildSlots() {
		int[] armorSlots = {39, 38, 37, 36, 40};
		for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
			this.addSlot(new Slot(playerInventory, armorSlots[i], ARMOR_PANEL_X + 53, PANEL_Y + i * 20 - 20));
		}
		int[] playerSlotMap = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35};
		for (int row = 0; row < GRID_ROWS; row++) {
			for (int col = 0; col < GRID_COLS; col++) {
				int gridIndex = row * GRID_COLS + col;
				int invIndex = playerSlotMap[gridIndex];
				this.addSlot(new Slot(playerInventory, invIndex, MAIN_GRID_X + col * 19 - 1, PANEL_Y + row * 19 - 1));
			}
		}
		for (int row = 0; row < GRID_ROWS; row++) {
			for (int col = 0; col < GRID_COLS; col++) {
				int invIndex = row * GRID_COLS + col;
				this.addSlot(new VillagerReadOnlySlot(villagerInventory, invIndex, RIGHT_PANEL_X + col * 19 - 1, PANEL_Y + row * 19 - 1));
			}
		}
	}

	public boolean shouldClose() {
		if (villager == null || !villager.isAlive() || villager.isRemoved()) return false;
		PlayerEntity player = playerInventory.player;
		if (player == null || !player.isAlive()) return true;
		return villager.squaredDistanceTo(player) > 100.0;
	}

	public VillagerEntity getVillager() { return villager; }
	public VillagerInventoryComponent getVillagerInventory() { return villagerInventory; }

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex >= 0 && slotIndex < this.slots.size()) {
			Slot slot = this.slots.get(slotIndex);
			if (slot.inventory == this.villagerInventory) return;
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}

	public int getSyncedWallet() {
		if (villager instanceof VillagerTradeData data) {
			return data.tradeOverhaul$getCurrency().getTotalCopper();
		}
		return clientWalletHolder[0];
	}

	public int getClientBuyQuantityForSlot(int screenSlot) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || sl.getStack().isEmpty()) return 1;
		int slotIndex = sl.getIndex();
		if (clientBuyQuantities != null && slotIndex >= 0 && slotIndex < clientBuyQuantities.length) {
			return clientBuyQuantities[slotIndex];
		}
		if (professionFile != null) {
			return TradePricing.getBuyQuantity(sl.getStack(), villager, professionFile);
		}
		return 1;
	}

	public int getClientSellQuantity(ItemStack stack) {
		if (stack.isEmpty()) return 1;
		if (professionFile != null) {
			return TradePricing.getSellQuantity(stack, professionFile);
		}
		return 1;
	}

	public int getClientBuyPrice(int screenSlot) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || sl.getStack().isEmpty()) return 0;
		if (professionFile == null) {
			TradeOverhaulMod.LOGGER.warn("getClientBuyPrice: professionFile is null!");
			return 0;
		}
		int price = TradePricing.getBuyPrice(sl.getStack(), professionFile);
		TradeOverhaulMod.LOGGER.info("getClientBuyPrice: item={}, price={}", sl.getStack().getItem().getTranslationKey(), price);
		return price;
	}

	public int getClientSellPrice(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		if (professionFile == null) {
			TradeOverhaulMod.LOGGER.warn("getClientSellPrice: professionFile is null!");
			return 0;
		}
		int price = TradePricing.getSellPrice(stack, professionFile);
		TradeOverhaulMod.LOGGER.info("getClientSellPrice: item={}, price={}", stack.getItem().getTranslationKey(), price);
		return price;
	}

	public boolean clientCanAffordBuy(int price) {
		return NumismaticHelper.hasEnoughMoney(playerInventory.player, price);
	}

	public boolean villagerCanAffordSell(int price) {
		if (villager instanceof VillagerTradeData data) {
			return data.tradeOverhaul$getCurrency().hasEnough(price);
		}
		return clientWalletHolder[0] >= price;
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

	public void handleBuyOnServer(int screenSlot, PlayerEntity player, boolean buyWholeStack) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || professionFile == null) return;

		int vIdx = sl.getIndex();
		if (vIdx < 0 || vIdx >= villagerInventory.size()) return;

		ItemStack villagerStack = villagerInventory.getStack(vIdx);
		if (villagerStack.isEmpty()) return;

		int buyQty = TradePricing.getBuyQuantity(villagerStack, villager, professionFile);
		int price = TradePricing.getBuyPrice(villagerStack, professionFile);
		if (buyQty <= 0) buyQty = 1;
		if (price <= 0) price = 1;

		// Цена за 1 предмет
		int pricePerItem = price / buyQty;
		if (pricePerItem <= 0) pricePerItem = 1;

		int toBuy, totalCost;
		if (buyWholeStack) {
			// Покупаем весь стак
			toBuy = villagerStack.getCount();
			totalCost = toBuy * pricePerItem;
		} else {
			// Покупаем 1 предмет
			toBuy = 1;
			totalCost = pricePerItem;
		}

		if (toBuy <= 0 || !NumismaticHelper.hasEnoughMoney(player, totalCost)) return;
		if (!clientHasInventorySpaceForStack(villagerStack, toBuy)) return;

		ItemStack copy = villagerStack.copy();
		copy.setCount(toBuy);
		if (!player.getInventory().insertStack(copy)) return;

		NumismaticHelper.removeMoney(player, totalCost);
		villagerStack.decrement(toBuy);
		// Добавляем монеты жителю через новый компонент
		if (villager instanceof VillagerTradeData data) {
			data.tradeOverhaul$getCurrency().addMoney(totalCost);
		}

		updateClientQuantities();
		sendContentUpdates();
		if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			ModNetworking.sendInventorySync(serverPlayer, this.syncId, villagerInventory);
		}
	}

	public void handleSellOnServer(int screenSlot, PlayerEntity player, boolean sellWholeStack) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != playerInventory || !(villager instanceof VillagerTradeData data) || professionFile == null) return;

		ItemStack item = sl.getStack();
		if (item.isEmpty()) return;

		if (!TradePricing.canVillagerBuyItem(item, villager, professionFile)) return;

		int sellQty = TradePricing.getSellQuantity(item, professionFile);
		int sellPrice = TradePricing.getSellPrice(item, professionFile);
		if (sellQty <= 0) sellQty = 1;
		if (sellPrice <= 0) sellPrice = 1;

		// Цена за 1 предмет
		int pricePerItem = sellPrice / sellQty;
		if (pricePerItem <= 0) pricePerItem = 1;

		int toSell, totalEarned;
		if (sellWholeStack) {
			// Продаём весь стак
			toSell = item.getCount();
			totalEarned = toSell * pricePerItem;
		} else {
			// Продаём 1 предмет
			toSell = 1;
			totalEarned = pricePerItem;
		}

		if (toSell <= 0) return;
		// Проверяем, достаточно ли монет у жителя
		if (villager instanceof VillagerTradeData currencyData) {
			if (!currencyData.tradeOverhaul$getCurrency().hasEnough(totalEarned)) return;
		}

		int maxFit = maxItemsVillagerCanAccept(item, toSell);
		if (maxFit < toSell) return;

		if (!insertItemCountIntoVillager(item, toSell)) return;

		item.decrement(toSell);
		// Снимаем монеты у жителя через новый компонент
		if (villager instanceof VillagerTradeData currencyData2) {
			if (!currencyData2.tradeOverhaul$getCurrency().removeMoney(totalEarned)) {
				return; // Недостаточно монет
			}
		}
		NumismaticHelper.addMoney(player, totalEarned);

		updateClientQuantities();
		sendContentUpdates();
		if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			ModNetworking.sendInventorySync(serverPlayer, this.syncId, villagerInventory);
		}
	}

	private int maxItemsVillagerCanAccept(ItemStack template, int want) {
		int maxStack = template.getMaxCount();
		int space = 0;
		for (int i = 0; i < villagerInventory.size(); i++) {
			ItemStack s = villagerInventory.getStack(i);
			if (s.isEmpty()) {
				space += maxStack;
			} else if (ItemStack.areItemsEqual(s, template)) {
				space += maxStack - s.getCount();
			}
			if (space >= want) return want;
		}
		return space;
	}

	private boolean insertItemCountIntoVillager(ItemStack template, int count) {
		ItemStack remaining = template.copy();
		remaining.setCount(count);
		while (!remaining.isEmpty()) {
			boolean moved = false;
			for (int i = 0; i < villagerInventory.size(); i++) {
				ItemStack slotStack = villagerInventory.getStack(i);
				if (slotStack.isEmpty()) {
					int put = Math.min(remaining.getCount(), remaining.getMaxCount());
					villagerInventory.setStack(i, remaining.copyWithCount(put));
					remaining.decrement(put);
					moved = true;
					break;
				}
				if (ItemStack.areItemsEqual(slotStack, remaining) && slotStack.getCount() < slotStack.getMaxCount()) {
					int space = slotStack.getMaxCount() - slotStack.getCount();
					int put = Math.min(remaining.getCount(), space);
					slotStack.increment(put);
					remaining.decrement(put);
					moved = true;
					break;
				}
			}
			if (!moved) return false;
		}
		return true;
	}

	private void updateClientQuantities() {
		if (clientBuyQuantities == null || clientSellQuantities == null || professionFile == null) return;
		for (int i = 0; i < villagerInventory.size(); i++) {
			ItemStack stack = villagerInventory.getStack(i);
			if (stack.isEmpty()) {
				clientBuyQuantities[i] = 1;
				clientSellQuantities[i] = 1;
			} else {
				clientBuyQuantities[i] = TradePricing.getBuyQuantity(stack, villager, professionFile);
				clientSellQuantities[i] = TradePricing.getSellQuantity(stack, professionFile);
			}
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
	@Override
	public boolean canUse(PlayerEntity player) { return true; }
}
