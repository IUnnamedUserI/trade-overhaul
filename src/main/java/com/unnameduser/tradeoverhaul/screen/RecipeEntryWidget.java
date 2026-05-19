package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class RecipeEntryWidget extends ClickableWidget {

    private final CraftRecipe recipe;
    private final Runnable onPress;
    private boolean selected = false;

    private static final int ITEM_ICON_SIZE = 16;
    private static final int PADDING = 4;
    private static final int TEXT_OFFSET = ITEM_ICON_SIZE + PADDING * 2;

    public RecipeEntryWidget(int x, int y, int width, int height, CraftRecipe recipe, Runnable onPress) {
        super(x, y, width, height, Text.literal(""));
        this.recipe = recipe;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон
        int bgColor = 0xFF333333;
        if (selected) {
            bgColor = 0xFF555555;
        } else if (isHovered()) {
            bgColor = 0xFF444444;
        }
        context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        // Рамка
        context.drawBorder(getX(), getY(), width, height, 0xFF666666);

        // Иконка предмета результата
        ItemStack resultStack = recipe.getResult().copy();
        int iconX = getX() + PADDING;
        int iconY = getY() + (height - ITEM_ICON_SIZE) / 2;
        context.drawItem(resultStack, iconX, iconY);
        context.drawItemInSlot(MinecraftClient.getInstance().textRenderer, resultStack, iconX, iconY);

        // Название предмета
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String name = resultStack.getName().getString();
        int maxTextWidth = width - TEXT_OFFSET - PADDING;
        if (textRenderer.getWidth(name) > maxTextWidth) {
            name = textRenderer.trimToWidth(name, maxTextWidth - 6) + "...";
        }

        int textX = getX() + TEXT_OFFSET;
        int textY = getY() + (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, name, textX, textY, 0xFFFFFF, false);

        // Отображаем уровень требования
        int level = recipe.getRequiredLevel();
        if (level > 1) {
            String levelText = "Lv." + level;
            int levelX = getX() + width - textRenderer.getWidth(levelText) - PADDING;
            context.drawText(textRenderer, levelText, levelX, textY, 0xAAAAAA, false);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal(recipe.getResult().getName().getString()));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) {
            onPress.run();
        }
    }

    public CraftRecipe getRecipe() {
        return recipe;
    }
}