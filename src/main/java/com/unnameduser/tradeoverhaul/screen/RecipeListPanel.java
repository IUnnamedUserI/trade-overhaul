package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RecipeListPanel extends ClickableWidget {

    private List<CraftRecipe> recipes;
    private final List<RecipeButton> buttons = new ArrayList<>();
    private final Runnable onSelectionChanged;
    private final VillagerInteractionScreen parent;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_WIDTH = 130;
    private boolean visible = true;

    public RecipeListPanel(VillagerInteractionScreen parent, int x, int y, int width, int height, List<CraftRecipe> recipes, Runnable onSelectionChanged) {
        super(x, y, width, height, Text.literal(""));
        this.parent = parent;
        this.recipes = recipes;
        this.onSelectionChanged = onSelectionChanged;
        createButtons();
        updateMaxScroll();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void updateRecipes(List<CraftRecipe> newRecipes) {
        this.recipes = newRecipes;
        buttons.clear();
        createButtons();
        updateMaxScroll();
    }

    private void createButtons() {
        buttons.clear();
        for (int i = 0; i < recipes.size(); i++) {
            final int index = i;
            CraftRecipe recipe = recipes.get(i);
            RecipeButton button = new RecipeButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, recipe, index);
            button.setPressAction(() -> selectRecipe(index));
            buttons.add(button);
        }
    }

    private void updateMaxScroll() {
        int totalHeight = recipes.size() * (BUTTON_HEIGHT + BUTTON_GAP);
        maxScroll = Math.max(0, totalHeight - height + 2);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void selectRecipe(int index) {
        if (selectedIndex != index) {
            selectedIndex = index;
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        }
    }

    public void scroll(int amount) {
        int newOffset = scrollOffset - amount * (BUTTON_HEIGHT + BUTTON_GAP);
        scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
        updateButtonPositions();
    }

    private void updateButtonPositions() {
        int startY = getY() + 5;
        int step = BUTTON_HEIGHT + BUTTON_GAP;
        for (int i = 0; i < buttons.size(); i++) {
            RecipeButton button = buttons.get(i);
            int y = startY + i * step - scrollOffset;
            button.setPosition(getX() + 5, y);
            button.visible = y + BUTTON_HEIGHT > getY() && y < getY() + height;
        }
    }

    public CraftRecipe getSelectedRecipe() {
        if (selectedIndex >= 0 && selectedIndex < recipes.size()) {
            return recipes.get(selectedIndex);
        }
        return null;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        context.enableScissor(getX(), getY(), getX() + width, getY() + height + 2);

        updateButtonPositions();

        for (RecipeButton button : buttons) {
            button.render(context, mouseX, mouseY, delta);
        }

        context.disableScissor();
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Recipe List"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        for (RecipeButton btn : buttons) {
            if (btn.visible && btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;
        if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
            scroll((int)amount);
            return true;
        }
        return false;
    }

    private class RecipeButton {
        private int x, y;
        private final int width, height;
        private final CraftRecipe recipe;
        private final int index;
        private Runnable pressAction;
        private boolean visible = true;

        public RecipeButton(int x, int y, int width, int height, CraftRecipe recipe, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.recipe = recipe;
            this.index = index;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setPressAction(Runnable action) {
            this.pressAction = action;
        }

        private boolean isCraftable() {
            PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
            int uniqueIndex = recipe.getUniqueIngredientIndex();
            List<Ingredient> ingredients = recipe.getIngredients();

            // Проверяем уровень жителя
            if (parent != null && parent.getCraftingHandler() != null) {
                int villagerLevel = parent.getCraftingHandler().getVillagerLevel();
                if (recipe.getRequiredLevel() > villagerLevel) {
                    return false;
                }
            }

            // Проверяем уникальный ингредиент
            if (uniqueIndex >= 0 && uniqueIndex < ingredients.size()) {
                Ingredient uniqueIng = ingredients.get(uniqueIndex);
                boolean hasUnique = false;
                for (int slot = 0; slot < inv.size(); slot++) {
                    ItemStack stack = inv.getStack(slot);
                    if (stack.getItem() == uniqueIng.getItem().getItem() &&
                            stack.getCount() >= uniqueIng.getCount()) {
                        hasUnique = true;
                        break;
                    }
                }
                if (!hasUnique) return false;
            }

            // Проверяем все ингредиенты
            for (Ingredient ing : ingredients) {
                int needed = ing.getCount();
                int found = 0;
                for (int slot = 0; slot < inv.size(); slot++) {
                    ItemStack stack = inv.getStack(slot);
                    if (stack.getItem() == ing.getItem().getItem()) {
                        found += stack.getCount();
                        if (found >= needed) break;
                    }
                }
                if (found < needed) return false;
            }
            return true;
        }

        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!visible) return;

            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            boolean isSelected = (index == selectedIndex);
            boolean craftable = isCraftable();

            int bgColor;
            if (isSelected) {
                bgColor = 0xFF555555;
            } else if (hovered) {
                bgColor = 0xFF444444;
            } else {
                bgColor = craftable ? 0xFF333333 : 0xFF222222;
            }
            context.fill(x, y, x + width, y + height, bgColor);
            context.drawBorder(x, y, width, height, 0xFF666666);

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            ItemStack result = recipe.getResult().copy();
            context.drawItem(result, x + 4, y + (height - 16) / 2);
            context.drawItemInSlot(textRenderer, result, x + 4, y + (height - 16) / 2);

            String name = result.getName().getString();
            int maxWidth = width - 30 - 30;
            if (textRenderer.getWidth(name) > maxWidth) {
                name = textRenderer.trimToWidth(name, maxWidth - 6) + "...";
            }

            int textColor = craftable ? 0xFFFFFF : 0x888888;
            context.drawText(textRenderer, name, x + 24, y + (height - textRenderer.fontHeight) / 2, textColor, false);

            int level = recipe.getRequiredLevel();
            if (level > 1) {
                String levelText = "Lv." + level;
                int levelColor = craftable ? 0xAAAAAA : 0x666666;
                context.drawText(textRenderer, levelText, x + width - textRenderer.getWidth(levelText) - 8, y + (height - textRenderer.fontHeight) / 2, levelColor, false);
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