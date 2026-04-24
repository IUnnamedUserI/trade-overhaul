package com.unnameduser.tradeoverhaul.client.gui;

import java.util.List;

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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class VillagerTradeScreen extends HandledScreen<VillagerTradeScreenHandler> {
	private static final int BG_COLOR_TOP = 0xC6C6C6;
	private static final int BG_COLOR_BOTTOM = 0x8B8B8B;
	private static final int SLOT_BG_COLOR = 0x8B8B8B8B;
	private static final int BORDER_COLOR = 0xFF555555;

	private int layoutPanelY;
	private int renderArmorX;
	private int renderPlayerGridX;
	private int renderVillagerX;

	private Text playerInventoryLabel;
	private int openDelayTicks = 10;

	public VillagerTradeScreen(VillagerTradeScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		this.backgroundWidth = (int) (this.client.getWindow().getScaledWidth() * 0.90);
		this.backgroundHeight = (int) (this.client.getWindow().getScaledHeight() * 0.85);

		super.init();
		this.playerInventoryLabel = Text.translatable("gui.tradeoverhaul.player_inventory", this.client.player.getDisplayName());

		int slot = 18;
		int slotStep = 19;
		int armorW = VillagerTradeScreenHandler.ARMOR_SLOT_WIDTH;
		int armorGridGap = 16;
		int gridW = VillagerTradeScreenHandler.GRID_COLS * slot;
		int gapBetweenGrids = 64;

		int contentW = armorW + armorGridGap + gridW + gapBetweenGrids + gridW;
		int marginX = Math.max(16, (this.backgroundWidth - contentW) / 2);

		int armorX = marginX;
		int playerGridX = armorX + armorW + armorGridGap;
		int villagerX = playerGridX + gridW + gapBetweenGrids;

		this.renderArmorX = armorX;
		this.renderPlayerGridX = playerGridX;
		this.renderVillagerX = villagerX;

		this.layoutPanelY = 48 + (this.backgroundHeight - 48 - VillagerTradeScreenHandler.GRID_ROWS * slot - 28) / 3;
		this.layoutPanelY = Math.max(44, Math.min(this.layoutPanelY, this.backgroundHeight - VillagerTradeScreenHandler.GRID_ROWS * slot - 28));

		for (int i = 0; i < this.handler.slots.size(); i++) {
			Slot s = this.handler.slots.get(i);
			if (i < VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX + VillagerTradeScreenHandler.ARMOR_SLOT_COUNT) {
				int armorIndex = i - VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX;
				s.x = armorX + 1;
				s.y = this.layoutPanelY + armorIndex * slotStep + armorIndex;
			} else if (i < VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX + VillagerTradeScreenHandler.PLAYER_GRID_SLOTS) {
				int gridIndex = i - VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX;
				int col = gridIndex % VillagerTradeScreenHandler.GRID_COLS;
				int row = gridIndex / VillagerTradeScreenHandler.GRID_COLS;
				s.x = playerGridX + col * slotStep + 1;
				s.y = this.layoutPanelY + row * slotStep;
			} else {
				int invIndex = i - VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX;
				int col = invIndex % VillagerTradeScreenHandler.GRID_COLS;
				int row = invIndex / VillagerTradeScreenHandler.GRID_COLS;
				s.x = villagerX + col * slotStep + 1;
				s.y = this.layoutPanelY + row * slotStep;
			}
		}

		this.playerInventoryTitleX = playerGridX;
		this.playerInventoryTitleY = this.layoutPanelY - 20;
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

		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;
		int labelY = y + this.layoutPanelY - 20;

		// Заголовок инвентаря игрока
		context.drawText(this.textRenderer, this.playerInventoryLabel,
				x + this.renderPlayerGridX, labelY, 0xFFFFFF, true);

		// Деньги игрока
		int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
		var playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
		playerCurrency.setTotalCopper(playerMoney);
		String playerMoneyText = playerCurrency.formatMoneyVertical();
		context.drawText(this.textRenderer, playerMoneyText,
				x + this.renderPlayerGridX, labelY + 10, 0xFFFFFF, true);

		// === Инвентарь жителя ===
		int villagerX = x + this.renderVillagerX;
		float expectedXp = getExpectedXpForCurrentHover();

		// 1. Полоска прогресса
		int level = handler.getProfessionLevel();
		int xpForNext = handler.getXpForNextLevel();
		int experience = handler.getProfessionExperience();
		float fractionalXp = handler.getClientFractionalXp();
		float totalXp = experience + fractionalXp;
		float progress = xpForNext > 0 ? Math.min(1.0f, totalXp / xpForNext) : 1.0f;

		if (progress < 1.0f && xpForNext > 0) {
			int barWidth = VillagerTradeScreenHandler.GRID_COLS * 19;
			int barHeight = 4;
			int barX = villagerX;
			int barY = labelY - 7;

			context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF303030);
			int filledWidth = (int) (barWidth * progress);
			context.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF40FF40);

			if (expectedXp > 0) {
				float progressStart = progress;
				float progressGain = Math.min(1.0f - progressStart, expectedXp / xpForNext);
				int blueStartX = barX + filledWidth;
				int blueWidth = (int) (barWidth * progressGain);
				context.fill(blueStartX, barY, blueStartX + blueWidth, barY + barHeight, 0xFF00FFFF);
			}

			String expectedXpText = expectedXp > 0 ? String.format(" +%.2f", expectedXp) : "";
			Text xpText = Text.literal(String.format("%.2f", totalXp) + expectedXpText + " / " + xpForNext + " XP");
			context.drawText(this.textRenderer, xpText, barX, barY - 9, 0x808080, true);
		} else if (level >= 5) {
			// Максимальный уровень — исправлено: используем append, а не конкатенацию
			Text maxLevelText = Text.translatable("tradeoverhaul.gui.maxlevel")
					.formatted(Formatting.GOLD, Formatting.BOLD);
			context.drawText(this.textRenderer, maxLevelText, villagerX, labelY - 7, 0xFFAA00, true);
		}

		// 2. Уровень жителя — ИСПРАВЛЕНО: правильная сборка через append()
		String levelName = handler.getProfessionLevelName();
		Text levelTitle = switch (levelName) {
			case "apprentice" -> Text.translatable("tradeoverhaul.level.apprentice");
			case "journeyman" -> Text.translatable("tradeoverhaul.level.journeyman");
			case "expert" -> Text.translatable("tradeoverhaul.level.expert");
			case "master" -> Text.translatable("tradeoverhaul.level.master");
			default -> Text.translatable("tradeoverhaul.level.novice");
		};

		// Профессия в скобках (переводится через ванильный ключ)
		Text professionText = Text.empty();
		if (this.handler.getVillager() != null) {
			var villager = this.handler.getVillager();
			var profession = villager.getVillagerData().getProfession();
			if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE) {
				var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
				if (profId != null) {
					professionText = Text.literal(" (")
							.append(Text.translatable("entity.minecraft.villager." + profId.getPath()))
							.append(")");
				}
			}
		}

		// Собираем итоговый текст: "Мастер (Кузнец) (ур. 5)"
		Text levelText = Text.empty()
				.append(levelTitle)
				.append(professionText)
				.append(" (")
				.append(Text.translatable("tradeoverhaul.gui.level"))
				.append(" ")
				.append(String.valueOf(level))
				.append(")");

		context.drawText(this.textRenderer, levelText, villagerX, labelY, 0xFFFFFF, true);

		// 3. Деньги жителя
		int wallet = handler.getSyncedWallet();
		var villagerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
		villagerCurrency.setTotalCopper(wallet);
		String villagerMoneyText = villagerCurrency.formatMoneyVertical();
		context.drawText(this.textRenderer, villagerMoneyText, villagerX, labelY + 10, 0xFFFFFF, true);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
		if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
			ItemStack stack = this.focusedSlot.getStack();
			int slotIndex = this.focusedSlot.id;
			boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
			boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();

			List<Text> tooltip = this.getTooltipFromItem(this.client, stack);

			if (slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				int price = handler.getClientBuyPrice(slotIndex);
				if (price > 0) {
					int pricePerItem = price;
					int wantToBuy = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);

					int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
					int maxCanBuy = playerMoney / pricePerItem;
					int quantity = Math.min(wantToBuy, maxCanBuy);

					boolean hasInventorySpace = handler.clientHasInventorySpaceForStack(stack, quantity);
					int maxSpace = hasInventorySpace ? wantToBuy : 0;
					if (maxSpace < quantity) quantity = maxSpace;

					boolean canFitAtLeastOne = handler.clientHasInventorySpaceForStack(stack, 1);
					boolean canAffordOne = playerMoney >= pricePerItem;

					int displayQuantity, totalCost;
					boolean canAfford;
					String quantityText;

					if (!canFitAtLeastOne) {
						tooltip.add(Text.translatable("gui.tradeoverhaul.no_inventory_space").formatted(Formatting.RED, Formatting.ITALIC));
						displayQuantity = 1;
						quantityText = " (1)";
						totalCost = pricePerItem;
						canAfford = false;
					} else if (quantity <= 0) {
						displayQuantity = 1;
						quantityText = " (1)";
						totalCost = pricePerItem;
						canAfford = false;
					} else {
						displayQuantity = quantity;
						quantityText = " (" + displayQuantity + ")";
						totalCost = quantity * pricePerItem;
						canAfford = canAffordOne;
					}

					Formatting textColor = canAfford ? Formatting.GREEN : Formatting.RED;

					if (stack.isDamageable() && stack.getDamage() > 0) {
						int maxDamage = stack.getMaxDamage();
						int currentDamage = stack.getDamage();
						int durabilityPercent = (int) Math.round(((double) (maxDamage - currentDamage) / maxDamage) * 100.0);
						tooltip.add(Text.translatable("gui.tradeoverhaul.durability", durabilityPercent).formatted(Formatting.GRAY));
					}

					if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
						var nbt = stack.getNbt();
						if (nbt != null && nbt.contains("StoredEnchantments", 10)) {
							var enchantList = nbt.getList("StoredEnchantments", 10);
							if (!enchantList.isEmpty()) {
								var enchantNbt = enchantList.getCompound(0);
								String enchantId = enchantNbt.getString("id");
								int enchantLevel = enchantNbt.getShort("lvl");
								Identifier enchantIdentifier = Identifier.tryParse(enchantId);
								if (enchantIdentifier != null) {
									var enchant = Registries.ENCHANTMENT.get(enchantIdentifier);
									if (enchant != null) {
										String enchantName = enchant.getName(enchantLevel).getString();
										tooltip.add(Text.literal("§7" + enchantName));
									}
								}
							}
						}
					}

					tooltip.add(Text.translatable("gui.tradeoverhaul.buy", quantityText, NumismaticHelper.formatMoney(totalCost)).formatted(textColor));
				}
			} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX && slotIndex < VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
				int price = handler.getClientSellPrice(stack);
				if (price > 0) {
					int pricePerItem = price;
					int wantToSell = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);

					int villagerMoney = handler.getSyncedWallet();
					int maxCanBuy = villagerMoney / pricePerItem;
					int quantity = Math.min(wantToSell, maxCanBuy);

					boolean canAffordOne = villagerMoney >= pricePerItem;
					int displayQuantity = quantity <= 0 ? 1 : quantity;
					String quantityText = " (" + displayQuantity + ")";
					int totalEarned = displayQuantity * pricePerItem;
					Formatting textColor = canAffordOne ? Formatting.GREEN : Formatting.RED;

					if (stack.isDamageable() && stack.getDamage() > 0) {
						int maxDamage = stack.getMaxDamage();
						int currentDamage = stack.getDamage();
						int durabilityPercent = (int) Math.round(((double) (maxDamage - currentDamage) / maxDamage) * 100.0);
						tooltip.add(Text.translatable("gui.tradeoverhaul.durability", durabilityPercent).formatted(Formatting.GRAY));
					}

					if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
						var nbt = stack.getNbt();
						if (nbt != null && nbt.contains("StoredEnchantments", 10)) {
							var enchantList = nbt.getList("StoredEnchantments", 10);
							if (!enchantList.isEmpty()) {
								var enchantNbt = enchantList.getCompound(0);
								String enchantId = enchantNbt.getString("id");
								int enchantLevel = enchantNbt.getShort("lvl");
								Identifier enchantIdentifier = Identifier.tryParse(enchantId);
								if (enchantIdentifier != null) {
									var enchant = Registries.ENCHANTMENT.get(enchantIdentifier);
									if (enchant != null) {
										String enchantName = enchant.getName(enchantLevel).getString();
										tooltip.add(Text.literal("§7" + enchantName));
									}
								}
							}
						}
					}

					tooltip.add(Text.translatable("gui.tradeoverhaul.sell", quantityText, NumismaticHelper.formatMoney(totalEarned)).formatted(textColor));
				}
			} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX && slotIndex < VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
				int price = handler.getClientSellPrice(stack);
				if (price > 0) {
					int pricePerItem = price;
					int wantToSell = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
					int villagerMoney = handler.getSyncedWallet();
					int maxCanBuy = villagerMoney / pricePerItem;
					int quantity = Math.min(wantToSell, maxCanBuy);

					boolean canAffordOne = villagerMoney >= pricePerItem;
					int displayQuantity = quantity <= 0 ? 1 : quantity;
					String quantityText = " (" + displayQuantity + ")";
					int totalEarned = displayQuantity * pricePerItem;
					Formatting textColor = canAffordOne ? Formatting.GREEN : Formatting.RED;

					if (stack.isDamageable() && stack.getDamage() > 0) {
						int maxDamage = stack.getMaxDamage();
						int currentDamage = stack.getDamage();
						int durabilityPercent = (int) Math.round(((double) (maxDamage - currentDamage) / maxDamage) * 100.0);
						tooltip.add(Text.translatable("gui.tradeoverhaul.durability", durabilityPercent).formatted(Formatting.GRAY));
					}

					tooltip.add(Text.translatable("gui.tradeoverhaul.sell", quantityText, NumismaticHelper.formatMoney(totalEarned)).formatted(textColor));
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

		context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight, BG_COLOR_TOP, BG_COLOR_BOTTOM);
		context.fill(x, y, x + this.backgroundWidth, y + 2, BORDER_COLOR);
		context.fill(x, y + this.backgroundHeight - 2, x + this.backgroundWidth, y + this.backgroundHeight, BORDER_COLOR);
		context.fill(x, y, x + 2, y + this.backgroundHeight, BORDER_COLOR);
		context.fill(x + this.backgroundWidth - 2, y, x + this.backgroundWidth, y + this.backgroundHeight, BORDER_COLOR);
		context.fill(x + 2, y + 2, x + 3, y + this.backgroundHeight - 2, 0xFFA0A0A0);
		context.fill(x + 3, y + 2, x + this.backgroundWidth - 3, y + 4, 0xFFA0A0A0);
		context.fill(x + this.backgroundWidth - 3, y + 3, x + this.backgroundWidth - 2, y + this.backgroundHeight - 2, 0xFF404040);
		context.fill(x + 3, y + this.backgroundHeight - 3, x + this.backgroundWidth - 3, y + this.backgroundHeight - 2, 0xFF404040);

		drawSlotBackgrounds(context, x, y);
		drawBuyableItemHighlights(context, x, y);
	}

	private void drawSlotBackgrounds(DrawContext context, int x, int y) {
		int slotSize = 19;
		int slotStep = 19;
		int armorStep = 20;

		int armorX = x + this.renderArmorX;
		for (int i = 0; i < VillagerTradeScreenHandler.ARMOR_SLOT_COUNT; i++) {
			int slotX = armorX;
			int slotY = y + this.layoutPanelY + i * armorStep - 1;
			context.fill(slotX, slotY, slotX + slotSize - 1, slotY + slotSize - 1, SLOT_BG_COLOR);
			context.fill(slotX + 1, slotY + 1, slotX + slotSize - 2, slotY + slotSize - 2, 0xFF303030);
		}

		int playerGridX = x + this.renderPlayerGridX;
		for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
			for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
				int slotX = playerGridX + col * slotStep;
				int slotY = y + this.layoutPanelY + row * slotStep - 1;
				context.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, SLOT_BG_COLOR);
				context.fill(slotX + 1, slotY + 1, slotX + slotSize - 2, slotY + slotSize - 2, 0xFF303030);
			}
		}

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
				if (buying && slot.getStack().isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
				if (!buying && slot.getStack().isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

				if (buying) {
					ItemStack stack = slot.getStack();
					if (!stack.isEmpty() && !handler.clientHasInventorySpaceForStack(stack, 1)) {
						return true;
					}
				}

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
		// Не рисуем стандартный заголовок
	}

	private static final int SLOT_BORDER_CAN_AFFORD = 0xFF00FFFF;
	private static final int SLOT_BORDER_CANNOT_AFFORD = 0xFFFF5555;

	private void drawBuyableItemHighlights(DrawContext context, int x, int y) {
		int slotSize = 19;
		int slotStep = 19;
		int armorStep = 20;
		int villagerMoney = handler.getSyncedWallet();

		for (int row = 0; row < VillagerTradeScreenHandler.GRID_ROWS; row++) {
			for (int col = 0; col < VillagerTradeScreenHandler.GRID_COLS; col++) {
				int gridIndex = row * VillagerTradeScreenHandler.GRID_COLS + col;
				int slotIndex = VillagerTradeScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX + gridIndex;
				Slot slot = this.handler.getSlot(slotIndex);
				if (slot != null && slot.hasStack()) {
					ItemStack stack = slot.getStack();
					if (handler.canVillagerBuyItem(stack)) {
						int sellPrice = handler.getClientSellPrice(stack);
						int pricePerItem = sellPrice <= 0 ? 1 : sellPrice;
						int borderColor = villagerMoney >= pricePerItem ? SLOT_BORDER_CAN_AFFORD : SLOT_BORDER_CANNOT_AFFORD;

						int slotX = x + this.renderPlayerGridX + col * slotStep;
						int slotY = y + this.layoutPanelY + row * slotStep - 1;

						context.fill(slotX, slotY, slotX + slotSize - 1, slotY + 1, borderColor);
						context.fill(slotX + 1, slotY + slotSize - 2, slotX + slotSize - 1, slotY + slotSize - 1, borderColor);
						context.fill(slotX, slotY + 1, slotX + 1, slotY + slotSize - 1, borderColor);
						context.fill(slotX + slotSize - 2, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, borderColor);
					}
				}
			}
		}

		for (int i = 0; i < VillagerTradeScreenHandler.ARMOR_SLOT_COUNT; i++) {
			int slotIndex = VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX + i;
			Slot slot = this.handler.getSlot(slotIndex);
			if (slot != null && slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (handler.canVillagerBuyItem(stack)) {
					int sellPrice = handler.getClientSellPrice(stack);
					int pricePerItem = sellPrice <= 0 ? 1 : sellPrice;
					int borderColor = villagerMoney >= pricePerItem ? SLOT_BORDER_CAN_AFFORD : SLOT_BORDER_CANNOT_AFFORD;

					int slotX = x + this.renderArmorX;
					int slotY = y + this.layoutPanelY + i * armorStep - 1;

					context.fill(slotX, slotY, slotX + slotSize - 1, slotY + 1, borderColor);
					context.fill(slotX + 1, slotY + slotSize - 2, slotX + slotSize - 1, slotY + slotSize - 1, borderColor);
					context.fill(slotX, slotY + 1, slotX + 1, slotY + slotSize - 1, borderColor);
					context.fill(slotX + slotSize - 2, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, borderColor);
				}
			}
		}
	}

	private float getExpectedXpForCurrentHover() {
		if (this.focusedSlot == null || !this.focusedSlot.hasStack()) return 0f;

		int slotIndex = this.focusedSlot.id;
		ItemStack stack = this.focusedSlot.getStack();
		boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
		boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();

		if (slotIndex >= VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
			int price = handler.getClientBuyPrice(slotIndex);
			if (price <= 0) return 0f;

			int pricePerItem = price;
			int wantToBuy = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
			int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
			int maxCanBuy = playerMoney / pricePerItem;
			int quantity = Math.min(wantToBuy, maxCanBuy);

			int maxSpace = handler.clientHasInventorySpaceForStack(stack, wantToBuy) ? wantToBuy : 0;
			if (maxSpace < quantity) quantity = maxSpace;
			if (quantity <= 0) return 0f;

			return handler.getExpectedXpForBuy(slotIndex, quantity);
		} else if (slotIndex >= VillagerTradeScreenHandler.FIRST_ARMOR_SLOT_INDEX && slotIndex < VillagerTradeScreenHandler.FIRST_VILLAGER_SLOT_INDEX) {
			int wantToSell = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
			int villagerMoney = handler.getSyncedWallet();
			int price = handler.getClientSellPrice(stack);
			if (price <= 0) return 0f;

			int maxCanBuy = villagerMoney / price;
			int quantity = Math.min(wantToSell, maxCanBuy);
			if (quantity <= 0) quantity = 1;

			return handler.getExpectedXpForSell(stack, quantity);
		}
		return 0f;
	}
}