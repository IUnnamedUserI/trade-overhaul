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
	
	// Данные о профессии для клиента
	private int clientProfessionLevel = 1;
	private int clientProfessionExperience = 0;
	private int clientProfessionTradesCompleted = 0;
	private float clientFractionalXp = 0f;
	private java.util.Map<String, Integer> clientSoldItemsTracker = new java.util.HashMap<>();
	private java.util.Map<String, Float> clientDamageReputation = new java.util.HashMap<>();
	
	/**
	 * Получает накопленный дробный XP (для GUI)
	 */
	public float getClientFractionalXp() {
		return clientFractionalXp;
	}
	
	/**
	 * Обновляет данные о профессии (вызывается при получении сетевого пакета)
	 */
	public void updateProfessionLevel(int level, int experience, int tradesCompleted, float fractionalXp, net.minecraft.nbt.NbtCompound soldItemsTracker) {
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
	}

	/**
	 * Обновляет репутацию урона (вызывается при получении сетевого пакета)
	 */
	public void updateDamageReputation(java.util.Map<String, Float> damageReputation) {
		this.clientDamageReputation.clear();
		this.clientDamageReputation.putAll(damageReputation);
	}

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
		
		// Читаем данные о профессии (уровень, опыт, трекинг предметов)
		this.clientProfessionLevel = buf.readVarInt();
		this.clientProfessionExperience = buf.readVarInt();
		this.clientProfessionTradesCompleted = buf.readVarInt();
		this.clientFractionalXp = buf.readFloat();
		net.minecraft.nbt.NbtCompound soldItemsTrackerNbt = buf.readNbt();
		if (soldItemsTrackerNbt != null) {
			this.clientSoldItemsTracker.clear();
			for (String key : soldItemsTrackerNbt.getKeys()) {
				this.clientSoldItemsTracker.put(key, soldItemsTrackerNbt.getInt(key));
			}
		}

		// Читаем репутацию урона
		net.minecraft.nbt.NbtCompound damageRepNbt = buf.readNbt();
		if (damageRepNbt != null) {
			this.clientDamageReputation.clear();
			for (String key : damageRepNbt.getKeys()) {
				this.clientDamageReputation.put(key, damageRepNbt.getFloat(key));
			}
		}

		// Читаем кошелёк и инвентарь из TradeScreenSync
		this.clientWalletHolder[0] = buf.readVarInt();
		TradeScreenSync.SyncedInventory synced = TradeScreenSync.readInventory(buf);
		this.villagerInventory = synced.inventory;
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
			
			// Инициализируем данные о профессии
			this.clientProfessionLevel = data.tradeOverhaul$getProfession().getLevel();
			this.clientProfessionExperience = data.tradeOverhaul$getProfession().getExperience();
			this.clientProfessionTradesCompleted = data.tradeOverhaul$getProfession().getTradesCompleted();
			this.clientFractionalXp = data.tradeOverhaul$getProfession().getFractionalXpAccumulator();
			
			// Копируем трекинг предметов
			this.clientSoldItemsTracker.clear();
			this.clientSoldItemsTracker.putAll(data.tradeOverhaul$getProfession().soldItemsTracker);
		} else {
			this.villagerInventory = new VillagerInventoryComponent();
			this.clientWalletHolder[0] = 0;
		}
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
	
	/**
	 * Получает уровень профессии жителя
	 */
	public int getProfessionLevel() {
		if (villager instanceof VillagerTradeData data) {
			return data.tradeOverhaul$getProfession().getLevel();
		}
		// На клиенте используем синхронизированные данные
		return clientProfessionLevel;
	}
	
	/**
	 * Получает название уровня профессии жителя
	 */
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
	
	/**
	 * Получает прогресс до следующего уровня (0.0 - 1.0)
	 */
	public float getProfessionLevelProgress() {
		int level = getProfessionLevel();
		int experience = getProfessionExperience();
		int xpForNext = getXpForNextLevel();
		if (level >= 5 || xpForNext <= 0) return 1.0f;
		return Math.min(1.0f, (float) experience / xpForNext);
	}
	
	/**
	 * Получает текущий опыт жителя
	 */
	public int getProfessionExperience() {
		if (villager instanceof VillagerTradeData data) {
			return data.tradeOverhaul$getProfession().getExperience();
		}
		return clientProfessionExperience;
	}
	
	/**
	 * Получает опыт, необходимый для следующего уровня
	 */
	public int getXpForNextLevel() {
		int level = getProfessionLevel();
		if (level >= 5) return 0;
		// XP_REQUIRED = {0, 10, 30, 60, 100} для уровней 1-5
		// Для уровня 1 нужно 10 XP, для уровня 2 нужно 30 XP, и т.д.
		int[] xpRequired = {0, 10, 30, 60, 100};
		return xpRequired[level];
	}
	
	/**
	 * Рассчитывает ожидаемый XP за покупку предмета из слота жителя
	 * @param screenSlot Индекс слота в GUI
	 * @param amount Количество предметов для покупки
	 * @return Ожидаемый XP (с учётом множителей предметов)
	 */
	public float getExpectedXpForBuy(int screenSlot, int amount) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || sl.getStack().isEmpty()) {
			return 0f;
		}

		String itemId = net.minecraft.registry.Registries.ITEM.getId(sl.getStack().getItem()).toString();

		// Проверяем, не были ли предметы проданы игроком ранее
		if (com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isPlayerSold(sl.getStack())) {
			return 0f;  // XP не даётся за предметы, проданные игроком
		}

		// Рассчитываем XP: multiplier × amount
		// Сначала проверяем множитель из пула предметов в конфигурации профессии
		float multiplier = 1.0f;
		if (professionFile != null) {
			Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
			if (poolMultiplier != null) {
				multiplier = poolMultiplier;
			} else {
				// Fallback на глобальный конфиг
				multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
			}
		} else {
			multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
		}
		float totalXp = multiplier * amount;

		return totalXp;
	}

	public int getClientBuyPrice(int screenSlot) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || sl.getStack().isEmpty()) return 0;
		if (professionFile == null) {
			TradeOverhaulMod.LOGGER.warn("getClientBuyPrice: professionFile is null!");
			return 0;
		}
		ItemStack stack = sl.getStack();
		int price = TradePricing.getBuyPrice(stack, professionFile);

		// Применяем модификатор репутации урона (используем клиентские данные)
		if (playerInventory.player != null) {
			String playerId = playerInventory.player.getUuidAsString();
			float totalDamage = clientDamageReputation.getOrDefault(playerId, 0f);
			if (totalDamage > 0) {
				com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings s = TradeConfigLoader.getSettings();
				double repPercent = Math.min(totalDamage * s.damageReputationPercentPerHP, s.damageReputationMaxPercent);
				if (repPercent > 0) {
					double multiplier = 1.0 + (repPercent / 100.0);
					price = (int) Math.ceil(price * multiplier);
				}
			}
		}

		// Логирование для зачарованных книг (отладка)
		if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK && price > 0) {
			TradeOverhaulMod.LOGGER.debug("Enchanted book price: {} copper, enchantments count: {}", price,
				professionFile.enchantments != null ? professionFile.enchantments.size() : 0);
		}

		return price;
	}

	public int getClientSellPrice(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		if (professionFile == null) return 0;
		int price = TradePricing.getSellPrice(stack, professionFile);

		// Применяем модификатор прочности (для повреждённых предметов)
		price = TradePricing.applyDurabilityPriceModifier(price, stack, TradeConfigLoader.getSettings());

		// При продаже репутация урона НЕ применяется (игрок не должен получать выгоду от избиения жителя)
		return price;
	}

	/**
	 * Рассчитывает ожидаемый XP при продаже предмета жителю.
	 * @param stack Предмет для продажи
	 * @param amount Количество
	 * @return Ожидаемый XP или 0, если предмет не даёт XP
	 */
	public float getExpectedXpForSell(ItemStack stack, int amount) {
		if (stack.isEmpty() || professionFile == null) return 0f;

		// Предметы, купленные у жителя, XP НЕ дают
		if (com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isVillagerSold(stack)) {
			return 0f;
		}

		String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();

		// Предмет должен быть в пулах продажи жителя
		if (!professionFile.isItemSoldByVillager(itemId)) {
			return 0f;
		}

		// Рассчитываем XP
		float multiplier = 1.0f;
		Float poolMultiplier = professionFile.findXpMultiplierForItem(itemId);
		if (poolMultiplier != null) {
			multiplier = poolMultiplier;
		} else {
			multiplier = com.unnameduser.tradeoverhaul.common.config.VillagerXpConfig.getXpMultiplier(itemId);
		}

		return multiplier * amount;
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
	
	/**
	 * Проверяет, может ли житель купить этот предмет у игрока.
	 */
	public boolean canVillagerBuyItem(ItemStack stack) {
		if (stack.isEmpty() || professionFile == null) return false;
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

	public void handleBuyOnServer(int screenSlot, PlayerEntity player, boolean buyWholeStack, boolean buyTen) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != villagerInventory || professionFile == null) return;

		int vIdx = sl.getIndex();
		if (vIdx < 0 || vIdx >= villagerInventory.size()) return;

		ItemStack villagerStack = villagerInventory.getStack(vIdx);
		if (villagerStack.isEmpty()) return;

		int price = TradePricing.getBuyPrice(villagerStack, professionFile);

		// Применяем модификатор репутации урона
		if (villager instanceof VillagerTradeData data) {
			price = TradePricing.applyDamageReputation(price, player.getUuidAsString(),
				data.tradeOverhaul$getProfession(), TradeConfigLoader.getSettings());
		}

		if (price <= 0) price = 1;
		int pricePerItem = price;

		// Определяем количество для покупки
		int wantToBuy;
		if (buyWholeStack) {
			wantToBuy = villagerStack.getCount();
		} else if (buyTen) {
			wantToBuy = Math.min(10, villagerStack.getCount());
		} else {
			wantToBuy = 1;
		}

		// Проверяем, сколько денег у игрока
		int playerMoney = NumismaticHelper.getTotalMoney(player);

		// Рассчитываем, сколько предметов может купить игрок
		int maxCanBuy = playerMoney / pricePerItem;
		int toBuy = Math.min(wantToBuy, maxCanBuy);
		
		if (toBuy <= 0) return; // У игрока совсем нет денег

		int totalCost = toBuy * pricePerItem;

		// Проверяем место в инвентаре игрока
		int maxFit = maxInventorySpaceForStack(player, villagerStack, toBuy);
		if (maxFit <= 0) return; // Нет места в инвентаре
		if (maxFit < toBuy) {
			toBuy = maxFit;
			totalCost = toBuy * pricePerItem;
		}
		
		if (toBuy <= 0) return;

		ItemStack copy = villagerStack.copy();
		copy.setCount(toBuy);
		// Помечаем предмет как купленный у жителя (защита от дюпа XP)
		com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.markAsVillagerSold(copy);
		if (!player.getInventory().insertStack(copy)) return;

		NumismaticHelper.removeMoney(player, totalCost);
		
		// Получаем itemId ДО уменьшения стака!
		String itemId = net.minecraft.registry.Registries.ITEM.getId(villagerStack.getItem()).toString();
		
		// Проверяем тег PlayerSold ДО уменьшения стака
		boolean wasPlayerSold = com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isPlayerSold(villagerStack);
		
		villagerStack.decrement(toBuy);
		
		// Добавляем монеты жителю через новый компонент
		if (villager instanceof VillagerTradeData data) {
			data.tradeOverhaul$getCurrency().addMoney(totalCost);

			// Отмечаем, что с жителем была совершена сделка
			data.tradeOverhaul$getProfession().markAsTraded();

			// Добавляем опыт жителю за ПОКУПКУ у жителя (игрок покупает, житель продаёт)
			// XP НЕ даётся, если предмет был продан игроком ранее
			if (!wasPlayerSold) {
				TradeOverhaulMod.LOGGER.info("XP DEBUG: Applying XP for item={}, amount={}, profession={}", 
					itemId, toBuy, professionFile != null ? professionFile.profession : "null");
				data.tradeOverhaul$getProfession().applyXpFromSale(itemId, toBuy, professionFile);
				
				// Синхронизируем ванильный уровень с нашим компонентом
				int modLevel = data.tradeOverhaul$getProfession().getLevel();
				int vanillaLevel = villager.getVillagerData().getLevel();
				if (modLevel > vanillaLevel) {
					villager.setVillagerData(villager.getVillagerData().withLevel(modLevel));
				}
			}

			// Отправляем синхронизацию уровня профессии клиенту (с трекингом предметов)
			if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
				net.minecraft.nbt.NbtCompound soldItemsTracker = new net.minecraft.nbt.NbtCompound();
				for (java.util.Map.Entry<String, Integer> entry : data.tradeOverhaul$getProfession().soldItemsTracker.entrySet()) {
					soldItemsTracker.putInt(entry.getKey(), entry.getValue());
				}
				com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
					serverPlayer, this.syncId,
					data.tradeOverhaul$getProfession().getLevel(),
					data.tradeOverhaul$getProfession().getExperience(),
					data.tradeOverhaul$getProfession().getTradesCompleted(),
					data.tradeOverhaul$getProfession().getFractionalXpAccumulator(),
					soldItemsTracker
				);
			}
		}

		sendContentUpdates();
		if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			ModNetworking.sendInventorySync(serverPlayer, this.syncId, villagerInventory);
		}
	}

	public void handleSellOnServer(int screenSlot, PlayerEntity player, boolean sellWholeStack, boolean sellTen) {
		Slot sl = getSlot(screenSlot);
		if (sl == null || sl.inventory != playerInventory || !(villager instanceof VillagerTradeData data) || professionFile == null) return;

		ItemStack item = sl.getStack();
		if (item.isEmpty()) return;

		TradeOverhaulMod.LOGGER.debug("handleSellOnServer: item={}, profession={}", Registries.ITEM.getId(item.getItem()), professionFile.profession);
		boolean canBuy = TradePricing.canVillagerBuyItem(item, villager, professionFile);
		TradeOverhaulMod.LOGGER.debug("handleSellOnServer: canVillagerBuyItem={}", canBuy);
		if (!canBuy) return;

		int sellPrice = TradePricing.getSellPrice(item, professionFile);

		// Применяем модификатор прочности (для повреждённых предметов)
		sellPrice = TradePricing.applyDurabilityPriceModifier(sellPrice, item, TradeConfigLoader.getSettings());

		// При продаже репутация урона НЕ применяется
		if (sellPrice <= 0) sellPrice = 1;
		int pricePerItem = sellPrice;

		// Определяем количество для продажи
		int wantToSell;
		if (sellWholeStack) {
			wantToSell = item.getCount();
		} else if (sellTen) {
			wantToSell = Math.min(10, item.getCount());
		} else {
			wantToSell = 1;
		}

		// Проверяем, сколько монет у жителя
		int villagerMoney = 0;
		if (villager instanceof VillagerTradeData currencyData) {
			villagerMoney = currencyData.tradeOverhaul$getCurrency().getTotalCopper();
		}

		// Рассчитываем, сколько предметов может купить житель
		int maxCanBuy = villagerMoney / pricePerItem;
		int toSell = Math.min(wantToSell, maxCanBuy);
		
		if (toSell <= 0) return; // У жителя совсем нет денег

		int totalEarned = toSell * pricePerItem;

		// Проверяем место в инвентаре жителя
		int maxFit = maxItemsVillagerCanAccept(item, toSell);
		if (maxFit < toSell) {
			toSell = maxFit;
			totalEarned = toSell * pricePerItem;
		}
		
		if (toSell <= 0) return;

		if (!insertItemCountIntoVillager(item, toSell)) return;

		item.decrement(toSell);
		// Снимаем монеты у жителя через новый компонент
		if (villager instanceof VillagerTradeData currencyData2) {
			if (!currencyData2.tradeOverhaul$getCurrency().removeMoney(totalEarned)) {
				return; // Недостаточно монет
			}
			// Отмечаем, что с жителем была совершена сделка
			currencyData2.tradeOverhaul$getProfession().markAsTraded();

			// Даём XP жителю за покупку предмета у игрока
			// Проверки для защиты от дюпа XP:
			String itemId = net.minecraft.registry.Registries.ITEM.getId(item.getItem()).toString();

			// 1. Предметы, купленные у жителя (тег VillagerSold), XP НЕ дают
			boolean wasVillagerSold = com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isVillagerSold(item);

			// 2. Предмет должен быть в пулах продажи жителя (иначе XP не даётся)
			boolean isSoldByVillager = professionFile.isItemSoldByVillager(itemId);

			if (!wasVillagerSold && isSoldByVillager) {
				TradeOverhaulMod.LOGGER.info("XP DEBUG (sell): Giving XP for item={}, amount={}, profession={}",
					itemId, toSell, professionFile.profession);
				currencyData2.tradeOverhaul$getProfession().applyXpFromSale(itemId, toSell, professionFile);
				
				// Синхронизируем уровень профессии клиенту после начисления XP
				if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
					net.minecraft.nbt.NbtCompound soldItemsTracker = new net.minecraft.nbt.NbtCompound();
					for (java.util.Map.Entry<String, Integer> entry : currencyData2.tradeOverhaul$getProfession().soldItemsTracker.entrySet()) {
						soldItemsTracker.putInt(entry.getKey(), entry.getValue());
					}
					com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendProfessionLevelSync(
						serverPlayer, this.syncId,
						currencyData2.tradeOverhaul$getProfession().getLevel(),
						currencyData2.tradeOverhaul$getProfession().getExperience(),
						currencyData2.tradeOverhaul$getProfession().getTradesCompleted(),
						currencyData2.tradeOverhaul$getProfession().getFractionalXpAccumulator(),
						soldItemsTracker
					);
				}
			} else if (wasVillagerSold) {
				TradeOverhaulMod.LOGGER.debug("XP DEBUG (sell): Item {} was bought from villager, no XP given", itemId);
			} else if (!isSoldByVillager) {
				TradeOverhaulMod.LOGGER.debug("XP DEBUG (sell): Item {} is not in villager's sell pools, no XP given", itemId);
			}
		}
		NumismaticHelper.addMoney(player, totalEarned);

		sendContentUpdates();
		if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			ModNetworking.sendInventorySync(serverPlayer, this.syncId, villagerInventory);
		}
	}
	
	/**
	 * Вставляет предметы в инвентарь жителя, помечая их как проданные игроком
	 */
	private boolean insertItemCountIntoVillager(ItemStack template, int count) {
		ItemStack remaining = template.copy();
		remaining.setCount(count);

		// Помечаем все предметы как проданные игроком
		com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.markAsPlayerSold(remaining);

		// Определяем максимальный размер стака для этого предмета
		int maxStackSize = getMaxStackSizeForItem(template);

		// Сначала пытаемся добавить в существующие стакающиеся слоты (с тегом)
		for (int i = 0; i < villagerInventory.size() && !remaining.isEmpty(); i++) {
			ItemStack slotStack = villagerInventory.getStack(i);
			if (ItemStack.areItemsEqual(slotStack, remaining) &&
				slotStack.getCount() < maxStackSize &&
				canStackItems(slotStack, remaining)) {
				int space = maxStackSize - slotStack.getCount();
				int put = Math.min(remaining.getCount(), space);
				slotStack.increment(put);
				remaining.decrement(put);
			}
		}

		// Затем занимаем пустые слоты
		while (!remaining.isEmpty()) {
			boolean moved = false;
			for (int i = 0; i < villagerInventory.size(); i++) {
				ItemStack slotStack = villagerInventory.getStack(i);
				if (slotStack.isEmpty()) {
					int put = Math.min(remaining.getCount(), maxStackSize);
					villagerInventory.setStack(i, remaining.copyWithCount(put));
					remaining.decrement(put);
					moved = true;
					break;
				}
			}
			if (!moved) return false;
		}
		return true;
	}

	private int maxInventorySpaceForStack(PlayerEntity player, ItemStack template, int want) {
		int maxStack = template.getMaxCount();
		int space = 0;
		var inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (s.isEmpty()) {
				space += maxStack;
			} else if (ItemStack.areItemsEqual(s, template)) {
				space += maxStack - s.getCount();
			}
			if (space >= want) return want;
		}
		return space;
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

	/**
	 * Возвращает максимальный размер стака для предмета.
	 * Инструменты, броня, оружие не стакаются (maxCount=1).
	 */
	private int getMaxStackSizeForItem(ItemStack stack) {
		if (stack.isEmpty()) return 64;
		
		// Предметы, которые не стакаются (maxCount=1)
		if (stack.getMaxCount() <= 1) {
			return 1;
		}
		
		// Зачарованные предметы, переименованные предметы не стакаются
		if (stack.hasEnchantments() || stack.hasCustomName() || stack.isDamaged()) {
			return 1;
		}
		
		return stack.getMaxCount();
	}
	
	/**
	 * Проверяет, можно ли стакать эти предметы (для зачарованных предметов и т.д.).
	 */
	private boolean canStackItems(ItemStack stack1, ItemStack stack2) {
		if (!ItemStack.areItemsEqual(stack1, stack2)) return false;

		// Зачарованные предметы, переименованные предметы не стакаются
		if (stack1.hasEnchantments() || stack2.hasEnchantments() ||
			stack1.hasCustomName() || stack2.hasCustomName() ||
			stack1.isDamaged() || stack2.isDamaged()) {
			return false;
		}
		
		// Предметы с тегом PlayerSold и без тега не стакаются
		boolean tag1 = com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isPlayerSold(stack1);
		boolean tag2 = com.unnameduser.tradeoverhaul.common.util.ItemTagHelper.isPlayerSold(stack2);
		if (tag1 != tag2) {
			return false;  // Один с тегом, другой без — не стакаются
		}

		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

	@Override
	public void onClosed(PlayerEntity player) {
		// Очищаем ссылку на жителя при закрытии, чтобы другие игроки могли открыть его
		villager = null;
		super.onClosed(player);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		if (villager != null && villager.isAlive() && !villager.isRemoved()) {
			return player.squaredDistanceTo(villager) <= 64.0; // 8 блоков
		}
		return false;
	}
}
