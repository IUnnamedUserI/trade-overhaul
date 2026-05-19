package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class RecipeScrollListWidget extends EntryListWidget<RecipeScrollListWidget.RecipeEntry> {

    private final List<CraftRecipe> recipes;
    private final Runnable onSelectionChanged;
    private int selectedIndex = -1;
    private boolean visible = true;

    public RecipeScrollListWidget(MinecraftClient client, int x, int y, int width, int height, int itemHeight, List<CraftRecipe> recipes, Runnable onSelectionChanged) {
        super(client, width, height, y, y + height, itemHeight);
        this.setLeftPos(x);
        this.recipes = recipes;
        this.onSelectionChanged = onSelectionChanged;

        for (int i = 0; i < recipes.size(); i++) {
            this.addEntry(new RecipeEntry(recipes.get(i), i));
        }

        System.out.println("[TradeOverhaul] RecipeScrollList created with " + recipes.size() + " recipes");
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (visible) {
            super.render(context, mouseX, mouseY, delta);
        }
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < recipes.size()) {
            selectedIndex = index;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        }
    }

    public CraftRecipe getSelectedRecipe() {
        if (selectedIndex >= 0 && selectedIndex < recipes.size()) {
            return recipes.get(selectedIndex);
        }
        return null;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Recipe List"));
    }

    public class RecipeEntry extends Entry<RecipeEntry> {
        private final CraftRecipe recipe;
        private final int index;

        public RecipeEntry(CraftRecipe recipe, int index) {
            this.recipe = recipe;
            this.index = index;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean isSelected = (this.index == selectedIndex);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            int bgColor = 0xFF333333;
            if (isSelected) {
                bgColor = 0xFF555555;
            } else if (hovered) {
                bgColor = 0xFF444444;
            }
            context.fill(x, y, x + entryWidth, y + entryHeight, bgColor);
            context.drawBorder(x, y, entryWidth, entryHeight, 0xFF666666);

            ItemStack result = recipe.getResult().copy();
            context.drawItem(result, x + 4, y + (entryHeight - 16) / 2);
            context.drawItemInSlot(textRenderer, result, x + 4, y + (entryHeight - 16) / 2);

            String name = result.getName().getString();
            int maxWidth = entryWidth - 30 - 30;
            if (textRenderer.getWidth(name) > maxWidth) {
                name = textRenderer.trimToWidth(name, maxWidth - 6) + "...";
            }
            context.drawText(textRenderer, name, x + 24, y + (entryHeight - textRenderer.fontHeight) / 2, 0xFFFFFF, false);

            int level = recipe.getRequiredLevel();
            if (level > 1) {
                String levelText = "Lv." + level;
                context.drawText(textRenderer, levelText, x + entryWidth - textRenderer.getWidth(levelText) - 8, y + (entryHeight - textRenderer.fontHeight) / 2, 0xAAAAAA, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectedIndex = this.index;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
            return true;
        }

        public CraftRecipe getRecipe() {
            return recipe;
        }
    }
}