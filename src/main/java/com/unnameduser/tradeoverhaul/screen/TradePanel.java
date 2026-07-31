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
    // Текстуры
    private static final Identifier PANEL_BG = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/trade_panel_background.png");
    private static final Identifier SLIDER_BG = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/slider_background.png");
    private static final Identifier SLIDER_HANDLE = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/slider_handle.png");
    private static final Identifier BUTTON_TRADE = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/trade_button.png");

    // Размеры панели
    private static final int PANEL_WIDTH = 130;
    private static final int PANEL_HEIGHT = 190;
    private static final int SLIDER_WIDTH = 90;
    private static final int SLIDER_HEIGHT = 9;
    private static final int HANDLE_SIZE = 11;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ITEM_SIZE = 32;
    private static final int SLIDER_PADDING = 7; // Отступ от краёв ползунка

    private final TextRenderer textRenderer;
    private final TradePanelCallback callback;
    private final MinecraftClient client;

    // Состояние панели
    private boolean visible = false;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private int slotIndex = -1;
    private int pricePerUnit = 0;
    private int maxAmount = 0;
    private int currentAmount = 1;
    private boolean isBuying = true;
    private boolean canAfford = false;
    private int totalPrice = 0;

    // Позиции элементов
    private int panelX, panelY;
    private int sliderX, sliderY;
    private int handleX, handleY;
    private int buttonX, buttonY;
    private int itemX, itemY;

    // Состояние ползунка
    private boolean isDragging = false;
    private int dragOffsetX = 0; // Смещение клика относительно центра маркера

    public TradePanel(TextRenderer textRenderer, TradePanelCallback callback) {
        this.textRenderer = textRenderer;
        this.callback = callback;
        this.client = MinecraftClient.getInstance();
    }

    /**
     * Открыть панель с выбранным предметом
     */
    // В TradePanel.java
    public void open(ItemStack stack, int slotIndex, int price, int maxAmount, boolean buying) {
        this.selectedItem = stack.copy();
        this.slotIndex = slotIndex;
        this.pricePerUnit = price;
        this.maxAmount = Math.max(1, maxAmount);
        this.isBuying = buying;
        this.visible = true;
        this.isDragging = false;

        // ✅ Получаем деньги в зависимости от типа сделки
        int money;
        if (isBuying) {
            // Покупка — проверяем деньги игрока
            money = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(client.player);
        } else {
            // Продажа — проверяем деньги жителя через callback
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

    /**
     * Закрыть панель
     */
    public void close() {
        this.visible = false;
        this.selectedItem = ItemStack.EMPTY;
        this.slotIndex = -1;
        this.isDragging = false;
        callback.onTradeCancel();
    }

    /**
     * Установить позицию панели (центрируется относительно переданных координат)
     */
    public void setPosition(int centerX, int centerY) {
        this.panelX = centerX - PANEL_WIDTH / 2;
        this.panelY = centerY - PANEL_HEIGHT / 2;
        calculateElementPositions();
    }

    /**
     * Пересчитать позиции элементов
     */
    private void calculateElementPositions() {
        // Позиция увеличенного предмета
        itemX = panelX + PANEL_WIDTH / 2 - ITEM_SIZE / 2;
        itemY = panelY + 30;

        // Ползунок
        sliderX = panelX + (PANEL_WIDTH - SLIDER_WIDTH) / 2;
        sliderY = panelY + 100;

        // Маркер ползунка
        updateHandlePosition();

        // Кнопка
        buttonX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        buttonY = panelY + PANEL_HEIGHT - 35;
    }

    /**
     * Обновить позицию маркера ползунка
     */
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

    /**
     * Обновить количество предметов по позиции маркера
     */
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

    /**
     * Проверка, активна ли кнопка
     */
    private boolean isEnabled() {
        return canAfford && currentAmount > 0;
    }

    /**
     * Отрисовка панели
     */
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible || selectedItem.isEmpty()) return;

        // 1. Фон панели
        context.drawTexture(PANEL_BG, panelX, panelY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        // 2. Название предмета
        Text name = selectedItem.getName();
        int nameWidth = textRenderer.getWidth(name);
        context.drawText(textRenderer, name, panelX + PANEL_WIDTH / 2 - nameWidth / 2, panelY + 8, 0xFFFFFF, false);

        // 3. Увеличенный предмет
        context.getMatrices().push();
        context.getMatrices().translate(itemX + ITEM_SIZE / 2, itemY + ITEM_SIZE / 2, 0);
        context.getMatrices().scale(2.2f, 2.2f, 1.0f);
        context.drawItem(selectedItem, -8, -8);
        context.getMatrices().pop();

        // 4. Текст "Amount: X" над ползунком
        String amountStr = canAfford ? "Amount: " + currentAmount : "Amount: 0";
        int amountWidth = textRenderer.getWidth(amountStr);
        context.drawText(textRenderer, Text.literal(amountStr), panelX + PANEL_WIDTH / 2 - amountWidth / 2, sliderY - 14, canAfford ? 0xFFFFFF : 0xFF4444, false);

        // 5. Фон ползунка
        context.drawTexture(SLIDER_BG, sliderX, sliderY, 0, 0, SLIDER_WIDTH, SLIDER_HEIGHT, SLIDER_WIDTH, SLIDER_HEIGHT);

        // 6. Маркер ползунка (только если можно двигать)
        if (canAfford && maxAmount > 1) {
            context.drawTexture(SLIDER_HANDLE, handleX, handleY, 0, 0, HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE, HANDLE_SIZE);
        }

        // 7. Цена
        String priceStr;
        int priceColor;
        boolean isExpensive = false; // Флаг для уменьшения шрифта

        if (canAfford) {
            int price = totalPrice;
            int gold = Math.floorDiv(price, 10000);
            int remainder = price % 10000;

            int silver = Math.floorDiv(remainder, 100);
            int copper = remainder % 100;

            List<String> parts = new ArrayList<>();
            if (gold > 0) parts.add(String.format("%d gold", gold));
            if (silver > 0) parts.add(String.format("%d silver", silver));
            if (copper > 0) parts.add(String.format("%d copper", copper));

            // Если цена 0, показываем хотя бы медь
            if (parts.isEmpty()) parts.add("0c");

            String totalPriceString = String.join(", ", parts);
            priceStr = "Price: " + totalPriceString;
            priceColor = 0xFFFFAA;

            if ((silver > 0 && copper > 0) || (silver > 0 && gold > 0) || (gold > 0 && copper > 0)) {
                isExpensive = true;
            }

        } else {
            priceStr = "Price: " + pricePerUnit + "c";
            priceColor = 0xFF4444;
        }

// --- ОТРИСОВКА ---
        int textY = buttonY - 16;
        float scale = 1.0f;

// Если текст дорогой, уменьшаем масштаб
        if (isExpensive) {
            scale = 0.85f; // Уменьшаем до 85%
        }

        int priceWidth = (int) (textRenderer.getWidth(priceStr) * scale);
        int centerX = panelX + PANEL_WIDTH / 2;
        int finalX = centerX - priceWidth / 2;

// Сохраняем состояние матрицы
        context.getMatrices().push();
// Перемещаем точку отсчета, масштабируем и возвращаем обратно
        context.getMatrices().translate(finalX, textY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

// Рисуем текст в точке (0, 0) относительно текущего смещения матрицы
        context.drawText(textRenderer, Text.literal(priceStr), 0, 0, priceColor, false);

// Восстанавливаем матрицу, чтобы не сломать остальной интерфейс
        context.getMatrices().pop();
        // 8. Кнопка (3 состояния)
        boolean hovered = isMouseOverButton(mouseX, mouseY);
        boolean enabled = isEnabled();

        int state = 0;
        if (!enabled) {
            state = 2; // Неактивна
        } else if (hovered) {
            state = 1; // Наведение
        }

        int vOffset = state * BUTTON_HEIGHT;
        context.drawTexture(BUTTON_TRADE, buttonX, buttonY, 0, vOffset, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT * 3);

        // Текст на кнопке
        String btnText = isBuying ? "BUY" : "SELL";
        int btnTextWidth = textRenderer.getWidth(btnText);
        context.drawText(textRenderer, Text.literal(btnText), buttonX + BUTTON_WIDTH / 2 - btnTextWidth / 2, buttonY + BUTTON_HEIGHT / 2 - 4, enabled ? 0xFFFFFF : 0x666666, false);
    }

    /**
     * Обработка кликов мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        // Проверка клика по маркеру ползунка
        if (canAfford && maxAmount > 1 && isMouseOverHandle(mouseX, mouseY)) {
            isDragging = true;
            // Запоминаем смещение клика относительно центра маркера
            dragOffsetX = (int) (mouseX - (handleX + HANDLE_SIZE / 2));
            return true;
        }

        // Проверка клика по кнопке
        if (isEnabled() && isMouseOverButton(mouseX, mouseY)) {
            callback.onTradeConfirm(slotIndex, currentAmount, isBuying);
            close();
            return true;
        }

        return false;
    }

    /**
     * Обработка перетаскивания
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isDragging || !canAfford || maxAmount <= 1) return false;

        // Обновляем позицию маркера с учётом смещения клика
        int newHandleX = (int) mouseX - HANDLE_SIZE / 2 - dragOffsetX;
        int minX = sliderX + SLIDER_PADDING;
        int maxX = sliderX + SLIDER_WIDTH - HANDLE_SIZE - SLIDER_PADDING;
        newHandleX = Math.max(minX, Math.min(maxX, newHandleX));
        handleX = newHandleX;

        // Обновляем количество
        updateAmountFromHandle();
        return true;
    }

    /**
     * Обработка отпускания кнопки мыши
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            dragOffsetX = 0;
            return true;
        }
        return false;
    }

    /**
     * Проверка попадания на маркер ползунка
     */
    private boolean isMouseOverHandle(double mouseX, double mouseY) {
        return mouseX >= handleX && mouseX <= handleX + HANDLE_SIZE &&
                mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE;
    }

    /**
     * Проверка попадания на кнопку
     */
    private boolean isMouseOverButton(double mouseX, double mouseY) {
        return mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
    }

    // Геттеры
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