package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class VillagerInteractionScreen extends HandledScreen<VillagerCraftingScreenHandler> {

    private static final int BG_COLOR_TOP = 0xC6C6C6;
    private static final int BG_COLOR_BOTTOM = 0x8B8B8B;
    private static final int BORDER_COLOR = 0xFF555555;

    private int selectedInventoryIndex = -1;
    private int selectedItemSlot = -1;
    private ItemStack selectedItemStack = ItemStack.EMPTY;
    private boolean hasSelectedItem = false;
    private boolean craftEnabled = false;

    // Размеры элементов
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_STEP = 19;
    private static final int ARMOR_SLOT_WIDTH = 18;
    private static final int ARMOR_SLOT_COUNT = 5;
    private static final int ARMOR_STEP = 20;
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 6;

    // Вычисляемые позиции
    private int armorX;
    private int inventoryX;
    private int centerX;
    private int recipesX;
    private int panelY;
    private int titleY;

    private Text playerInventoryLabel;
    private TabType currentTab = TabType.CRAFT;

    private ButtonWidget tradeTabButton;
    private ButtonWidget craftTabButton;
    private ButtonWidget disassembleTabButton;

    // Панель рецептов
    private int recipesPanelWidth = 150;
    private int recipesPanelHeight = 200;
    private boolean showRecipesPanel = true;

    // Список рецептов
    private RecipeScrollListWidget recipeScrollList;
    private List<CraftRecipe> availableRecipes = new ArrayList<>();
    private CraftRecipe currentRecipe;
    private int selectedRecipeIndex = -1;

    public VillagerInteractionScreen(VillagerCraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        this.backgroundWidth = (int) (this.client.getWindow().getScaledWidth() * 0.85);
        this.backgroundHeight = (int) (this.client.getWindow().getScaledHeight() * 0.80);

        super.init();
        this.playerInventoryLabel = Text.translatable("gui.tradeoverhaul.player_inventory");

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        int armorWidth = ARMOR_SLOT_WIDTH;
        int inventoryWidth = GRID_COLS * SLOT_STEP;
        int centerWidth = 160;
        int recipesWidth = recipesPanelWidth;

        int gapArmorInventory = 16;
        int gapInventoryCenter = 24;
        int gapCenterRecipes = 16;

        int totalContentWidth = armorWidth + gapArmorInventory + inventoryWidth + gapInventoryCenter + centerWidth + gapCenterRecipes + recipesWidth;
        int startX = x + (this.backgroundWidth - totalContentWidth) / 2;

        this.armorX = startX;
        this.inventoryX = armorX + armorWidth + gapArmorInventory;
        this.centerX = inventoryX + inventoryWidth + gapInventoryCenter;
        this.recipesX = centerX + centerWidth + gapCenterRecipes;

        int inventoryHeight = GRID_ROWS * SLOT_STEP;
        this.panelY = y + 35;
        this.titleY = this.panelY - 20;

        // Динамическая высота панели рецептов
        int availableHeight = this.backgroundHeight - this.panelY - 50;
        int maxVisible = Math.min(8, availableHeight / 24);
        recipesPanelHeight = maxVisible * 24 + 30;

        positionSlots();

        this.playerInventoryTitleX = inventoryX;
        this.playerInventoryTitleY = titleY;
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;

        int tabWidth = 60;
        int tabHeight = 20;
        int tabStartX = x + 10;
        int tabStartY = y + 10;

        tradeTabButton = ButtonWidget.builder(Text.literal("Trade"), button -> onTabSelected(TabType.TRADE))
                .dimensions(tabStartX, tabStartY, tabWidth, tabHeight).build();
        craftTabButton = ButtonWidget.builder(Text.literal("Craft"), button -> onTabSelected(TabType.CRAFT))
                .dimensions(tabStartX + tabWidth + 5, tabStartY, tabWidth, tabHeight).build();
        disassembleTabButton = ButtonWidget.builder(Text.literal("Disassemble"), button -> onTabSelected(TabType.DISASSEMBLE))
                .dimensions(tabStartX + (tabWidth + 5) * 2, tabStartY, tabWidth, tabHeight).build();

        addDrawableChild(tradeTabButton);
        addDrawableChild(craftTabButton);
        addDrawableChild(disassembleTabButton);

        updateTabButtons();
        loadRecipes();
    }

    private void positionSlots() {
        int armorSlotIndex = 0;
        int inventorySlotIndex = 0;

        for (int i = 0; i < this.handler.slots.size(); i++) {
            var slotObj = this.handler.slots.get(i);

            if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                slotObj.x = armorX + 1;
                slotObj.y = panelY + armorSlotIndex * ARMOR_STEP;
                armorSlotIndex++;
            } else {
                int col = inventorySlotIndex % GRID_COLS;
                int row = inventorySlotIndex / GRID_COLS;
                slotObj.x = inventoryX + col * SLOT_STEP + 1;
                slotObj.y = panelY + row * SLOT_STEP;
                inventorySlotIndex++;
            }
        }
    }

    private void loadRecipes() {
        String professionId = handler.getProfessionId();
        int level = handler.getVillagerLevel();
        availableRecipes = RecipeManager.getInstance().getCraftRecipesForProfession(professionId, level);
        createRecipeList();
    }

    private void createRecipeList() {
        if (recipeScrollList != null) {
            remove(recipeScrollList);
        }

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.recipesX - 30;
        int panelY = y + this.panelY - 30;

        int listWidth = recipesPanelWidth - 4;
        int listHeight = recipesPanelHeight - 25;

        recipeScrollList = new RecipeScrollListWidget(
                this.client,
                panelX + 2,
                panelY + 22,
                listWidth,
                listHeight,
                24,
                availableRecipes,
                () -> onRecipeSelected(recipeScrollList.getSelectedRecipe())
        );

        addSelectableChild(recipeScrollList);

        if (!availableRecipes.isEmpty()) {
            recipeScrollList.setSelectedIndex(0);
            onRecipeSelected(recipeScrollList.getSelectedRecipe());
        }

        updatePanelVisibility();
    }

    private void onRecipeSelected(CraftRecipe recipe) {
        if (recipe == null) return;
        this.currentRecipe = recipe;
        this.selectedRecipeIndex = availableRecipes.indexOf(recipe);
        this.hasSelectedItem = false;
        this.selectedItemStack = ItemStack.EMPTY;
        this.selectedItemSlot = -1;
        this.selectedInventoryIndex = -1;
        System.out.println("[TradeOverhaul] Selected recipe: " + recipe.getId());
    }

    private void onTabSelected(TabType tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        updateTabButtons();
        updatePanelVisibility();
    }

    private void updateTabButtons() {
        tradeTabButton.active = currentTab != TabType.TRADE;
        craftTabButton.active = currentTab != TabType.CRAFT;
        disassembleTabButton.active = currentTab != TabType.DISASSEMBLE;
    }

    private void updatePanelVisibility() {
        showRecipesPanel = (currentTab == TabType.CRAFT);
        if (recipeScrollList != null) {
            recipeScrollList.setVisible(showRecipesPanel);
        }
    }

    private void drawRecipeInfo(DrawContext context, int x, int y) {
        if (currentRecipe == null) return;

        int startX = x + this.centerX;
        int startY = y + this.panelY;
        int uniqueIndex = currentRecipe.getUniqueIngredientIndex();
        List<Ingredient> ingredients = currentRecipe.getIngredients();
        PlayerInventory inv = client.player.getInventory();

        context.drawText(this.textRenderer, Text.literal("Selected Recipe:"), startX, startY, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal(currentRecipe.getId()), startX, startY + 15, 0xFFFFAA, false);
        context.drawText(this.textRenderer, Text.literal("Cost: " + currentRecipe.getCost() + " copper"), startX, startY + 35, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Ingredients:"), startX, startY + 55, 0xCCCCCC, false);

        int itemY = startY + 70;
        boolean hasAllIngredients = true;
        int[] available = new int[ingredients.size()];

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            int count = 0;
            for (int slot = 0; slot < inv.size(); slot++) {
                ItemStack stack = inv.getStack(slot);
                if (stack.getItem() == ing.getItem().getItem()) count += stack.getCount();
            }
            available[i] = count;
        }

        if (uniqueIndex >= 0 && hasSelectedItem && !selectedItemStack.isEmpty()) {
            Ingredient uniqueIng = ingredients.get(uniqueIndex);
            if (selectedItemStack.getItem() == uniqueIng.getItem().getItem()) {
                int invCount = 0;
                for (int slot = 0; slot < inv.size(); slot++) {
                    if (slot == selectedItemSlot) continue;
                    ItemStack stack = inv.getStack(slot);
                    if (stack.getItem() == uniqueIng.getItem().getItem()) invCount += stack.getCount();
                }
                available[uniqueIndex] = invCount + selectedItemStack.getCount();
            }
        }

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            String itemName = ing.getItem().getItem().getName().getString();
            int needed = ing.getCount();
            int have = available[i];
            boolean hasEnough = have >= needed;
            if (!hasEnough) hasAllIngredients = false;
            int color = hasEnough ? 0x55FF55 : 0xFF6666;
            context.drawText(this.textRenderer, Text.literal((hasEnough ? "✓ " : "✗ ") + itemName + " x" + needed + " (have: " + have + ")"), startX + 5, itemY + i * 12, color, false);
        }

        int resultY = itemY + ingredients.size() * 12 + 15;
        context.drawText(this.textRenderer, Text.literal("Result:"), startX, resultY, 0xCCCCCC, false);
        context.drawText(this.textRenderer, Text.literal("• " + currentRecipe.getResult().getItem().getName().getString() + " x" + currentRecipe.getResult().getCount()), startX + 5, resultY + 12, 0xAAAAAA, false);

        boolean hasUniqueSelected = true;
        if (uniqueIndex >= 0) {
            hasUniqueSelected = hasSelectedItem && !selectedItemStack.isEmpty() &&
                    selectedItemStack.getItem() == ingredients.get(uniqueIndex).getItem().getItem() &&
                    selectedItemStack.getCount() >= ingredients.get(uniqueIndex).getCount();
            if (!hasUniqueSelected) hasAllIngredients = false;
        }

        int buttonY;
        if (uniqueIndex >= 0) {
            buttonY = resultY + 50;
            context.drawText(this.textRenderer, Text.literal("Selected Item:"), startX, resultY + 28, 0xCCCCCC, false);
            if (hasSelectedItem && !selectedItemStack.isEmpty()) {
                context.drawText(this.textRenderer, Text.literal(selectedItemStack.getItem().getName().getString() + " x" + selectedItemStack.getCount()), startX + 5, resultY + 40, hasUniqueSelected ? 0x55FF55 : 0xFF6666, false);
            } else {
                context.drawText(this.textRenderer, Text.literal("None (RMB click on item)"), startX + 5, resultY + 40, 0xFF6666, false);
            }
        } else {
            buttonY = resultY + 40;
        }

        int buttonX = startX;
        context.fill(buttonX, buttonY, buttonX + 60, buttonY + 20, hasAllIngredients ? 0xFF444444 : 0xFF333333);
        context.fill(buttonX + 1, buttonY + 1, buttonX + 59, buttonY + 19, hasAllIngredients ? 0xFF666666 : 0xFF444444);
        context.drawText(this.textRenderer, Text.literal("Craft"), buttonX + 15, buttonY + 6, hasAllIngredients ? 0xFFFFFF : 0x888888, false);
        craftEnabled = hasAllIngredients;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        if (showRecipesPanel) {
            int panelX = x + this.recipesX - 30;
            int panelY = y + this.panelY - 30;
            context.fill(panelX, panelY, panelX + recipesPanelWidth, panelY + recipesPanelHeight, 0xCC000000);
            context.fill(panelX + 1, panelY + 1, panelX + recipesPanelWidth - 1, panelY + recipesPanelHeight - 1, 0xCC333333);
            context.fill(panelX, panelY, panelX + recipesPanelWidth, panelY + 1, 0xFFAAAAAA);
            context.fill(panelX, panelY + recipesPanelHeight - 1, panelX + recipesPanelWidth, panelY + recipesPanelHeight, 0xFFAAAAAA);
            context.fill(panelX, panelY, panelX + 1, panelY + recipesPanelHeight, 0xFFAAAAAA);
            context.fill(panelX + recipesPanelWidth - 1, panelY, panelX + recipesPanelWidth, panelY + recipesPanelHeight, 0xFFAAAAAA);
            context.drawText(this.textRenderer, Text.literal("Recipes"), panelX + 5, panelY + 5, 0xFFFFFF, false);
            context.fill(panelX + 2, panelY + 18, panelX + recipesPanelWidth - 2, panelY + 19, 0xFF666666);
        }

        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        context.drawText(this.textRenderer, this.playerInventoryLabel, x + this.inventoryX, y + this.titleY, 0xFFFFFF, true);
        int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
        var playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
        playerCurrency.setTotalCopper(playerMoney);
        context.drawText(this.textRenderer, playerCurrency.formatMoneyVertical(), x + this.inventoryX, y + this.titleY + 12, 0xFFFFFF, true);
        drawRecipeInfo(context, x, y);
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

        int armorXabs = x + this.armorX;
        for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
            int slotX = armorXabs;
            int slotY = y + this.panelY + i * ARMOR_STEP - 1;
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8B8B8B8B);
            context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, 0xFF303030);
        }

        int inventoryXabs = x + this.inventoryX;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotX = inventoryXabs + col * SLOT_STEP;
                int slotY = y + this.panelY + row * SLOT_STEP - 1;
                context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8B8B8B8B);
                context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, 0xFF303030);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}

    @Override
    public void removed() {
        if (recipeScrollList != null) {
            remove(recipeScrollList);
        }
        super.removed();
    }

    private Slot getSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.handler.slots) {
            if (isPointOverSlot(slot, mouseX, mouseY)) return slot;
        }
        return null;
    }

    private boolean isPointOverSlot(Slot slot, double mouseX, double mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int slotX = x + slot.x;
        int slotY = y + slot.y;
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private boolean isPointOverCraftButton(double mouseX, double mouseY) {
        if (currentRecipe == null) return false;
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int startX = x + this.centerX;
        int startY = y + this.panelY;
        List<Ingredient> ingredients = currentRecipe.getIngredients();
        int itemY = startY + 70;
        int resultY = itemY + ingredients.size() * 12 + 15;
        int buttonY = currentRecipe.getUniqueIngredientIndex() >= 0 ? resultY + 50 : resultY + 40;
        return mouseX >= startX && mouseX < startX + 60 && mouseY >= buttonY && mouseY < buttonY + 20;
    }

    private int getRealInventoryIndex(int screenSlot) {
        if (screenSlot >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
            int gridIndex = screenSlot - VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX;
            if (gridIndex >= 0 && gridIndex < 36) return gridIndex;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && currentRecipe != null && currentRecipe.getUniqueIngredientIndex() >= 0) {
            Slot hoveredSlot = getSlotAt(mouseX, mouseY);
            if (hoveredSlot != null) {
                int slotIndex = hoveredSlot.id;
                if (slotIndex >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                    ItemStack stack = hoveredSlot.getStack();
                    if (!stack.isEmpty()) {
                        this.selectedItemSlot = slotIndex;
                        this.selectedItemStack = stack.copy();
                        this.hasSelectedItem = true;
                        this.selectedInventoryIndex = getRealInventoryIndex(slotIndex);
                        return true;
                    }
                }
            }
        }

        if (button == 0 && currentRecipe != null && isPointOverCraftButton(mouseX, mouseY)) {
            if (!craftEnabled) return true;
            int slotToSend = currentRecipe.getUniqueIngredientIndex() < 0 ? -1 : selectedInventoryIndex;
            com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket packet = new com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket(
                    handler.syncId, currentRecipe.getId(), slotToSend);
            PacketByteBuf buf = PacketByteBufs.create();
            com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket.encode(packet, buf);
            ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "craft_request"), buf);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}