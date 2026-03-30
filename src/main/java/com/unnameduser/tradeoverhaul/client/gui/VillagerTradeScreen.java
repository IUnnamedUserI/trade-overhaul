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
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
		
		// Получаем профессию жителя для заголовка (больше не используется, но оставляем для совместимости)
		String professionKey = "";
		if (this.handler.getVillager() != null) {
			var villager = this.handler.getVillager();
			var profession = villager.getVillagerData().getProfession();
			if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE) {
				var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
				if (profId != null) {
					professionKey = " (" + net.minecraft.text.Text.translatable("entity.minecraft.villager." + profId.getPath()).getString() + ")";
				}
			}
		}
		// villagerInventoryLabel больше не используется для отрисовки
		this.villagerInventoryLabel = Text.literal("");

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
		String playerMoneyText = playerCurrency.formatMoneyVertical();
		context.drawText(this.textRenderer, playerMoneyText,
			x + this.renderPlayerGridX, labelY + 10, 0xFFFFFF, true);

		// === Инвентарь жителя ===
		int villagerX = x + this.renderVillagerX;
		
		// Получаем ожидаемый XP (из tooltip)
		float expectedXp = getExpectedXpForCurrentHover();

		// 1. Полоска прогресса (над заголовками, но с отступом)
		int level = handler.getProfessionLevel();
		float progress = handler.getProfessionLevelProgress();
		int xpForNext = handler.getXpForNextLevel();
		int experience = handler.getProfessionExperience();

		if (progress < 1.0f && xpForNext > 0) {
			int barWidth = VillagerTradeScreenHandler.GRID_COLS * 19;
			int barHeight = 4;
			int barX = villagerX;
			int barY = labelY - 7;

			// Фон полоски прогресса (тёмно-серый)
			context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF303030);

			// Заполненная часть (зелёная) - текущий опыт
			int filledWidth = (int) (barWidth * progress);
			context.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF40FF40);
			
			// Голубая полоса предпросмотра XP (если наведены на предмет)
			if (expectedXp > 0) {
				// Рассчитываем, где начнётся и сколько займёт голубая полоса
				float progressStart = progress;
				float progressGain = Math.min(1.0f - progressStart, expectedXp / xpForNext);
				
				int blueStartX = barX + filledWidth;
				int blueWidth = (int) (barWidth * progressGain);
				
				// Рисуем голубую полосу
				context.fill(blueStartX, barY, blueStartX + blueWidth, barY + barHeight, 0xFF00FFFF);
			}

			// Показатель XP (над полосой) с ожидаемым XP
			String expectedXpText = expectedXp > 0 ? String.format(" +%.2f", expectedXp) : "";
			Text xpText = Text.literal(String.format("%.2f", (float)experience) + expectedXpText + " / " + xpForNext + " XP");
			context.drawText(this.textRenderer, xpText, barX, barY - 9, 0x808080, true);
		} else if (level >= 5) {
			// Максимальный уровень (над заголовками)
			Text maxLevelText = Text.literal("§6§lМАКСИМАЛЬНЫЙ УРОВЕНЬ");
			context.drawText(this.textRenderer, maxLevelText, villagerX, labelY - 7, 0xFFAA00, true);
		}
		
		// 2. Уровень жителя (на позиции заголовка)
		String levelName = handler.getProfessionLevelName();
		String levelTitle = switch (levelName) {
			case "apprentice" -> "Подмастерье";
			case "journeyman" -> "Ремесленник";
			case "expert" -> "Эксперт";
			case "master" -> "Мастер";
			default -> "Новичок";
		};
		
		// Добавляем профессию в скобках
		String professionText = "";
		if (this.handler.getVillager() != null) {
			var villager = this.handler.getVillager();
			var profession = villager.getVillagerData().getProfession();
			if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE) {
				var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
				if (profId != null) {
					professionText = " (" + net.minecraft.text.Text.translatable("entity.minecraft.villager." + profId.getPath()).getString() + ")";
				}
			}
		}
		
		Text levelText = Text.literal(levelTitle + professionText + " (ур. " + level + ")");
		context.drawText(this.textRenderer, levelText, villagerX, labelY, 0xFFFFFF, true);
		
		// 3. Деньги жителя (горизонтально под уровнем)
		int wallet = handler.getSyncedWallet();
		com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent villagerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
		villagerCurrency.setTotalCopper(wallet);
		String villagerMoneyText = villagerCurrency.formatMoneyVertical();
		context.drawText(this.textRenderer, villagerMoneyText,
			villagerX, labelY + 10, 0xFFFFFF, true);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			ItemStack stack = this.focusedSlot.getStack();
			int slotIndex = this.focusedSlot.id;
			boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
			boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();

			// Получаем стандартный tooltip
			List<Text> tooltip = this.getTooltipFromItem(this.client, stack);

			// Добавляем цену только для слотов инвентаря жителя и игрока (не для брони)
			if (slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				// Покупка у жителя
				int buyQty = handler.getClientBuyQuantityForSlot(slotIndex);
				int price = handler.getClientBuyPrice(slotIndex);

				if (buyQty > 0 && price > 0) {
					// Цена за 1 предмет
					int pricePerItem = price / buyQty;
					if (pricePerItem <= 0) pricePerItem = 1;

					int wantToBuy = 1;
					if (shift) {
						wantToBuy = stack.getCount();
					} else if (ctrl) {
						wantToBuy = Math.min(10, stack.getCount());
					}

					// Проверяем, сколько может купить игрок
					int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
					int maxCanBuy = playerMoney / pricePerItem;
					int quantity = Math.min(wantToBuy, maxCanBuy);

					// Проверяем место в инвентаре
					int maxSpace = handler.clientHasInventorySpaceForStack(stack, wantToBuy) ? wantToBuy : 0;
					if (maxSpace < quantity) quantity = maxSpace;

					// Если у игрока нет денег, показываем цену за 1 предмет
					if (quantity <= 0) {
						quantity = 1;
					}

					String quantityText = " (" + quantity + ")";

					int totalCost = quantity * pricePerItem;
					boolean canAfford = playerMoney >= pricePerItem;
					boolean hasInventorySpace = handler.clientHasInventorySpaceForStack(stack, quantity);

					String colorCode = canAfford ? "§a" : "§c";

					// Для зачарованных книг добавляем информацию о зачаровании
					if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
						net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
						if (nbt != null && nbt.contains("StoredEnchantments", 10)) {
							net.minecraft.nbt.NbtList enchantList = nbt.getList("StoredEnchantments", 10);
							if (!enchantList.isEmpty()) {
								net.minecraft.nbt.NbtCompound enchantNbt = enchantList.getCompound(0);
								String enchantId = enchantNbt.getString("id");
								int level = enchantNbt.getShort("lvl");
								Identifier enchantIdentifier = Identifier.tryParse(enchantId);
								if (enchantIdentifier != null) {
									var enchant = Registries.ENCHANTMENT.get(enchantIdentifier);
									if (enchant != null) {
										String enchantName = enchant.getName(level).getString();
										tooltip.add(Text.literal("§7" + enchantName));
									}
								}
							}
						}
					}

					tooltip.add(Text.literal(colorCode + "Купить" + quantityText + ": " + NumismaticHelper.formatMoney(totalCost)));

					if (!hasInventorySpace && quantity > 0) {
						tooltip.add(Text.literal("§c§oНет места в инвентаре"));
					}
				}
			} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX && slotIndex < VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				// Продажа жителю (инвентарь игрока, не броня)
				int sellQty = handler.getClientSellQuantity(stack);
				int price = handler.getClientSellPrice(stack);
				
				if (sellQty > 0 && price > 0) {
					// Цена за 1 предмет
					int pricePerItem = price / sellQty;
					if (pricePerItem <= 0) pricePerItem = 1;
					
					int wantToSell = 1;
					if (shift) {
						wantToSell = stack.getCount();
					} else if (ctrl) {
						wantToSell = Math.min(10, stack.getCount());
					}
					
					// Проверяем, сколько может купить житель
					int villagerMoney = handler.getSyncedWallet();
					int maxCanBuy = villagerMoney / pricePerItem;
					int quantity = Math.min(wantToSell, maxCanBuy);
					
					// Если у жителя нет денег, показываем цену за 1 предмет
					if (quantity <= 0) {
						quantity = 1;
					}
					
					String quantityText = " (" + quantity + ")";
					
					int totalEarned = quantity * pricePerItem;
					boolean canAfford = villagerMoney >= pricePerItem;
					String colorCode = canAfford ? "§a" : "§c";
					
					// Для зачарованных книг добавляем информацию о зачаровании
					if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
						net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
						if (nbt != null && nbt.contains("StoredEnchantments", 10)) {
							net.minecraft.nbt.NbtList enchantList = nbt.getList("StoredEnchantments", 10);
							if (!enchantList.isEmpty()) {
								net.minecraft.nbt.NbtCompound enchantNbt = enchantList.getCompound(0);
								String enchantId = enchantNbt.getString("id");
								int level = enchantNbt.getShort("lvl");
								Identifier enchantIdentifier = Identifier.tryParse(enchantId);
								if (enchantIdentifier != null) {
									var enchant = Registries.ENCHANTMENT.get(enchantIdentifier);
									if (enchant != null) {
										String enchantName = enchant.getName(level).getString();
										tooltip.add(Text.literal("§7" + enchantName));
									}
								}
							}
						}
					}

					tooltip.add(Text.literal(colorCode + "Продать" + quantityText + ": " + NumismaticHelper.formatMoney(totalEarned)));
				}
			} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX && slotIndex < VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
				// Броня и вторая рука - тоже показываем цену продажи
				int sellQty = handler.getClientSellQuantity(stack);
				int price = handler.getClientSellPrice(stack);
				
				if (sellQty > 0 && price > 0) {
					// Цена за 1 предмет
					int pricePerItem = price / sellQty;
					if (pricePerItem <= 0) pricePerItem = 1;
					
					int wantToSell = 1;
					if (shift) {
						wantToSell = stack.getCount();
					} else if (ctrl) {
						wantToSell = Math.min(10, stack.getCount());
					}
					
					// Проверяем, сколько может купить житель
					int villagerMoney = handler.getSyncedWallet();
					int maxCanBuy = villagerMoney / pricePerItem;
					int quantity = Math.min(wantToSell, maxCanBuy);
					
					// Если у жителя нет денег, показываем цену за 1 предмет
					if (quantity <= 0) {
						quantity = 1;
					}
					
					String quantityText = " (" + quantity + ")";
					
					int totalEarned = quantity * pricePerItem;
					boolean canAfford = villagerMoney >= pricePerItem;
					String colorCode = canAfford ? "§a" : "§c";

					tooltip.add(Text.literal(colorCode + "Продать" + quantityText + ": " + NumismaticHelper.formatMoney(totalEarned)));
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

		// Highlight slots with buyable items
		drawBuyableItemHighlights(context, x, y);
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

				// ПКМ = 1 предмет, Ctrl+ПКМ = 10 предметов, Shift+ПКМ = весь стак
				boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
				boolean ctrlPressed = net.minecraft.client.gui.screen.Screen.hasControlDown();
				boolean sellWholeStack = shiftPressed && !buying;
				boolean buyWholeStack = shiftPressed && buying;
				boolean buyTen = ctrlPressed;

				var buf = PacketByteBufs.create();
				new TradePayload(handler.syncId, slotIndex, buying, sellWholeStack, buyWholeStack, buyTen).write(buf);
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
	
	// Цвета рамок для предметов, которые можно продать жителю
	private static final int SLOT_BORDER_CAN_AFFORD = 0xFF00FFFF; // Светло-голубой (хватает денег)
	private static final int SLOT_BORDER_CANNOT_AFFORD = 0xFFFF5555; // Красный (не хватает денег)

	/**
	 * Подсвечивает слоты инвентаря игрока, содержащие предметы, которые житель может купить.
	 * Светло-голубая рамка - хватает денег на покупку.
	 * Красная рамка - не хватает денег.
	 */
	private void drawBuyableItemHighlights(DrawContext context, int x, int y) {
		int slotSize = 19;
		int slotStep = 19;
		int armorStep = 20;

		// Получаем деньги жителя
		int villagerMoney = handler.getSyncedWallet();

		// Проверяем слоты инвентаря игрока (не броню)
		for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
			for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
				int gridIndex = row * VillagerTradeScreenHandler.GRID_COLS + col;
				int slotIndex = VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX + gridIndex;

				Slot slot = this.handler.getSlot(slotIndex);
				if (slot != null && slot.hasStack()) {
					ItemStack stack = slot.getStack();
					// Проверяем, может ли житель купить этот предмет
					if (handler.canVillagerBuyItem(stack)) {
						// Получаем цену продажи (сколько житель заплатит за 1 предмет)
						int sellQty = handler.getClientSellQuantity(stack);
						int sellPrice = handler.getClientSellPrice(stack);
						
						// Цена за 1 предмет
						int pricePerItem = sellQty > 0 ? sellPrice / sellQty : 0;
						if (pricePerItem <= 0) pricePerItem = 1;
						
						// Определяем цвет рамки: хватает ли денег хотя бы на 1 предмет
						int borderColor = villagerMoney >= pricePerItem ? SLOT_BORDER_CAN_AFFORD : SLOT_BORDER_CANNOT_AFFORD;
						
						int slotX = x + this.renderPlayerGridX + col * slotStep;
						int slotY = y + this.layoutPanelY + row * slotStep - 1;
						
						// Рисуем цветную рамку (все линии на 1 пиксель короче)
						context.fill(slotX, slotY, slotX + slotSize - 1, slotY + 1, borderColor); // Top
						context.fill(slotX + 1, slotY + slotSize - 2, slotX + slotSize - 1, slotY + slotSize - 1, borderColor); // Bottom
						context.fill(slotX, slotY + 1, slotX + 1, slotY + slotSize - 1, borderColor); // Left
						context.fill(slotX + slotSize - 2, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, borderColor); // Right
					}
				}
			}
		}

		// Проверяем слоты брони
		for (int i = 0; i < VillagerTradeScreenHandler.ARMOR_SLOT_COUNT; i++) {
			int slotIndex = VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX + i;
			Slot slot = this.handler.getSlot(slotIndex);
			if (slot != null && slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (handler.canVillagerBuyItem(stack)) {
					// Получаем цену продажи (сколько житель заплатит за 1 предмет)
					int sellQty = handler.getClientSellQuantity(stack);
					int sellPrice = handler.getClientSellPrice(stack);

					// Цена за 1 предмет
					int pricePerItem = sellQty > 0 ? sellPrice / sellQty : 0;
					if (pricePerItem <= 0) pricePerItem = 1;

					// Определяем цвет рамки: хватает ли денег хотя бы на 1 предмет
					int borderColor = villagerMoney >= pricePerItem ? SLOT_BORDER_CAN_AFFORD : SLOT_BORDER_CANNOT_AFFORD;

					int slotX = x + this.renderArmorX;
					int slotY = y + this.layoutPanelY + i * armorStep - 1;

					// Рисуем цветную рамку (все линии на 1 пиксель короче)
					context.fill(slotX, slotY, slotX + slotSize - 1, slotY + 1, borderColor); // Top
					context.fill(slotX + 1, slotY + slotSize - 2, slotX + slotSize - 1, slotY + slotSize - 1, borderColor); // Bottom
					context.fill(slotX, slotY + 1, slotX + 1, slotY + slotSize - 1, borderColor); // Left
					context.fill(slotX + slotSize - 2, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, borderColor); // Right
				}
			}
		}
	}
	
	/**
	 * Получает ожидаемый XP для текущего наведения (используется в drawForeground)
	 */
	private float getExpectedXpForCurrentHover() {
		// Проверяем, наведены ли на слот инвентаря жителя
		if (this.focusedSlot == null || !this.focusedSlot.hasStack()) {
			return 0f;
		}
		
		int slotIndex = this.focusedSlot.id;
		if (slotIndex < VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
			return 0f;
		}
		
		ItemStack stack = this.focusedSlot.getStack();
		boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
		boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();
		
		// Получаем цену
		int buyQty = handler.getClientBuyQuantityForSlot(slotIndex);
		int price = handler.getClientBuyPrice(slotIndex);
		if (buyQty <= 0 || price <= 0) {
			return 0f;
		}
		
		int pricePerItem = price / buyQty;
		if (pricePerItem <= 0) pricePerItem = 1;
		
		// Определяем количество
		int wantToBuy = 1;
		if (shift) {
			wantToBuy = stack.getCount();
		} else if (ctrl) {
			wantToBuy = Math.min(10, stack.getCount());
		}
		
		// Проверяем деньги
		int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
		int maxCanBuy = playerMoney / pricePerItem;
		int quantity = Math.min(wantToBuy, maxCanBuy);
		
		// Проверяем место
		int maxSpace = handler.clientHasInventorySpaceForStack(stack, wantToBuy) ? wantToBuy : 0;
		if (maxSpace < quantity) quantity = maxSpace;
		
		if (quantity <= 0) {
			return 0f;
		}
		
		// Рассчитываем XP
		float xp = handler.getExpectedXpForBuy(slotIndex, quantity);
		return xp;
	}
}
