package com.unnameduser.tradeoverhaul.client.gui;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.network.TradePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class VillagerTradeScreen extends HandledScreen<VillagerTradeScreenHandler> {
	private static final int BG_COLOR_TOP = 0xC6C6C6;  // Светло-серый (как ванильный GUI)
	private static final int BG_COLOR_BOTTOM = 0x8B8B8B; // Тёмно-серый для градиента
	private static final int SLOT_BG_COLOR = 0x8B8B8B8B; // Полупрозрачный фон слотов
	private static final int BORDER_COLOR = 0xFF555555; // Тёмная рамка
	private static final int CENTER_PANEL_COLOR = 0xA0A0A0A0; // Полупрозрачный центр
	private static final int WALLET_BG_COLOR = 0xFF404040; // Фон кошелька
	private static final int WALLET_TEXT_COLOR = 0xFF55FF55; // Зелёный для изумрудов

	private int layoutCenterStart;
	private int layoutCenterWidth;
	private int layoutPanelY;
	
	private Text playerInventoryLabel;
	private Text villagerInventoryLabel;
	private int openDelayTicks = 10;

	public VillagerTradeScreen(VillagerTradeScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		// Динамический размер будет установлен в init()
	}

	private static int[] playerColRow(int invIndex) {
		if (invIndex >= 9 && invIndex <= 17) {
			return new int[]{0, invIndex - 9};
		}
		if (invIndex >= 18 && invIndex <= 26) {
			return new int[]{1, invIndex - 18};
		}
		if (invIndex >= 27 && invIndex <= 35) {
			return new int[]{2, invIndex - 27};
		}
		if (invIndex >= 0 && invIndex <= 8) {
			return new int[]{3, invIndex};
		}
		return new int[]{0, 0};
	}

	@Override
	protected void init() {
		// Устанавливаем динамический размер: 90% ширины, 85% высоты экрана
		this.backgroundWidth = (int) (this.client.getWindow().getScaledWidth() * 0.90);
		this.backgroundHeight = (int) (this.client.getWindow().getScaledHeight() * 0.85);
		
		super.init();
		this.playerInventoryLabel = Text.translatable("gui.tradeoverhaul.player_inventory", this.client.player.getDisplayName());
		this.villagerInventoryLabel = Text.translatable("gui.tradeoverhaul.villager_inventory", this.handler.getVillager() != null ? this.handler.getVillager().getCustomName() : Text.literal("Trader"));
		
		// Пересчитываем позиции для динамического размера
		int slot = 18;
		int gridW = VillagerTradeScreenHandler.GRID_COLS * slot;
		int gap = 24;
		int minCenter = 72;
		int contentW = gridW + gap + minCenter + gap + gridW;
		int marginX = Math.max(16, (this.backgroundWidth - contentW) / 2);
		this.layoutCenterWidth = this.backgroundWidth - marginX * 2 - gridW * 2 - gap * 2;
		this.layoutCenterWidth = Math.max(minCenter, this.layoutCenterWidth);

		int playerX = marginX;
		this.layoutCenterStart = playerX + gridW + gap;
		int villagerX = this.layoutCenterStart + this.layoutCenterWidth + gap;

		this.layoutPanelY = 42 + (this.backgroundHeight - 42 - VillagerTradeScreenHandler.GRID_ROWS * slot - 28) / 3;
		this.layoutPanelY = Math.max(38, Math.min(this.layoutPanelY, this.backgroundHeight - VillagerTradeScreenHandler.GRID_ROWS * slot - 28));

		for (int i = 0; i < this.handler.slots.size(); i++) {
			Slot s = this.handler.slots.get(i);
			if (i < VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				int[] cr = playerColRow(s.getIndex());
				s.x = playerX + cr[0] * slot;
				s.y = this.layoutPanelY + cr[1] * slot;
			} else {
				int inv = s.getIndex();
				int col = inv % VillagerTradeScreenHandler.GRID_COLS;
				int row = inv / VillagerTradeScreenHandler.GRID_COLS;
				s.x = villagerX + col * slot;
				s.y = this.layoutPanelY + row * slot;
			}
		}

		this.playerInventoryTitleX = playerX;
		this.playerInventoryTitleY = this.layoutPanelY - 12;
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
		int labelY = y + this.layoutPanelY - 16;
		context.drawText(this.textRenderer, this.playerInventoryLabel,
			x + this.layoutCenterStart - 72, labelY, 0xFFFFFF, true);
		context.drawText(this.textRenderer, this.villagerInventoryLabel,
			x + this.layoutCenterStart + this.layoutCenterWidth, labelY, 0xFFFFFF, true);
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

		// Draw center panel background (between inventories)
		int centerX = x + this.layoutCenterStart - 24;
		int centerWidth = this.layoutCenterWidth + 48;
		context.fill(centerX, y + this.layoutPanelY - 2,
			centerX + centerWidth, y + this.layoutPanelY + VillagerTradeScreenHandler.GRID_ROWS * 18 + 2,
			CENTER_PANEL_COLOR);

		// Draw vertical separators
		int separatorX1 = x + this.layoutCenterStart - 24;
		context.fill(separatorX1, y + this.layoutPanelY - 2,
			separatorX1 + 2, y + this.layoutPanelY + VillagerTradeScreenHandler.GRID_ROWS * 18 + 2,
			BORDER_COLOR);

		int separatorX2 = x + this.layoutCenterStart + this.layoutCenterWidth + 24 - 2;
		context.fill(separatorX2, y + this.layoutPanelY - 2,
			separatorX2 + 2, y + this.layoutPanelY + VillagerTradeScreenHandler.GRID_ROWS * 18 + 2,
			BORDER_COLOR);

		// Draw wallet/info panel in center
		drawCenterInfoPanel(context, x, y);

		// Draw slot backgrounds (positions match slot positions from init())
		drawSlotBackgrounds(context, x, y);
	}
	
	private void drawCenterInfoPanel(DrawContext context, int x, int y) {
		int centerX = x + this.layoutCenterStart;
		int centerWidth = this.layoutCenterWidth;
		int walletBoxWidth = 120;
		int walletBoxHeight = 40;
		int walletX = centerX + (centerWidth - walletBoxWidth) / 2;
		int walletY = y + this.layoutPanelY + VillagerTradeScreenHandler.GRID_ROWS * 18 - walletBoxHeight - 4;
		
		// Wallet background
		context.fill(walletX, walletY, walletX + walletBoxWidth, walletY + walletBoxHeight, WALLET_BG_COLOR);
		context.fill(walletX + 1, walletY + 1, walletX + walletBoxWidth - 1, walletY + walletBoxHeight - 1, 0xFF303030);
		
		// Wallet text
		String walletText = String.valueOf(handler.getSyncedWallet());
		int textWidth = this.textRenderer.getWidth(walletText);
		int textX = walletX + (walletBoxWidth - textWidth) / 2;
		int textY = walletY + (walletBoxHeight - 8) / 2;
		context.drawText(this.textRenderer, walletText, textX, textY, WALLET_TEXT_COLOR, false);
		
		// Price display for hovered item
		if (this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty()) {
			int slotIndex = this.focusedSlot.id;
			boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
			String priceText = "";
			int priceColor = 0xFFFFFF;
			
			if (slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				// Buying from villager
				int buy = handler.getClientBuyPriceForSlot(slotIndex);
				if (buy > 0) {
					priceText = "Купить: " + buy;
					priceColor = handler.clientCanAffordBuy(slotIndex) ? 0x55FF55 : 0xFF5555;
				}
			} else {
				// Selling to villager
				int shown = handler.getDisplayedSellCost(this.focusedSlot.getStack(), shift);
				if (shown > 0) {
					priceText = "Продать: " + shown;
					priceColor = handler.villagerCanAffordSellDisplay(this.focusedSlot.getStack(), shift) ? 0x55FFFF : 0xFF5555;
				}
			}
			
			if (!priceText.isEmpty()) {
				int pTextWidth = this.textRenderer.getWidth(priceText);
				int pTextX = centerX + (centerWidth - pTextWidth) / 2;
				int pTextY = walletY + walletBoxHeight + 4;
				context.drawText(this.textRenderer, priceText, pTextX, pTextY, priceColor, false);
			}
		}
	}
	
	private void drawSlotBackgrounds(DrawContext context, int x, int y) {
		// Player inventory slot backgrounds (positions match init())
		int playerX = x + this.layoutCenterStart - 72;
		for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
			for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
				int slotX = playerX + col * 18;
				int slotY = y + this.layoutPanelY + row * 18;
				context.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_BG_COLOR);
				context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF303030);
			}
		}

		// Villager inventory slot backgrounds (positions match init())
		int villagerX = x + this.layoutCenterStart + this.layoutCenterWidth;
		for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
			for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
				int slotX = villagerX + col * 18;
				int slotY = y + this.layoutPanelY + row * 18;
				context.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_BG_COLOR);
				context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF303030);
			}
		}
	}

	private Slot slotAt(double mouseX, double mouseY) {
		int gx = (this.width - this.backgroundWidth) / 2;
		int gy = (this.height - this.backgroundHeight) / 2;
		double lx = mouseX - gx;
		double ly = mouseY - gy;
		for (Slot slot : this.handler.slots) {
			if (lx >= slot.x && lx < slot.x + 16 && ly >= slot.y && ly < slot.y + 16) {
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
				boolean bulkSell = net.minecraft.client.gui.screen.Screen.hasShiftDown() && !buying;
				var buf = PacketByteBufs.create();
				new TradePayload(handler.syncId, slotIndex, buying, bulkSell).write(buf);
				ClientPlayNetworking.send(TradePayload.ID, buf);
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
		// Кошелёк и цена рисуются в drawBackground через drawCenterInfoPanel
	}
}
