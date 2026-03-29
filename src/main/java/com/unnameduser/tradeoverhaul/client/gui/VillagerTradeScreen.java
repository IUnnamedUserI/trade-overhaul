package com.unnameduser.tradeoverhaul.client.gui;

import java.awt.Color;
import java.util.List;

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Red;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.network.TradePayload;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class VillagerTradeScreen extends HandledScreen<VillagerTradeScreenHandler> {
	private static final int BG_COLOR_TOP = 0xC6C6C6;  // Светло-серый (как ванильный GUI)
	private static final int BG_COLOR_BOTTOM = 0x8B8B8B; // Тёмно-серый для градиента
	private static final int SLOT_BG_COLOR = 0x8B8B8B8B; // Полупрозрачный фон слотов
	private static final int BORDER_COLOR = 0xFF555555; // Тёмная рамка

	private int layoutPanelY;
	
	// Позиции для отрисовки (вычисляются в init(), используются в drawSlotBackgrounds)
	private int renderArmorX;
	private int renderPlayerGridX;
	private int renderVillagerX;

	private Text playerInventoryLabel;
	private Text villagerInventoryLabel;
	private int openDelayTicks = 10;

	public VillagerTradeScreen(VillagerTradeScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		// Динамический размер будет установлен в init()
	}

	@Override
	protected void init() {
		// Устанавливаем динамический размер: 90% ширины, 85% высоты экрана
		this.backgroundWidth = (int) (this.client.getWindow().getScaledWidth() * 0.90);
		this.backgroundHeight = (int) (this.client.getWindow().getScaledHeight() * 0.85);

		super.init();
		this.playerInventoryLabel = Text.translatable("gui.tradeoverhaul.player_inventory", this.client.player.getDisplayName());
		
		// Получаем профессию жителя для заголовка
		String professionKey = "";
		if (this.handler.getVillager() != null) {
			var villager = this.handler.getVillager();
			var profession = villager.getVillagerData().getProfession();
			if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE) {
				// Получаем перевод профессии через registry
				var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
				if (profId != null) {
					professionKey = " (" + net.minecraft.text.Text.translatable("entity.minecraft.villager." + profId.getPath()).getString() + ")";
				}
			}
		}
		Text villagerName = this.handler.getVillager() != null ? this.handler.getVillager().getCustomName() : Text.literal("Trader");
		this.villagerInventoryLabel = Text.translatable("gui.tradeoverhaul.villager_inventory", villagerName.getString() + professionKey);

		// Пересчитываем позиции для динамического размера
		int slot = 18;
		int slotStep = 19;  // Шаг для слотов (на 1 пиксель больше для отображения)
		int armorW = VillagerTradeScreenHandler.ARMOR_SLOT_WIDTH;
		int armorGridGap = 16; // зазор между бронёй и сеткой
		int gridW = VillagerTradeScreenHandler.GRID_COLS * slot;
		int gapBetweenGrids = 64; // зазор между сетками (бывший центр)

		// contentW = броня + зазор + сетка игрока + зазор + сетка жителя
		int contentW = armorW + armorGridGap + gridW + gapBetweenGrids + gridW;
		int marginX = Math.max(16, (this.backgroundWidth - contentW) / 2);

		// Позиции X
		int armorX = marginX;
		int playerGridX = armorX + armorW + armorGridGap;
		int villagerX = playerGridX + gridW + gapBetweenGrids;

		// Сохраняем позиции для отрисовки фона
		this.renderArmorX = armorX;
		this.renderPlayerGridX = playerGridX;
		this.renderVillagerX = villagerX;

		// Позиция Y панели (чуть выше для места под кошелёк)
		this.layoutPanelY = 48 + (this.backgroundHeight - 48 - VillagerTradeScreenHandler.GRID_ROWS * slot - 28) / 3;
		this.layoutPanelY = Math.max(44, Math.min(this.layoutPanelY, this.backgroundHeight - VillagerTradeScreenHandler.GRID_ROWS * slot - 28));

		// Расставляем слоты
		for (int i = 0; i < this.handler.slots.size(); i++) {
			Slot s = this.handler.slots.get(i);
			
			if (i < VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX + VillagerTradeScreenHandler.ARMOR_SLOT_COUNT) {
				// Слоты брони (0-4)
				int armorIndex = i - VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX;
				s.x = armorX + 1;  // +1 пиксель вправо
				s.y = this.layoutPanelY + armorIndex * slotStep + armorIndex;  // +1 пиксель за каждый ряд вниз
			} else if (i < VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX + VillagerTradeScreenHandler.PLAYER_GRID_SLOTS) {
				// Основная сетка игрока (5-40)
				int gridIndex = i - VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX;
				int col = gridIndex % VillagerTradeScreenHandler.GRID_COLS;
				int row = gridIndex / VillagerTradeScreenHandler.GRID_COLS;
				s.x = playerGridX + col * slotStep + 1;  // +1 пиксель вправо
				s.y = this.layoutPanelY + row * slotStep;
			} else {
				// Инвентарь жителя (41-76)
				int invIndex = i - VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX;
				int col = invIndex % VillagerTradeScreenHandler.GRID_COLS;
				int row = invIndex / VillagerTradeScreenHandler.GRID_COLS;
				s.x = villagerX + col * slotStep + 1;  // +1 пиксель вправо
				s.y = this.layoutPanelY + row * slotStep;
			}
		}

		this.playerInventoryTitleX = playerGridX;
		this.playerInventoryTitleY = this.layoutPanelY - 20;  // Выше
		this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		if (openDelayTicks > 0) {
			openDelayTicks--;
			return;
		}
		if (handler.shouldClose()) {
			this.close();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);

		// Draw inventory labels (светлый текст)
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;
		int labelY = y + this.layoutPanelY - 20;

		// Заголовок инвентаря игрока
		context.drawText(this.textRenderer, this.playerInventoryLabel,
			x + this.renderPlayerGridX, labelY, 0xFFFFFF, true);

		// Деньги игрока (горизонтально под заголовком)
		int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
		com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
		playerCurrency.setTotalCopper(playerMoney);
		String playerMoneyText = String.format("§6ЗМ: §f%d §fСМ: §f%d §fММ: §f%d", 
			playerCurrency.getGold(), playerCurrency.getSilver(), playerCurrency.getCopper());
		context.drawText(this.textRenderer, playerMoneyText,
			x + this.renderPlayerGridX, labelY + 10, 0xFFFFFF, true);

		// Заголовок инвентаря жителя
		context.drawText(this.textRenderer, this.villagerInventoryLabel,
			x + this.renderVillagerX, labelY, 0xFFFFFF, true);

		// Деньги жителя (горизонтально под заголовком)
		int wallet = handler.getSyncedWallet();
		com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent villagerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
		villagerCurrency.setTotalCopper(wallet);
		String villagerMoneyText = String.format("§6ЗМ: §f%d §fСМ: §f%d §fММ: §f%d", 
			villagerCurrency.getGold(), villagerCurrency.getSilver(), villagerCurrency.getCopper());
		context.drawText(this.textRenderer, villagerMoneyText,
			x + this.renderVillagerX, labelY + 10, 0xFFFFFF, true);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			ItemStack stack = this.focusedSlot.getStack();
			int slotIndex = this.focusedSlot.id;
			boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();

			// Получаем стандартный tooltip
			List<Text> tooltip = this.getTooltipFromItem(this.client, stack);

			// Добавляем цену только в этом GUI
			if (slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				// Покупка у жителя
				int buyQty = handler.getClientBuyQuantityForSlot(slotIndex);
				int price = handler.getClientBuyPrice(slotIndex);
				
				TradeOverhaulMod.LOGGER.info("Tooltip BUY: slotIndex={}, buyQty={}, price={}, stack={}", 
					slotIndex, buyQty, price, stack.getItem().getTranslationKey());
				
				if (buyQty > 0 && price > 0) {
					// Цена за 1 предмет
					int pricePerItem = price / buyQty;
					if (pricePerItem <= 0) pricePerItem = 1;
					
					boolean canAfford = handler.clientCanAffordBuy(pricePerItem);
					boolean hasInventorySpace = handler.clientHasInventorySpaceForStack(stack, 1);

					String colorCode = canAfford ? "§a" : "§c";

					tooltip.add(Text.literal("§61 шт. = " + NumismaticHelper.formatMoney(pricePerItem)));
					tooltip.add(Text.literal(colorCode + "Купить: " + NumismaticHelper.formatMoney(pricePerItem)));

					// Показываем цену за весь стак только при зажатом Shift
					if (shift) {
						int stackSize = stack.getCount();
						int totalCost = stackSize * pricePerItem;
						tooltip.add(Text.literal("§7(Shift) Весь стак (" + stackSize + " шт.): " + NumismaticHelper.formatMoney(totalCost)));
					}

					if (!hasInventorySpace) {
						tooltip.add(Text.literal("§c§oНет места в инвентаре"));
					}
				}
			} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
				// Продажа жителю (инвентарь игрока)
				int sellQty = handler.getClientSellQuantity(stack);
				int price = handler.getClientSellPrice(stack);
				
				TradeOverhaulMod.LOGGER.info("Tooltip SELL: sellQty={}, price={}, stack={}", 
					sellQty, price, stack.getItem().getTranslationKey());
				
				if (sellQty > 0 && price > 0) {
					// Цена за 1 предмет
					int pricePerItem = price / sellQty;
					if (pricePerItem <= 0) pricePerItem = 1;
					
					int totalEarned = shift ? (stack.getCount() * pricePerItem) : pricePerItem;
					int totalItems = shift ? stack.getCount() : 1;
					boolean canAfford = handler.villagerCanAffordSell(totalEarned);
					String colorCode = canAfford ? "§b" : "§c";

					tooltip.add(Text.literal("§61 шт. = " + NumismaticHelper.formatMoney(pricePerItem)));
					tooltip.add(Text.literal(colorCode + "Продать: " + NumismaticHelper.formatMoney(pricePerItem)));

					// Показываем цену за весь стак только при зажатом Shift
					if (shift) {
						tooltip.add(Text.literal("§7(Shift) Весь стак (" + stack.getCount() + " шт.): " + NumismaticHelper.formatMoney(totalEarned)));
					}
				}
			}

			context.drawTooltip(this.textRenderer, tooltip, x, y);
			return;
		}
		super.drawMouseoverTooltip(context, x, y);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;

		// Draw main background gradient
		context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight,
			BG_COLOR_TOP, BG_COLOR_BOTTOM);

		// Draw border
		context.fill(x, y, x + this.backgroundWidth, y + 2, BORDER_COLOR); // Top
		context.fill(x, y + this.backgroundHeight - 2, x + this.backgroundWidth, y + this.backgroundHeight, BORDER_COLOR); // Bottom
		context.fill(x, y, x + 2, y + this.backgroundHeight, BORDER_COLOR); // Left
		context.fill(x + this.backgroundWidth - 2, y, x + this.backgroundWidth, y + this.backgroundHeight, BORDER_COLOR); // Right

		// Draw inner border
		context.fill(x + 2, y + 2, x + 3, y + this.backgroundHeight - 2, 0xFFA0A0A0); // Left inner highlight
		context.fill(x + 3, y + 2, x + this.backgroundWidth - 3, y + 4, 0xFFA0A0A0); // Top inner highlight
		context.fill(x + this.backgroundWidth - 3, y + 3, x + this.backgroundWidth - 2, y + this.backgroundHeight - 2, 0xFF404040); // Right inner shadow
		context.fill(x + 3, y + this.backgroundHeight - 3, x + this.backgroundWidth - 3, y + this.backgroundHeight - 2, 0xFF404040); // Bottom inner shadow

		// Draw slot backgrounds
		drawSlotBackgrounds(context, x, y);
	}

	private void drawSlotBackgrounds(DrawContext context, int x, int y) {
		int slotSize = 19; // Размер слота: 19x19 (на 1 пиксель больше стандарта)
		int slotStep = 19; // Шаг позиционирования слотов сетки
		int armorStep = 20; // Шаг позиционирования слотов брони (на 1 пиксель больше)

		// Armor slot backgrounds (5 slots vertically on the left)
		int armorX = x + this.renderArmorX;
		for (int i = 0; i < VillagerTradeScreenHandler.ARMOR_SLOT_COUNT; i++) {
			int slotX = armorX;
			int slotY = y + this.layoutPanelY + i * armorStep - 1;
			context.fill(slotX, slotY, slotX + slotSize - 1, slotY + slotSize - 1, SLOT_BG_COLOR);
			context.fill(slotX + 1, slotY + 1, slotX + slotSize - 2, slotY + slotSize - 2, 0xFF303030);
		}

		// Player main grid slot backgrounds (6×6)
		int playerGridX = x + this.renderPlayerGridX;
		for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
			for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
				int slotX = playerGridX + col * slotStep;
				int slotY = y + this.layoutPanelY + row * slotStep - 1;
				context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, SLOT_BG_COLOR);
				context.fill(slotX + 1, slotY + 1, slotX + slotSize - 2, slotY + slotSize - 2, 0xFF303030);
			}
		}

		// Villager inventory slot backgrounds (6×6)
		int villagerX = x + this.renderVillagerX;
		for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
			for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
				int slotX = villagerX + col * slotStep;
				int slotY = y + this.layoutPanelY + row * slotStep - 1;
				context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, SLOT_BG_COLOR);
				context.fill(slotX + 1, slotY + 1, slotX + slotSize - 2, slotY + slotSize - 2, 0xFF303030);
			}
		}
	}

	private Slot slotAt(double mouseX, double mouseY) {
		int gx = (this.width - this.backgroundWidth) / 2;
		int gy = (this.height - this.backgroundHeight) / 2;
		double lx = mouseX - gx;
		double ly = mouseY - gy;
		for (Slot slot : this.handler.slots) {
			if (lx >= slot.x && lx < slot.x + 18 && ly >= slot.y && ly < slot.y + 18) {
				return slot;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1) {
			Slot slot = slotAt(mouseX, mouseY);
			if (slot != null) {
				int slotIndex = slot.id;
				boolean buying = slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX;
				if (buying && slot.getStack().isEmpty()) {
					return super.mouseClicked(mouseX, mouseY, button);
				}
				if (!buying && slot.getStack().isEmpty()) {
					return super.mouseClicked(mouseX, mouseY, button);
				}
				
				// При покупке (ПКМ по слоту жителя) всегда покупаем по buyQuantity, 
				// а при Shift+ПКМ покупаем максимально возможное количество
				// При продаже (ПКМ по слоту игрока) продаем 1 пакет (sellQuantity), 
				// а при Shift+ПКМ продаем максимально возможное количество
				boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
				boolean sellWholeStack = shiftPressed && !buying;
				boolean buyWholeStack = shiftPressed && buying;
				
				var buf = PacketByteBufs.create();
				new TradePayload(handler.syncId, slotIndex, buying, sellWholeStack, buyWholeStack).write(buf);
				ClientPlayNetworking.send(TradePayload.ID, buf);
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		// Не рисуем стандартный заголовок (title), так как у нас свои кастомные заголовки
		// Стандартный заголовок "Инвентарь" нам не нужен
	}
}
