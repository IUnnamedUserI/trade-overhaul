package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
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
import net.minecraft.util.Identifier;

import java.util.List;

public class RecipeListPanel extends ClickableWidget {

    private static final Identifier LIST_ELEMENT_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/list_element.png");
    private static final int TEX_WIDTH = 117;
    private static final int TEX_STATE_HEIGHT = 24;

    private List<CraftRecipe> recipes;
    private final Runnable onSelectionChanged;
    private final VillagerInteractionScreen parent;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_GAP = 2;
    private boolean visible = true;

    private static final int COLOR_ITEM_NAME = 0x8E7D6E;
    private static final int COLOR_STATUS_READY = 0xFF55FF55;
    private static final int COLOR_STATUS_MISSING = 0xFFFF5555;
    private static final int COLOR_STATUS_MONEY = 0xFFFF5555;

    public RecipeListPanel(VillagerInteractionScreen parent, int x, int y, int width, int height, List<CraftRecipe> recipes, Runnable onSelectionChanged) {
        super(x, y, width, height, Text.literal(""));
        this.parent = parent;
        this.recipes = recipes;
        this.onSelectionChanged = onSelectionChanged;
        updateMaxScroll();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void updateRecipes(List<CraftRecipe> newRecipes) {
        this.recipes = newRecipes;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        int totalHeight = recipes.size() * (ENTRY_HEIGHT + ENTRY_GAP);
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
        int newOffset = scrollOffset - amount * (ENTRY_HEIGHT + ENTRY_GAP);
        scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
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

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        context.enableScissor(getX(), getY(), getX() + width, getY() + height);

        // Элементы списка — полная ширина без резерва под скроллбар
        int visibleCount = height / (ENTRY_HEIGHT + ENTRY_GAP) + 1;
        int startIndex = scrollOffset / (ENTRY_HEIGHT + ENTRY_GAP);
        int leftMargin = 4;
        int elementWidth = width - leftMargin * 2;

        for (int i = 0; i <= visibleCount && startIndex + i < recipes.size(); i++) {
            int index = startIndex + i;
            CraftRecipe recipe = recipes.get(index);
            int entryY = getY() + i * (ENTRY_HEIGHT + ENTRY_GAP) - (scrollOffset % (ENTRY_HEIGHT + ENTRY_GAP));

            if (entryY + ENTRY_HEIGHT < getY() || entryY > getY() + height) continue;

            boolean isSelected = (index == selectedIndex);
            boolean isHovered = mouseX >= getX() && mouseX <= getX() + width &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT;

            int ex = getX() + leftMargin;

            // Определяем состояние текстуры: 0=обычное, 1=наведение, 2=выбрано
            int stateVOffset;
            if (isSelected) {
                stateVOffset = TEX_STATE_HEIGHT * 2;
            } else if (isHovered) {
                stateVOffset = TEX_STATE_HEIGHT;
            } else {
                stateVOffset = 0;
            }

            // Рисуем текстуру элемента списка на полную ширину
            context.drawTexture(LIST_ELEMENT_TEX, ex, entryY, 0, stateVOffset, elementWidth, ENTRY_HEIGHT, TEX_WIDTH, TEX_STATE_HEIGHT * 3);

            // Иконка результата
            ItemStack result = recipe.getResult();
            context.drawItem(result, ex + 6, entryY + 4);

            // Название предмета
            String name = result.getName().getString();
            int maxNameWidth = elementWidth - 30;
            if (textRenderer.getWidth(name) > maxNameWidth) {
                name = textRenderer.trimToWidth(name, maxNameWidth - 6) + "..";
            }
            context.getMatrices().push();
            context.getMatrices().translate(ex + 26, entryY + 6, 0);
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            context.drawText(textRenderer, Text.literal(name), 0, 0, COLOR_ITEM_NAME, false);
            context.getMatrices().pop();

            // Статусная строка
            String statusText;
            int statusColor;
            boolean hasIngredients = hasAllIngredients(recipe);
            boolean canAfford = NumismaticHelper.getTotalMoney(MinecraftClient.getInstance().player) >= recipe.getCost();

            if (!hasIngredients) {
                statusText = "Missing items";
                statusColor = COLOR_STATUS_MISSING;
            } else if (!canAfford) {
                statusText = "Not enough money";
                statusColor = COLOR_STATUS_MONEY;
            } else {
                statusText = "Ready to craft";
                statusColor = COLOR_STATUS_READY;
            }

            context.getMatrices().push();
            context.getMatrices().translate(ex + 26, entryY + 16, 0);
            context.getMatrices().scale(0.6f, 0.6f, 1.0f);
            context.drawText(textRenderer, Text.literal(statusText), 0, 0, statusColor, false);
            context.getMatrices().pop();
        }

        context.disableScissor();
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal("Recipe List"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        int step = ENTRY_HEIGHT + ENTRY_GAP;
        int startIndex = scrollOffset / step;
        int visibleCount = height / step + 1;

        for (int i = 0; i <= visibleCount && startIndex + i < recipes.size(); i++) {
            int index = startIndex + i;
            int entryY = getY() + i * step - (scrollOffset % step);

            if (mouseX >= getX() && mouseX <= getX() + width &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                selectRecipe(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;
        if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
            scroll((int) amount);
            return true;
        }
        return false;
    }

    private boolean hasAllIngredients(CraftRecipe recipe) {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        List<Ingredient> ingredients = recipe.getIngredients();

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            ItemStack template = ing.getItem();
            int need = ing.getCount();
            int have = 0;

            for (int s = 0; s < inv.size(); s++) {
                ItemStack stack = inv.getStack(s);
                if (ItemStack.areItemsEqual(stack, template)) {
                    have += stack.getCount();
                }
            }

            if (have < need) return false;
        }
        return true;
    }
}