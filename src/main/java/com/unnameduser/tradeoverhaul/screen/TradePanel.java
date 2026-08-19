package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TradePanel {
    private static final Identifier PANEL_BG = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/trade_panel_background.png");
    private static final Identifier SLIDER_BG = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/slider_background.png");
    private static final Identifier SLIDER_HANDLE = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/slider_handle.png");
    private static final Identifier BUTTON_TRADE = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/trade_button.png");
    private static final Identifier ITEM_BG_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_slot.png");

    private static final int PANEL_WIDTH = 130;
    private static final int PANEL_HEIGHT = 190;
    private static final int SLIDER_WIDTH = 90;
    private static final int SLIDER_HEIGHT = 9;
    private static final int HANDLE_SIZE = 11;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ITEM_BG_SIZE = 48;
    private static final int SLIDER_PADDING = 7;

    private final TextRenderer textRenderer;
    private final TradePanelCallback callback;
    private final MinecraftClient client;

    private boolean visible = false;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private int slotIndex = -1;
    private int pricePerUnit = 0;
    private int maxAmount = 0;
    private int currentAmount = 1;
    private boolean isBuying = true;
    private boolean canAfford = false;
    private int totalPrice = 0;

    private int panelX, panelY;
    private int sliderX, sliderY;
    private int handleX, handleY;
    private int buttonX, buttonY;
    private int itemBgX, itemBgY;

    private boolean isDragging = false;
    private int dragOffsetX = 0;

    public TradePanel(TextRenderer textRenderer, TradePanelCallback callback) {
        this.textRenderer = textRenderer;
        this.callback = callback;
        this.client = MinecraftClient.getInstance();
    }

    public void open(ItemStack stack, int slotIndex, int price, int maxAmount, boolean buying) {
        this.selectedItem = stack.copy();
        this.slotIndex = slotIndex;
        this.pricePerUnit = price;
        this.maxAmount = Math.max(1, maxAmount);
        this.isBuying = buying;
        this.visible = true;
        this.isDragging = false;

        int money;
        if (isBuying) {
            money = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(client.player);
        } else {
            money = callback.getVillagerMoney();
        }

        this.canAfford = money >= pricePerUnit;

        if (canAfford) {
            this.currentAmount = Math.min(1, maxAmount);
        } else {
            this.currentAmount = 0;
        }

        this.totalPrice = pricePerUnit * Math.max(1, currentAmount);

        calculateElementPositions();
        callback.onAmountChanged(currentAmount, totalPrice);
    }

    public void close() {
        this.visible = false;
        this.selectedItem = ItemStack.EMPTY;
        this.slotIndex = -1;
        this.isDragging = false;
        callback.onTradeCancel();
    }

    public void setPosition(int centerX, int centerY) {
        this.panelX = centerX - PANEL_WIDTH / 2;
        this.panelY = centerY - PANEL_HEIGHT / 2;
        calculateElementPositions();
    }

    private void calculateElementPositions() {
        itemBgX = panelX + PANEL_WIDTH / 2 - ITEM_BG_SIZE / 2;
        itemBgY = panelY + 22;

        sliderX = panelX + (PANEL_WIDTH - SLIDER_WIDTH) / 2;
        sliderY = panelY + 100;

        updateHandlePosition();

        buttonX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        buttonY = panelY + PANEL_HEIGHT - 35;
    }

    private void updateHandlePosition() {
        if (maxAmount <= 1) {
            handleX = sliderX + (SLIDER_WIDTH - HANDLE_SIZE) / 2;
            handleY = sliderY - (HANDLE_SIZE - SLIDER_HEIGHT) / 2;
            return;
        }

        if (!canAfford || currentAmount <= 0) {
            handleX = sliderX + SLIDER_PADDING;
            handleY = sliderY - (HANDLE_SIZE - SLIDER_HEIGHT) / 2;
            return;
        }

        int availableWidth = SLIDER_WIDTH - HANDLE_SIZE - (SLIDER_PADDING * 2);
        int progress = (currentAmount - 1) * availableWidth / (maxAmount - 1);
        handleX = sliderX + SLIDER_PADDING + progress;
        handleY = sliderY - (HANDLE_SIZE - SLIDER_HEIGHT) / 2;
    }

    private void updateAmountFromHandle() {
        if (maxAmount <= 1 || !canAfford) {
            currentAmount = canAfford ? 1 : 0;
            return;
        }

        int availableWidth = SLIDER_WIDTH - HANDLE_SIZE - (SLIDER_PADDING * 2);
        int relativeX = handleX - (sliderX + SLIDER_PADDING);
        float progress = (float) relativeX / availableWidth;
        progress = Math.max(0, Math.min(1, progress));

        currentAmount = 1 + (int) (progress * (maxAmount - 1));
        currentAmount = Math.max(1, Math.min(maxAmount, currentAmount));

        totalPrice = pricePerUnit * currentAmount;
        callback.onAmountChanged(currentAmount, totalPrice);
    }

    private boolean isEnabled() {
        return canAfford && currentAmount > 0;
    }

    /**
     * Форматирует цену в список строк.
     * Если все 3 номинала присутствуют: первая строка "Price: X gold, Y silver,", вторая "Z copper"
     * Иначе: одна строка "Price: ..."
     */
    private List<String> formatPriceLines(int price) {
        int gold = Math.floorDiv(price, 10000);
        int remainder = price % 10000;
        int silver = Math.floorDiv(remainder, 100);
        int copper = remainder % 100;

        List<String> parts = new ArrayList<>();
        if (gold > 0) parts.add(gold + " gold");
        if (silver > 0) parts.add(silver + " silver");
        if (copper > 0) parts.add(copper + " copper");
        if (parts.isEmpty()) parts.add("0c");

        List<String> lines = new ArrayList<>();

        // Если все 3 номинала — разбиваем на 2 строки
        if (gold > 0 && silver > 0 && copper > 0) {
            lines.add("Price: " + gold + " gold, " + silver + " silver,");
            lines.add(String.valueOf(copper) + " copper");
        } else {
            lines.add("Price: " + String.join(", ", parts));
        }

        return lines;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible || selectedItem.isEmpty()) return;

        // 1. Фон панели
        context.drawTexture(PANEL_BG, panelX, panelY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        // 2. Название предмета
        Text name = selectedItem.getName();
        int nameWidth = textRenderer.getWidth(name);
        context.drawText(textRenderer, name, panelX + PANEL_WIDTH / 2 - nameWidth / 2, panelY + 8, 0xFFFFFF, false);

        // 3. Рамка под предметом
        context.drawTexture(ITEM_BG_TEX, itemBgX, itemBgY, 0, 0, ITEM_BG_SIZE, ITEM_BG_SIZE, ITEM_BG_SIZE, ITEM_BG_SIZE);

        // 4. Увеличенный предмет поверх рамки
        context.getMatrices().push();
        context.getMatrices().translate(itemBgX + ITEM_BG_SIZE / 2f, itemBgY + ITEM_BG_SIZE / 2f, 0);
        context.getMatrices().scale(2.2f, 2.2f, 1.0f);
        context.drawItem(selectedItem, -8, -8);
        context.getMatrices().pop();

        // 5. Текст "Amount: X" над ползунком
        String amountStr = canAfford ? "Amount: " + currentAmount : "Amount: 0";
        int amountWidth = textRenderer.getWidth(amountStr);
        context.drawText(textRenderer, Text.literal(amountStr), panelX + PANEL_WIDTH / 2 - amountWidth / 2, sliderY - 14, canAfford ? 0xFFFFFF : 0xFF4444, false);

        // 6. Фон ползунка
        context.drawTexture(SLIDER_BG, sliderX, sliderY, 0, 0, SLIDER_WIDTH, SLIDER_HEIGHT, SLIDER_WIDTH, SLIDER_HEIGHT);

        // 7. Маркер ползунка
        if (canAfford && maxAmount > 1) {
            context.drawTexture(SLIDER_HANDLE, handleX, handleY, 0, 0, HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
        }

        // 8. Цена
        int displayPrice = canAfford ? totalPrice : pricePerUnit;
        List<String> priceLines = formatPriceLines(displayPrice);
        int priceColor = canAfford ? 0xFFFFAA : 0xFF4444;

        int lineHeight = textRenderer.fontHeight + 2;
        int totalTextHeight = priceLines.size() * lineHeight;
        int startY = buttonY - 6 - totalTextHeight;

        for (int i = 0; i < priceLines.size(); i++) {
            String line = priceLines.get(i);
            // Уменьшаем масштаб если 2+ номинала (2+ строки ИЛИ одна длинная строка с 2 номиналами)
            float scale = 1.0f;
            int nominalCount = 0;
            if (line.contains("gold")) nominalCount++;
            if (line.contains("silver")) nominalCount++;
            if (line.contains("copper")) nominalCount++;
            // Для второй строки (copper при 3 номиналах) считаем общее кол-во номиналов
            if (priceLines.size() > 1) nominalCount = 3;

            if (nominalCount >= 2 || priceLines.size() > 1) {
                scale = 0.85f;
            }

            int lineWidth = (int) (textRenderer.getWidth(line) * scale);
            int centerX = panelX + PANEL_WIDTH / 2;
            int finalX = centerX - lineWidth / 2;
            int lineY = startY + i * lineHeight;

            context.getMatrices().push();
            context.getMatrices().translate(finalX, lineY, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawText(textRenderer, Text.literal(line), 0, 0, priceColor, false);
            context.getMatrices().pop();
        }

        // 9. Кнопка
        boolean hovered = isMouseOverButton(mouseX, mouseY);
        boolean enabled = isEnabled();

        int state = 0;
        if (!enabled) {
            state = 2;
        } else if (hovered) {
            state = 1;
        }

        int vOffset = state * BUTTON_HEIGHT;
        context.drawTexture(BUTTON_TRADE, buttonX, buttonY, 0, vOffset, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 3);

        String btnText = isBuying ? "BUY" : "SELL";
        int btnTextWidth = textRenderer.getWidth(btnText);
        context.drawText(textRenderer, Text.literal(btnText), buttonX + BUTTON_WIDTH / 2 - btnTextWidth / 2, buttonY + BUTTON_HEIGHT / 2 - 4, enabled ? 0xFFFFFF : 0x666666, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        if (canAfford && maxAmount > 1 && isMouseOverHandle(mouseX, mouseY)) {
            isDragging = true;
            dragOffsetX = (int) (mouseX - (handleX + HANDLE_SIZE / 2));
            return true;
        }

        if (isEnabled() && isMouseOverButton(mouseX, mouseY)) {
            callback.onTradeConfirm(slotIndex, currentAmount, isBuying);
            close();
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isDragging || !canAfford || maxAmount <= 1) return false;

        int newHandleX = (int) mouseX - HANDLE_SIZE / 2 - dragOffsetX;
        int minX = sliderX + SLIDER_PADDING;
        int maxX = sliderX + SLIDER_WIDTH - HANDLE_SIZE - SLIDER_PADDING;
        newHandleX = Math.max(minX, Math.min(maxX, newHandleX));
        handleX = newHandleX;

        updateAmountFromHandle();
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            dragOffsetX = 0;
            return true;
        }
        return false;
    }

    private boolean isMouseOverHandle(double mouseX, double mouseY) {
        return mouseX >= handleX && mouseX <= handleX + HANDLE_SIZE &&
                mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE;
    }

    private boolean isMouseOverButton(double mouseX, double mouseY) {
        return mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
    }

    public boolean isVisible() { return visible; }
    public ItemStack getSelectedItem() { return selectedItem; }
    public int getCurrentAmount() { return currentAmount; }
    public int getTotalPrice() { return totalPrice; }
    public boolean canAfford() { return canAfford; }
    public boolean isBuying() { return isBuying; }
    public int getSlotIndex() { return slotIndex; }
    public int getX() { return panelX; }
    public int getY() { return panelY; }
    public int getWidth() { return PANEL_WIDTH; }
    public int getHeight() { return PANEL_HEIGHT; }
}