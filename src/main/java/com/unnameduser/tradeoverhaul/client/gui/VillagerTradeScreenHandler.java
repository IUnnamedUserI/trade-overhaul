package com.unnameduser.tradeoverhaul.client.gui;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.trade.ClientTradeView;
import com.unnameduser.tradeoverhaul.common.trade.TradePricing;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class VillagerTradeScreenHandler extends ScreenHandler {

	public static final int GRID_COLS = 4;
	public static final int GRID_ROWS = 9;
	public static final int PLAYER_PANEL_SLOTS = GRID_COLS * GRID_ROWS;
	public static final int FIRST_VILLAGER_SLOT_INDEX = PLAYER_PANEL_SLOTS;
	public static final int SLOT_COUNT = FIRST_VILLAGER_SLOT_INDEX + PLAYER_PANEL_SLOTS;

	public static final int LEFT_MARGIN = 48;
	public static final int LEFT_PANEL_X = LEFT_MARGIN;
	public static final int CENTER_PANEL_WIDTH = 240;
	public static final int GAP_BEFORE_CENTER = 64;
	public static final int GAP_AFTER_CENTER = 64;
	public static final int RIGHT_MARGIN = 48;
	public static final int PANEL_Y = 50;
	public static final int BOTTOM_MARGIN = 20;

	public static final int RIGHT_PANEL_X = LEFT_PANEL_X + GRID_COLS * 18 + GAP_BEFORE_CENTER;
	public static final int GUI_WIDTH = RIGHT_PANEL_X + GRID_COLS * 18 + RIGHT_MARGIN;
	public static final int GUI_HEIGHT = PANEL_Y + GRID_ROWS * 18 + BOTTOM_MARGIN;

	public static final int CENTER_TEXT_X = LEFT_PANEL_X + GRID_COLS * 18 + GAP_BEFORE_CENTER;
	public static final int LEFT_LABEL_Y = PANEL_Y - 16;
	public static final int RIGHT_LABEL_Y = PANEL_Y - 16;
	public static final int CENTER_INFO_START_Y = PANEL_Y + GRID_ROWS * 18 + 8;

	private final VillagerInventoryComponent villagerInventory;
	private final PlayerInventory playerInventory;
	private final VillagerEntity villager;
	private final ProfessionTradeFile professionFile;
	private final ClientTradeView clientView;
	private final int[] clientWalletHolder = new int[1];

	public VillagerTradeScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
		super(TradeOverhaulMod.VILLAGER_TRADE_SCREEN_HANDLER, syncId);
		this.playerInventory = playerInventory;
		this.villager = null;
		this.professionFile = null;
		this.clientWalletHolder[0] = buf.readVarInt();
		this.clientView = ClientTradeView.read(buf);
		this.villagerInventory = new VillagerInventoryComponent();
		buildSlots();
		this.addProperties(new PropertyDelegate() {
			@Override
			public int get(int index) {
				return index == 0 ? clientWalletHolder[0] : 0;
			}

			@Override
			public void set(int index, int value) {
				if (index == 0) {
					clientWalletHolder[0] = value;
				}
			}

			@Override
			public int size() {
				return 1;
			}
		});
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
		} else {
			this.villagerInventory = new VillagerInventoryComponent();
		}
		buildSlots();
		this.addProperties(new PropertyDelegate() {
			@Override
			public int get(int index) {
				if (index != 0) return 0;
				if (villager instanceof VillagerTradeData d) {
					return d.tradeOverhaul$getWalletEmeralds();
				}
				return 0;
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int size() {
				return 1;
			}
		});
	}

	private void buildSlots() {
		for (int col = 0; col < GRID_COLS; col++) {
			for (int row = 0; row < GRID_ROWS; row++) {
				int invIndex = col == GRID_COLS - 1 ? row : 9 + col * GRID_ROWS + row;
				this.addSlot(new Slot(playerInventory, invIndex, LEFT_PANEL_X + col * 18 - 1, PANEL_Y + row * 18 - 1));
			}
		}

		for (int col = 0; col < GRID_COLS; col++) {
			for (int row = 0; row < GRID_ROWS; row++) {
				int invIndex = row * GRID_COLS + col;
				this.addSlot(new VillagerReadOnlySlot(villagerInventory, invIndex, RIGHT_PANEL_X + col * 18 - 1, PANEL_Y + row * 18 - 1));
			}
		}
	}

	public boolean shouldClose() {
		if (villager == null) return false; // Не закрывать, если villager ещё не синхронизирован
		if (!villager.isAlive()) return true;
		if (villager.isRemoved()) return true;
		
		PlayerEntity player = playerInventory.player;
		if (player == null) return true;
		if (!player.isAlive()) return true;
		
		// Check distance (10 blocks squared = 100)
		if (villager.squaredDistanceTo(player) > 100.0) return true;
		
		return false;
	}

	public VillagerEntity getVillager() {
		return villager;
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex >= 0 && slotIndex < this.slots.size()) {
			Slot slot = this.slots.get(slotIndex);
			if (slot.inventory == this.villagerInventory) {
				return;
			}
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}

	public int getSyncedWallet() {
		if (villager instanceof VillagerTradeData d) {
			return d.tradeOverhaul$getWalletEmeralds();
		}
		return clientWalletHolder[0];
	}

	public int getClientBuyPriceForSlot(int screenSlot) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || sl.getStack().isEmpty()) return 0;
		if (clientView != null) {
			return clientView.buyPrice(sl.getStack());
		}
		if (professionFile != null) {
			return TradePricing.buyPriceForStack(sl.getStack(), professionFile);
		}
		return 0;
	}

	public int getClientSellUnitPrice(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		if (clientView != null) {
			return clientView.sellPrice(stack);
		}
		if (professionFile != null) {
			return TradePricing.sellPriceForStack(stack, professionFile);
		}
		return 0;
	}

	public int getDisplayedSellCost(ItemStack stack, boolean shiftHeld) {
		int unit = getClientSellUnitPrice(stack);
		if (unit <= 0) return 0;
		return shiftHeld ? unit * stack.getCount() : unit;
	}

	public boolean clientCanAffordBuy(int screenSlot) {
		int price = getClientBuyPriceForSlot(screenSlot);
		if (price <= 0) return false;
		return playerInventory.player.getInventory().count(Items.EMERALD) >= price;
	}

	public boolean villagerCanAffordSellDisplay(ItemStack stack, boolean shiftHeld) {
		int total = getDisplayedSellCost(stack, shiftHeld);
		if (total <= 0) return false;
		return getSyncedWallet() >= total;
	}

	public void handleBuyOnServer(int screenSlot, PlayerEntity player) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory) return;
		if (professionFile == null) return;

		int vIdx = sl.getIndex();
		if (vIdx < 0 || vIdx >= villagerInventory.size()) return;

		ItemStack villagerStack = villagerInventory.getStack(vIdx);
		if (villagerStack.isEmpty()) return;

		int price = TradePricing.buyPriceForStack(villagerStack, professionFile);
		if (price <= 0) return;
		if (!hasEnoughEmeralds(player, price)) return;

		ItemStack copy = villagerStack.copy();
		copy.setCount(1);

		if (!player.getInventory().insertStack(copy)) return;

		removeEmeralds(player, price);
		villagerStack.decrement(1);

		if (villager instanceof VillagerTradeData data) {
			data.tradeOverhaul$setWalletEmeralds(data.tradeOverhaul$getWalletEmeralds() + price);
		}
		sendContentUpdates();
	}

	public void handleSellOnServer(int screenSlot, PlayerEntity player, boolean sellWholeStack) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != playerInventory) return;
		if (!(villager instanceof VillagerTradeData data)) return;
		if (professionFile == null) return;

		ItemStack item = sl.getStack();
		if (item.isEmpty()) return;

		int unitPrice = TradePricing.sellPriceForStack(item, professionFile);
		if (unitPrice <= 0) return;

		int toSell = sellWholeStack ? item.getCount() : 1;
		long totalPriceLong = (long) unitPrice * toSell;
		if (totalPriceLong > Integer.MAX_VALUE) return;
		int totalPrice = (int) totalPriceLong;

		if (data.tradeOverhaul$getWalletEmeralds() < totalPrice) return;

		int maxFit = maxItemsVillagerCanAccept(item, toSell);
		if (maxFit < toSell) return;

		if (!insertItemCountIntoVillager(item, toSell)) return;

		item.decrement(toSell);
		data.tradeOverhaul$setWalletEmeralds(data.tradeOverhaul$getWalletEmeralds() - totalPrice);
		addEmeraldsToPlayer(player, totalPrice);
		sendContentUpdates();
	}

	private int maxItemsVillagerCanAccept(ItemStack template, int want) {
		ItemStack unit = template.copy();
		unit.setCount(1);
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
					ItemStack place = remaining.copy();
					place.setCount(put);
					villagerInventory.setStack(i, place);
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
			if (!moved) {
				return false;
			}
		}
		return true;
	}

	private boolean hasEnoughEmeralds(PlayerEntity player, int amount) {
		return player.getInventory().count(Items.EMERALD) >= amount;
	}

	private void removeEmeralds(PlayerEntity player, int amount) {
		int toRemove = amount;
		for (int i = 0; i < player.getInventory().size() && toRemove > 0; i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.getItem() == Items.EMERALD) {
				int remove = Math.min(toRemove, stack.getCount());
				stack.decrement(remove);
				toRemove -= remove;
			}
		}
	}

	private void addEmeraldsToPlayer(PlayerEntity player, int amount) {
		player.getInventory().offerOrDrop(new ItemStack(Items.EMERALD, amount));
	}

	public VillagerInventoryComponent getVillagerInventory() {
		return villagerInventory;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}
}
