package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.dto.DamagedItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RepairPanel extends ClickableWidget {

    private List<DamagedItem> damagedItems;
    private final List<RepairSlotButton> buttons = new ArrayList<>();
    private final Runnable onSelectionChanged;
    private DamagedItem selectedItem;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_STEP = 19;
    private static final int SLOTS_PER_ROW = 6;
    private static final int ROWS = 6;
    private boolean visible = true;
    private int panelWidth;
    private int panelHeight;

    public RepairPanel(int x, int y, int width, int height, List<DamagedItem> damagedItems, Runnable onSelectionChanged) {
        super(x, y, width, height, Text.literal(""));
        this.damagedItems = damagedItems;
        this.panelWidth = width;
        this.panelHeight = height;
        this.onSelectionChanged = onSelectionChanged;
        createButtons();
        updateMaxScroll();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void updateItems(List<DamagedItem> newItems) {
        this.damagedItems = newItems;
        buttons.clear();
        createButtons();
        updateMaxScroll();
        // Сбрасываем выбранный предмет, если он был удалён
        if (selectedItem != null && !damagedItems.contains(selectedItem)) {
            selectedItem = null;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        }
    }

    private void createButtons() {
        buttons.clear();
        for (int i = 0; i < damagedItems.size(); i++) {
            final int index = i;
            DamagedItem item = damagedItems.get(i);
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            int xPos = getX() + 5 + col * SLOT_STEP;
            int yPos = getY() + 25 + row * SLOT_STEP - scrollOffset;
            RepairSlotButton button = new RepairSlotButton(xPos, yPos, SLOT_SIZE, SLOT_SIZE, item, index);
            button.setPressAction(() -> selectItem(index));
            buttons.add(button);
        }
    }

    private void updateMaxScroll() {
        int totalRows = (int) Math.ceil((double) damagedItems.size() / SLOTS_PER_ROW);
        int totalHeight = totalRows * SLOT_STEP;
        int visibleHeight = panelHeight - 30;
        maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void selectItem(int index) {
        if (index >= 0 && index < damagedItems.size()) {
            selectedItem = damagedItems.get(index);
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        }
    }

    public void scroll(int amount) {
        int newOffset = scrollOffset - amount * SLOT_STEP;
        scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
        updateButtonPositions();
    }

    private void updateButtonPositions() {
        for (int i = 0; i < buttons.size(); i++) {
            RepairSlotButton button = buttons.get(i);
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            int xPos = getX() + 5 + col * SLOT_STEP;
            int yPos = getY() + 25 + row * SLOT_STEP - scrollOffset;
            button.setPosition(xPos, yPos);
            button.visible = yPos + SLOT_SIZE > getY() && yPos < getY() + panelHeight;
        }
    }

    public DamagedItem getSelectedItem() {
        return selectedItem;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        context.enableScissor(getX(), getY(), getX() + panelWidth, getY() + panelHeight);

        updateButtonPositions();

        for (RepairSlotButton button : buttons) {
            button.render(context, mouseX, mouseY, delta);
        }

        context.disableScissor();
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Damaged Items"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        for (RepairSlotButton btn : buttons) {
            if (btn.visible && btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;
        if (mouseX >= getX() && mouseX <= getX() + panelWidth && mouseY >= getY() && mouseY <= getY() + panelHeight) {
            scroll((int)amount);
            return true;
        }
        return false;
    }

    private class RepairSlotButton {
        private int x, y;
        private final int width, height;
        private final DamagedItem item;
        private final int index;
        private Runnable pressAction;
        private boolean visible = true;

        public RepairSlotButton(int x, int y, int width, int height, DamagedItem item, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.item = item;
            this.index = index;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setPressAction(Runnable action) {
            this.pressAction = action;
        }

        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!visible) return;

            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            boolean isSelected = (item == selectedItem);

            // Фон слота
            int bgColor = 0x8B8B8B8B;
            if (isSelected) {
                bgColor = 0xCC555555;
            } else if (hovered) {
                bgColor = 0xCC444444;
            }
            context.fill(x, y, x + width, y + height, bgColor);
            context.fill(x + 1, y + 1, x + width - 2, y + height - 2, 0xFF303030);

            // Рамка для выбранного
            if (isSelected) {
                context.drawBorder(x - 1, y - 1, width + 2, height + 2, 0xFFD4AF37);
            }

            // Иконка предмета
            ItemStack stack = item.getStack();
            context.drawItem(stack, x + 1, y + 1);
            context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x + 1, y + 1);

            // Красный фон для повреждения (визуальный индикатор)
            int damagePercent = (int) ((float) item.getCurrentDamage() / item.getMaxDamage() * 100);
            if (damagePercent > 50) {
                int redIntensity = Math.min(200, 100 + damagePercent);
                context.fill(x + 1, y + 1, x + 2, y + height - 2, 0xFF000000 | (redIntensity << 16));
                context.fill(x + 1, y + 1, x + width - 2, y + 2, 0xFF000000 | (redIntensity << 16));
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (visible && button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                if (pressAction != null) {
                    pressAction.run();
                }
                return true;
            }
            return false;
        }
    }
}