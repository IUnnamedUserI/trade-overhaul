package com.unnameduser.tradeoverhaul.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.network.RepairAllRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.network.RepairRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.DamagedItem;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
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

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_STEP = 19;
    private static final int ARMOR_SLOT_WIDTH = 18;
    private static final int ARMOR_SLOT_COUNT = 5;
    private static final int ARMOR_STEP = 20;
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 6;

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

    private int recipesPanelWidth = 150;
    private int recipesPanelHeight = 200;
    private boolean showRecipesPanel = true;

    private RecipeListPanel recipeListPanel;
    private List<CraftRecipe> availableRecipes = new ArrayList<>();
    private CraftRecipe currentRecipe;
    private int selectedRecipeIndex = -1;

    private TextFieldWidget searchField;
    private List<CraftRecipe> allRecipes = new ArrayList<>();

    // ✓ УДАЛЕНО: private VillagerTradeScreenHandler tradeHandler;
    // ✓ УДАЛЕНО: private VillagerTradeScreen tradeScreen;

    private ItemStack filteredItem = ItemStack.EMPTY;
    private boolean isFilterActive = false;
    private int filteredSlot = -1;

    private net.minecraft.client.util.math.Rect2i craftButtonBounds;

    private RepairPanel repairPanel;
    private List<DamagedItem> damagedItems = new ArrayList<>();
    private boolean showRepairPanel = true;
    private boolean repairEnabled = false;
    private boolean repairAllEnabled = false;
    private net.minecraft.client.util.math.Rect2i repairButtonBounds;
    private net.minecraft.client.util.math.Rect2i repairAllButtonBounds;
    private DamagedItem currentRepairItem;
    private ButtonWidget repairTabButton;

    private boolean renderSlots = true;

    // Позиции для панели торговли
    private int tradePanelX;
    private int tradePanelY;
    private static final int TRADE_SLOTS_COLS = 2;
    private static final int TRADE_SLOTS_ROWS = 4;

    private static final int TRADE_SLOT_SIZE = 18;
    private static final int TRADE_SLOT_STEP = 19;

    private static final int SLOT_BORDER_CAN_AFFORD = 0xFF00FFFF;
    private static final int SLOT_BORDER_CANNOT_AFFORD = 0xFFFF5555;

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

        int availableHeight = this.backgroundHeight - this.panelY - 50;
        int maxVisible = Math.min(8, availableHeight / 24);
        recipesPanelHeight = maxVisible * 24 + 55;

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
        repairTabButton = ButtonWidget.builder(Text.literal("Repair"), button -> onTabSelected(TabType.REPAIR))
                .dimensions(tabStartX + (tabWidth + 5) * 3, tabStartY, tabWidth, tabHeight).build();

        addDrawableChild(tradeTabButton);
        addDrawableChild(craftTabButton);
        addDrawableChild(disassembleTabButton);
        addDrawableChild(repairTabButton);

        updateTabButtons();
        loadRecipes();
        createSearchField();

        // ✓ УДАЛЕНО: initTradeScreen();

        refreshDamagedItems();
        createRepairPanel();

        // Инициализация позиций панели торговли
        // === НОВОЕ: Позиция панели торговли (справа от центральной панели) ===
        // Вычисляем относительно фона, а не абсолютных координат
        int tradePanelOffset = 16; // отступ от центральной панели
        this.tradePanelX = this.centerX + 160 + tradePanelOffset; // 160 = ширина центральной панели
        this.tradePanelY = this.panelY;
    }

    private void createSearchField() {
        if (searchField != null) remove(searchField);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.recipesX - 30;
        int panelY = y + this.panelY - 30;

        searchField = new TextFieldWidget(this.textRenderer, panelX + 5, panelY + 22, recipesPanelWidth - 10, 14, Text.literal("Search"));
        searchField.setMaxLength(50);
        searchField.setDrawsBackground(true);
        searchField.setVisible(showRecipesPanel);
        searchField.setChangedListener(this::filterRecipes);
        addDrawableChild(searchField);
    }

    private void positionSlots() {
        int armorSlotIndex = 0;
        int inventorySlotIndex = 0;
        int tradeSlotIndex = 0;

        for (int i = 0; i < this.handler.slots.size(); i++) {
            var slotObj = this.handler.slots.get(i);

            // === Скрываем ВСЕ слоты на вкладке REPAIR ===
            if (currentTab == TabType.REPAIR) {
                slotObj.x = -1000;
                slotObj.y = -1000;
                continue;
            }

            if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                // Слоты брони (показываем на всех вкладках кроме REPAIR)
                slotObj.x = armorX + 1;
                slotObj.y = panelY + armorSlotIndex * ARMOR_STEP;
                armorSlotIndex++;

            } else if (i < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                // Слоты инвентаря игрока (показываем на всех вкладках кроме REPAIR)
                int col = inventorySlotIndex % GRID_COLS;
                int row = inventorySlotIndex / GRID_COLS;
                slotObj.x = inventoryX + col * SLOT_STEP + 1;
                slotObj.y = panelY + row * SLOT_STEP;
                inventorySlotIndex++;

            } else {
                // === Слоты торговли жителя — ТОЛЬКО на вкладке TRADE ===
                if (currentTab == TabType.TRADE) {
                    int col = tradeSlotIndex % GRID_COLS;
                    int row = tradeSlotIndex / GRID_COLS;
                    slotObj.x = tradePanelX + col * SLOT_STEP + 1;
                    slotObj.y = tradePanelY + row * SLOT_STEP;
                    tradeSlotIndex++;
                } else {
                    // Скрываем на всех остальных вкладках
                    slotObj.x = -1000;
                    slotObj.y = -1000;
                    tradeSlotIndex++;
                }
            }
        }
    }

    private void loadRecipes() {
        String professionId = handler.getProfessionId();
        int level = handler.getVillagerLevel();
        allRecipes = RecipeManager.getInstance().getCraftRecipesForProfession(professionId, level);
        allRecipes.sort((a, b) -> {
            int levelCompare = Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
            if (levelCompare != 0) return levelCompare;
            return a.getResult().getName().getString().compareToIgnoreCase(b.getResult().getName().getString());
        });
        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);
        createRecipeList();
    }

    private void createRecipeList() {
        if (recipeListPanel != null) remove(recipeListPanel);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.recipesX - 30;
        int panelY = y + this.panelY - 30;

        int listWidth = recipesPanelWidth - 4;
        int listHeight = recipesPanelHeight - 42;

        recipeListPanel = new RecipeListPanel(this, panelX + 2, panelY + 40, listWidth, listHeight, availableRecipes,
                () -> onRecipeSelected(recipeListPanel.getSelectedRecipe()));
        addDrawableChild(recipeListPanel);

        if (!availableRecipes.isEmpty()) {
            recipeListPanel.setSelectedIndex(-1);
            currentRecipe = null;
            selectedRecipeIndex = -1;
        }
        updatePanelVisibility();
    }

    private void onRecipeSelected(CraftRecipe recipe) {
        if (recipe == null) return;
        this.currentRecipe = recipe;
        this.selectedRecipeIndex = availableRecipes.indexOf(recipe);

        if (isFilterActive && !filteredItem.isEmpty()) {
            int uniqueIdx = recipe.getUniqueIngredientIndex();
            if (uniqueIdx >= 0 && uniqueIdx < recipe.getIngredients().size()) {
                Ingredient uniqueIng = recipe.getIngredients().get(uniqueIdx);
                if (ItemStack.areItemsEqual(uniqueIng.getItem(), filteredItem)) {
                    this.selectedItemSlot = filteredSlot;
                    this.selectedItemStack = filteredItem.copy();
                    this.hasSelectedItem = true;
                    this.selectedInventoryIndex = getRealInventoryIndex(filteredSlot);
                    return;
                }
            }
        }
        this.hasSelectedItem = false;
        this.selectedItemStack = ItemStack.EMPTY;
        this.selectedItemSlot = -1;
        this.selectedInventoryIndex = -1;
    }

    private void onTabSelected(TabType tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        positionSlots();

        // ✓ УДАЛЕНО: if (tab == TabType.TRADE && tradeScreen == null) initTradeScreen();

        if (tab == TabType.REPAIR) {
            refreshDamagedItems();
            if (repairPanel != null) repairPanel.updateItems(damagedItems);
        }
        updateTabButtons();
        updatePanelVisibility();
    }

    private void updateTabButtons() {
        tradeTabButton.active = currentTab != TabType.TRADE;
        craftTabButton.active = currentTab != TabType.CRAFT;
        disassembleTabButton.active = currentTab != TabType.DISASSEMBLE;
        repairTabButton.active = currentTab != TabType.REPAIR;
    }

    private void updatePanelVisibility() {
        showRecipesPanel = (currentTab == TabType.CRAFT);
        showRepairPanel = (currentTab == TabType.REPAIR);
        if (recipeListPanel != null) recipeListPanel.setVisible(showRecipesPanel);
        if (searchField != null) searchField.setVisible(showRecipesPanel);
        if (repairPanel != null) repairPanel.setVisible(showRepairPanel);
    }

    private void drawRecipeInfo(DrawContext context, int x, int y) {
        if (currentRecipe == null) return;
        int startX = x + this.centerX;
        int startY = y + this.panelY;
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

        if (currentRecipe.getUniqueIngredientIndex() >= 0 && hasSelectedItem && !selectedItemStack.isEmpty()) {
            Ingredient uniqueIng = ingredients.get(currentRecipe.getUniqueIngredientIndex());
            if (selectedItemStack.getItem() == uniqueIng.getItem().getItem()) {
                int invCount = 0;
                for (int slot = 0; slot < inv.size(); slot++) {
                    if (slot == selectedItemSlot) continue;
                    ItemStack stack = inv.getStack(slot);
                    if (stack.getItem() == uniqueIng.getItem().getItem()) invCount += stack.getCount();
                }
                available[currentRecipe.getUniqueIngredientIndex()] = invCount + selectedItemStack.getCount();
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
        if (currentRecipe.getUniqueIngredientIndex() >= 0) {
            hasUniqueSelected = hasSelectedItem && !selectedItemStack.isEmpty() &&
                    selectedItemStack.getItem() == ingredients.get(currentRecipe.getUniqueIngredientIndex()).getItem().getItem() &&
                    selectedItemStack.getCount() >= ingredients.get(currentRecipe.getUniqueIngredientIndex()).getCount();
            if (!hasUniqueSelected) hasAllIngredients = false;
        }

        boolean itemNotDamaged = true;
        if (currentRecipe.getUniqueIngredientIndex() >= 0 && hasSelectedItem && !selectedItemStack.isEmpty()) {
            itemNotDamaged = !selectedItemStack.isDamageable() || selectedItemStack.getDamage() == 0;
        }

        boolean canCraft = hasAllIngredients && (currentRecipe.getUniqueIngredientIndex() < 0 || itemNotDamaged);
        craftEnabled = canCraft;

        int buttonY = currentRecipe.getUniqueIngredientIndex() >= 0 ? resultY + 50 : resultY + 40;
        int buttonX = startX;
        context.fill(buttonX, buttonY, buttonX + 60, buttonY + 20, canCraft ? 0xFF444444 : 0xFF333333);
        context.fill(buttonX + 1, buttonY + 1, buttonX + 59, buttonY + 19, canCraft ? 0xFF666666 : 0xFF444444);
        context.drawText(this.textRenderer, Text.literal("Craft"), buttonX + 15, buttonY + 6, canCraft ? 0xFFFFFF : 0x888888, false);
        craftButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, buttonY, 60, 20);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (currentTab == TabType.TRADE) {
            this.renderBackground(context);
            super.render(context, mouseX, mouseY, delta); // ← Отрисовка фона, слотов, предметов

            int x = (this.width - this.backgroundWidth) / 2;
            int y = (this.height - this.backgroundHeight) / 2;

            // ✅ СТРОГО ПОСЛЕ предметов, ДО тултипов
            drawTradeSlotIndicators(context, x, y);

            this.drawMouseoverTooltip(context, mouseX, mouseY);
            drawTradePanel(context, mouseX, mouseY, delta);

            int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
            var playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
            playerCurrency.setTotalCopper(playerMoney);
            context.drawText(this.textRenderer, playerCurrency.formatMoneyVertical(), x + this.inventoryX, y + this.titleY + 12, 0xFFFFFF, true);
        } else if (currentTab == TabType.CRAFT) {
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

            if (showRecipesPanel && isFilterActive && !filteredItem.isEmpty()) {
                int panelX = x + this.recipesX - 30;
                int panelY = y + this.panelY - 30;
                String filterText = "Filter: " + filteredItem.getItem().getName().getString();
                context.drawText(this.textRenderer, Text.literal(filterText), panelX + 5, panelY + 30, 0xFFFFAA, false);
            }

            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);
            context.drawText(this.textRenderer, this.playerInventoryLabel, x + this.inventoryX, y + this.titleY, 0xFFFFFF, true);
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            var playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
            playerCurrency.setTotalCopper(playerMoney);
            context.drawText(this.textRenderer, playerCurrency.formatMoneyVertical(), x + this.inventoryX, y + this.titleY + 12, 0xFFFFFF, true);
            drawRecipeInfo(context, x, y);

            if (currentRecipe != null && craftButtonBounds != null &&
                    mouseX >= craftButtonBounds.getX() && mouseX <= craftButtonBounds.getX() + craftButtonBounds.getWidth() &&
                    mouseY >= craftButtonBounds.getY() && mouseY <= craftButtonBounds.getY() + craftButtonBounds.getHeight()) {
                if (isSelectedItemDamaged()) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.literal(getDamageWarning()));
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                } else if (!craftEnabled && currentRecipe != null) {
                    List<Text> tooltip = new ArrayList<>();
                    if (currentRecipe.getUniqueIngredientIndex() >= 0 && !hasSelectedItem) {
                        tooltip.add(Text.literal("§eSelect a unique item (RMB click)"));
                    } else {
                        tooltip.add(Text.literal("§cMissing ingredients or insufficient funds"));
                    }
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                }
            }

        } else if (currentTab == TabType.DISASSEMBLE) {
            this.renderBackground(context);
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

        } else if (currentTab == TabType.REPAIR) {
            this.renderBackground(context);
            int x = (this.width - this.backgroundWidth) / 2;
            int y = (this.height - this.backgroundHeight) / 2;

            if (repairPanel != null) repairPanel.render(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

            int rightPanelX = x + this.inventoryX + (GRID_COLS * SLOT_STEP) + 120;
            int rightPanelY = y + this.panelY - 20;
            int rightPanelWidth = 180;
            int rightPanelHeight = 200;

            context.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + rightPanelHeight, 0xCC000000);
            context.fill(rightPanelX + 1, rightPanelY + 1, rightPanelX + rightPanelWidth - 1, rightPanelY + rightPanelHeight - 1, 0xCC333333);
            context.drawBorder(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight, 0xFF666666);

            int buttonX = rightPanelX - 85;
            int buttonY = rightPanelY + 70;
            int buttonWidth = 60;
            int buttonHeight = 20;

            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, repairEnabled ? 0xFF444444 : 0xFF333333);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1, repairEnabled ? 0xFF666666 : 0xFF444444);
            context.drawText(this.textRenderer, Text.literal("Repair"), buttonX + (buttonWidth / 2) - (this.textRenderer.getWidth("Repair") / 2), buttonY + 6, repairEnabled ? 0xFFFFFF : 0x888888, false);
            repairButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, buttonY, buttonWidth, buttonHeight);

            int repairAllButtonY = buttonY + buttonHeight + 5;
            int totalCost = getTotalRepairCost();
            repairAllEnabled = NumismaticHelper.getTotalMoney(this.client.player) >= totalCost && !damagedItems.isEmpty();

            context.fill(buttonX, repairAllButtonY, buttonX + buttonWidth, repairAllButtonY + buttonHeight, repairAllEnabled ? 0xFF444444 : 0xFF333333);
            context.fill(buttonX + 1, repairAllButtonY + 1, buttonX + buttonWidth - 1, repairAllButtonY + buttonHeight - 1, repairAllEnabled ? 0xFF666666 : 0xFF444444);
            context.drawText(this.textRenderer, Text.literal("Repair All"), buttonX + (buttonWidth / 2) - (this.textRenderer.getWidth("Repair All") / 2), repairAllButtonY + 6, repairAllEnabled ? 0xFFFFFF : 0x888888, false);
            repairAllButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, repairAllButtonY, buttonWidth, buttonHeight);

            if (repairPanel != null && repairPanel.getSelectedItem() != null) {
                DamagedItem selected = repairPanel.getSelectedItem();
                int startX = rightPanelX + rightPanelWidth / 2;
                int startY = rightPanelY + 10;

                context.getMatrices().push();
                context.getMatrices().scale(2.0f, 2.0f, 1.0f);
                context.drawItem(selected.getStack(), (startX) / 2 - 8, (startY) / 2 + 10);
                context.getMatrices().pop();

                String itemName = selected.getStack().getName().getString();
                int itemNameWidth = this.textRenderer.getWidth(itemName);
                int centerX = startX;
                int centeredX = centerX - (itemNameWidth / 2);
                context.drawText(this.textRenderer, itemName, centeredX, startY, 0xFFFFFF, false);

                int durabilityPercent = (int) ((1.0 - (double) selected.getCurrentDamage() / selected.getMaxDamage()) * 100);
                String durabilityText = "Durability";
                context.drawText(this.textRenderer, Text.literal(durabilityText), centerX - (this.textRenderer.getWidth(durabilityText) / 2), startY + 55, 0xCCCCCC, false);
                String durabilityPercentText = String.format("%d%%", durabilityPercent);
                context.drawText(this.textRenderer, Text.literal(durabilityPercentText), centerX - (this.textRenderer.getWidth(durabilityPercentText) / 2), startY + 65, 0xCCCCCC, false);

                String costText = String.format("Cost: %d copper", selected.getRepairCost());
                context.drawText(this.textRenderer, Text.literal(costText), centerX - (this.textRenderer.getWidth(costText) / 2), startY + 85, 0xFFFFAA, false);

                if (!repairEnabled && repairButtonBounds != null &&
                        mouseX >= repairButtonBounds.getX() && mouseX <= repairButtonBounds.getX() + repairButtonBounds.getWidth() &&
                        mouseY >= repairButtonBounds.getY() && mouseY <= repairButtonBounds.getY() + repairButtonBounds.getHeight()) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.literal("§cNeed " + selected.getRepairCost() + " copper"));
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                }
            } else {
                int startX = rightPanelX + 10;
                int startY = rightPanelY + 10;
                context.drawText(this.textRenderer, Text.literal("No item selected"), startX, startY, 0xCCCCCC, false);
                context.drawText(this.textRenderer, Text.literal("Click on a damaged item"), startX, startY + 15, 0x888888, false);
            }

            if (!repairAllEnabled && repairAllButtonBounds != null && !damagedItems.isEmpty() &&
                    mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                    mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal("§cNeed " + totalCost + " copper to repair all"));
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }

            context.drawText(this.textRenderer, this.playerInventoryLabel, x + this.inventoryX, y + this.titleY, 0xFFFFFF, true);
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            var playerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
            playerCurrency.setTotalCopper(playerMoney);
            context.drawText(this.textRenderer, playerCurrency.formatMoneyVertical(), x + this.inventoryX, y + this.titleY + 12, 0xFFFFFF, true);
        }
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

        if (currentTab != TabType.REPAIR) {
            drawSlotBackgrounds(context, x, y);
        }
    }

    private void drawSlotBackgrounds(DrawContext context, int x, int y) {
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

        if (currentTab == TabType.TRADE) {
            int tradeX = x + this.tradePanelX;
            int tradeY = y + this.panelY;

            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int slotX = tradeX + col * SLOT_STEP;
                    int slotY = tradeY + row * SLOT_STEP - 1;

                    // Фон слота
                    context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8B8B8B8B);
                    context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, 0xFF303030);
                }
            }
        }

        if (isFilterActive && filteredSlot >= 0) {
            for (int i = 0; i < this.handler.slots.size(); i++) {
                var slotObj = this.handler.slots.get(i);
                if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) continue;
                if (i >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) continue;
                int realSlot = getRealInventoryIndex(i);
                if (realSlot == filteredSlot) {
                    int slotX = x + slotObj.x - 1;
                    int slotY = y + slotObj.y - 1;
                    context.drawBorder(slotX, slotY, SLOT_SIZE + 2, SLOT_SIZE + 2, 0xFFD4AF37);
                    break;
                }
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}

    @Override
    public void removed() {
        if (recipeListPanel != null) remove(recipeListPanel);
        if (searchField != null) remove(searchField);
        // ✓ УДАЛЕНО: if (tradeScreen != null) { tradeScreen.removed(); tradeScreen = null; }
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
        if (screenSlot >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX &&
                screenSlot < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
            int gridIndex = screenSlot - VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX;
            if (gridIndex >= 0 && gridIndex < 36) return gridIndex;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (currentTab == TabType.CRAFT) {
                if (searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight()) {
                    searchField.setFocused(true);
                    return searchField.mouseClicked(mouseX, mouseY, button);
                } else if (searchField != null) {
                    searchField.setFocused(false);
                }
            }

            if (currentTab == TabType.CRAFT && currentRecipe != null && isPointOverCraftButton(mouseX, mouseY)) {
                if (!craftEnabled) {
                    if (isSelectedItemDamaged() && this.client.player != null) {
                        this.client.player.sendMessage(Text.literal(getDamageWarning()), false);
                    }
                    return true;
                }
                int slotToSend = currentRecipe.getUniqueIngredientIndex() < 0 ? -1 : selectedInventoryIndex;
                com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket packet = new com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket(
                        handler.syncId, currentRecipe.getId(), slotToSend);
                PacketByteBuf buf = PacketByteBufs.create();
                com.unnameduser.tradeoverhaul.common.network.CraftRequestC2SPacket.encode(packet, buf);
                ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "craft_request"), buf);
                resetAllFilters();
                return true;
            }

            if (currentTab == TabType.REPAIR && repairPanel != null && repairPanel.getSelectedItem() != null &&
                    repairButtonBounds != null && mouseX >= repairButtonBounds.getX() && mouseX <= repairButtonBounds.getX() + repairButtonBounds.getWidth() &&
                    mouseY >= repairButtonBounds.getY() && mouseY <= repairButtonBounds.getY() + repairButtonBounds.getHeight()) {
                if (!repairEnabled) return true;
                DamagedItem selected = repairPanel.getSelectedItem();
                RepairRequestC2SPacket packet = new RepairRequestC2SPacket(handler.syncId, selected.getSlotIndex());
                PacketByteBuf buf = PacketByteBufs.create();
                RepairRequestC2SPacket.encode(packet, buf);
                ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "repair_request"), buf);
                return true;
            }

            if (currentTab == TabType.REPAIR && repairAllButtonBounds != null && !damagedItems.isEmpty() &&
                    mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                    mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {
                if (!repairAllEnabled) return true;
                RepairAllRequestC2SPacket packet = new RepairAllRequestC2SPacket(handler.syncId);
                PacketByteBuf buf = PacketByteBufs.create();
                RepairAllRequestC2SPacket.encode(packet, buf);
                ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "repair_all_request"), buf);
                return true;
            }

            if (currentTab == TabType.CRAFT) {
                boolean clickedOnRecipePanel = isPointOverRecipePanel(mouseX, mouseY);
                boolean clickedOnCraftButton = isPointOverCraftButton(mouseX, mouseY);
                boolean clickedOnSlot = getSlotAt(mouseX, mouseY) != null;
                boolean clickedOnSearchField = searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight();
                if (!clickedOnRecipePanel && !clickedOnCraftButton && !clickedOnSlot && !clickedOnSearchField) {
                    resetAllFilters();
                }
            }

            if (currentTab == TabType.REPAIR) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null) {
                    this.client.execute(() -> {
                        refreshDamagedItems();
                        if (repairPanel != null) {
                            repairPanel.updateItems(damagedItems);
                            if (repairPanel.getSelectedItem() != null) {
                                boolean stillExists = damagedItems.stream().anyMatch(d -> d.getSlotIndex() == repairPanel.getSelectedItem().getSlotIndex());
                                if (!stillExists) {
                                    repairPanel.updateItems(damagedItems);
                                    onRepairItemSelected(null);
                                }
                            }
                        }
                    });
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        // === ПКМ ===
        if (button == 1) {
            if (currentTab == TabType.CRAFT) {
                Slot hoveredSlot = getSlotAt(mouseX, mouseY);
                if (hoveredSlot != null) {
                    int slotIndex = hoveredSlot.id;
                    if (slotIndex >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX &&
                            slotIndex < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                        ItemStack stack = hoveredSlot.getStack();
                        if (!stack.isEmpty()) {
                            if (currentRecipe != null && currentRecipe.getUniqueIngredientIndex() >= 0) {
                                int uniqueIdx = currentRecipe.getUniqueIngredientIndex();
                                if (uniqueIdx < currentRecipe.getIngredients().size()) {
                                    Ingredient uniqueIng = currentRecipe.getIngredients().get(uniqueIdx);
                                    if (ItemStack.areItemsEqual(stack, uniqueIng.getItem())) {
                                        this.selectedItemSlot = slotIndex;
                                        this.selectedItemStack = stack.copy();
                                        this.hasSelectedItem = true;
                                        this.selectedInventoryIndex = getRealInventoryIndex(slotIndex);
                                        return true;
                                    }
                                }
                            }
                            filterRecipesByItem(stack, slotIndex);
                            return true;
                        }
                    }
                }
                return true;
            }

            if (button == 1 && currentTab == TabType.TRADE) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null) {
                    int slotIndex = clickedSlot.id;
                    ItemStack stack = clickedSlot.getStack();
                    if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

                    boolean buying = slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT;
                    if (buying && !handler.clientHasInventorySpaceForStack(stack, 1)) return true;

                    boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
                    boolean ctrlPressed = net.minecraft.client.gui.screen.Screen.hasControlDown();

                    // ✓ Имя пакета должно совпадать с тем, что зарегистрировано на сервере!
                    var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                    buf.writeInt(handler.syncId);
                    buf.writeInt(slotIndex);
                    buf.writeBoolean(buying);              // покупка или продажа
                    buf.writeBoolean(shiftPressed && !buying); // sellWholeStack
                    buf.writeBoolean(shiftPressed && buying);  // buyWholeStack
                    buf.writeBoolean(ctrlPressed);              // buyTen/sellTen

                    // ✓ Имя пакета: "trade_action" (должно совпадать с серверной регистрацией!)
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new net.minecraft.util.Identifier(com.unnameduser.tradeoverhaul.TradeOverhaulMod.MOD_ID, "trade_action"),
                            buf);
                    return true;
                }
            }

            return true;
        }

        if (button == 1 && currentTab == TabType.TRADE) {
            Slot clickedSlot = getSlotAt(mouseX, mouseY);
            if (clickedSlot != null) {
                int slotIndex = clickedSlot.id;
                ItemStack stack = clickedSlot.getStack();
                if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

                boolean buying = slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT;

                // Проверка места в инвентаре при покупке
                if (buying && !handler.clientHasInventorySpaceForStack(stack, 1)) {
                    return true;
                }

                boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
                boolean ctrlPressed = net.minecraft.client.gui.screen.Screen.hasControlDown();

                var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeInt(handler.syncId);
                buf.writeInt(slotIndex);
                buf.writeBoolean(buying);
                buf.writeBoolean(shiftPressed && !buying); // sellWholeStack
                buf.writeBoolean(shiftPressed && buying);  // buyWholeStack
                buf.writeBoolean(ctrlPressed);              // buyTen/sellTen

                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new net.minecraft.util.Identifier(com.unnameduser.tradeoverhaul.TradeOverhaulMod.MOD_ID, "trade_action"), buf);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isPointOverRecipePanel(double mouseX, double mouseY) {
        if (recipeListPanel == null) return false;
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.recipesX - 30;
        int panelY = y + this.panelY - 30;
        return mouseX >= panelX && mouseX <= panelX + recipesPanelWidth &&
                mouseY >= panelY && mouseY <= panelY + recipesPanelHeight;
    }

    private void filterRecipes(String searchText) {
        availableRecipes.clear();
        if (searchText == null || searchText.trim().isEmpty()) {
            availableRecipes.addAll(allRecipes);
        } else {
            String lowerSearch = searchText.toLowerCase();
            for (CraftRecipe recipe : allRecipes) {
                if (recipe.getResult().getName().getString().toLowerCase().contains(lowerSearch)) {
                    availableRecipes.add(recipe);
                }
            }
        }
        if (recipeListPanel != null) recipeListPanel.updateRecipes(availableRecipes);
        currentRecipe = null;
        selectedRecipeIndex = -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == 69) return true;
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            if (searchField.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    public VillagerCraftingScreenHandler getCraftingHandler() { return this.handler; }

    // ✓ ИСПРАВЛЕНО: Убраны вызовы к tradeScreen
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // ✓ Раньше было: if (currentTab == TabType.TRADE) return tradeScreen.mouseScrolled(...);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        // ✓ Раньше было: if (tradeScreen != null) tradeScreen.resize(...);
    }

    // ✓ УДАЛЕНО: метод initTradeScreen()

    private void filterRecipesByItem(ItemStack item, int slotIndex) {
        if (item.isEmpty()) return;
        this.filteredItem = item.copy();
        this.filteredSlot = slotIndex;
        this.isFilterActive = true;

        List<CraftRecipe> filtered = new ArrayList<>();
        for (CraftRecipe recipe : allRecipes) {
            for (Ingredient ing : recipe.getIngredients()) {
                if (ItemStack.areItemsEqual(ing.getItem(), item)) {
                    filtered.add(recipe);
                    break;
                }
            }
        }
        availableRecipes.clear();
        availableRecipes.addAll(filtered);
        if (recipeListPanel != null) recipeListPanel.updateRecipes(availableRecipes);
        currentRecipe = null;
        selectedRecipeIndex = -1;
        hasSelectedItem = false;
        selectedItemStack = ItemStack.EMPTY;
        selectedItemSlot = -1;
        selectedInventoryIndex = -1;

        if (availableRecipes.size() == 1) {
            onRecipeSelected(availableRecipes.get(0));
            int uniqueIdx = currentRecipe.getUniqueIngredientIndex();
            if (uniqueIdx >= 0 && uniqueIdx < currentRecipe.getIngredients().size()) {
                Ingredient uniqueIng = currentRecipe.getIngredients().get(uniqueIdx);
                if (ItemStack.areItemsEqual(uniqueIng.getItem(), item)) {
                    this.selectedItemSlot = slotIndex;
                    this.selectedItemStack = item.copy();
                    this.hasSelectedItem = true;
                    this.selectedInventoryIndex = getRealInventoryIndex(slotIndex);
                }
            }
        }
    }

    private void resetFilter() {
        this.isFilterActive = false;
        this.filteredItem = ItemStack.EMPTY;
        this.filteredSlot = -1;
        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);
        if (recipeListPanel != null) recipeListPanel.updateRecipes(availableRecipes);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) return super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    private void resetAllFilters() {
        isFilterActive = false;
        filteredItem = ItemStack.EMPTY;
        filteredSlot = -1;
        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);
        if (recipeListPanel != null) recipeListPanel.updateRecipes(availableRecipes);
        currentRecipe = null;
        selectedRecipeIndex = -1;
        if (recipeListPanel != null) recipeListPanel.setSelectedIndex(-1);
        hasSelectedItem = false;
        selectedItemStack = ItemStack.EMPTY;
        selectedItemSlot = -1;
        selectedInventoryIndex = -1;
        if (searchField != null) searchField.setText("");
    }

    private boolean isSelectedItemDamaged() {
        if (!hasSelectedItem || selectedItemStack.isEmpty()) return false;
        if (!selectedItemStack.isDamageable()) return false;
        return selectedItemStack.getDamage() > 0;
    }

    private String getDamageWarning() {
        if (!hasSelectedItem || selectedItemStack.isEmpty()) return "";
        if (!selectedItemStack.isDamageable()) return "";
        int damage = selectedItemStack.getDamage();
        int maxDamage = selectedItemStack.getMaxDamage();
        int percent = (int) ((1.0 - (double) damage / maxDamage) * 100);
        return "§cCannot craft with damaged item! (" + percent + "% durability remaining)";
    }

    public void refreshDamagedItems() {
        damagedItems.clear();
        PlayerInventory inv = this.client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > 0) {
                damagedItems.add(new DamagedItem(stack, i));
            }
        }
        if (repairPanel != null) repairPanel.updateItems(damagedItems);
        if (currentRepairItem != null) {
            boolean stillDamaged = damagedItems.stream().anyMatch(d -> d.getSlotIndex() == currentRepairItem.getSlotIndex());
            if (!stillDamaged) {
                currentRepairItem = null;
                repairEnabled = false;
                if (repairPanel != null) repairPanel.updateItems(damagedItems);
            }
        }
    }

    private void createRepairPanel() {
        if (repairPanel != null) remove(repairPanel);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.inventoryX;
        int panelY = y + this.panelY - 10;
        int panelWidth = GRID_COLS * SLOT_STEP + 10;
        int panelHeight = GRID_ROWS * SLOT_STEP + 35;
        repairPanel = new RepairPanel(panelX, panelY, panelWidth, panelHeight, damagedItems,
                () -> onRepairItemSelected(repairPanel.getSelectedItem()));
        addDrawableChild(repairPanel);
        updatePanelVisibility();
    }

    private void onRepairItemSelected(DamagedItem item) {
        currentRepairItem = item;
        if (item == null) {
            repairEnabled = false;
            return;
        }
        int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
        repairEnabled = playerMoney >= item.getRepairCost();
    }

    private int getTotalRepairCost() {
        int total = 0;
        for (DamagedItem item : damagedItems) total += item.getRepairCost();
        return total;
    }

    // === НОВЫЙ ЭНУМ ДЛЯ ВКЛАДОК ===
    public enum TabType {
        TRADE, CRAFT, DISASSEMBLE, REPAIR
    }

    private void drawTradePanel(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int villagerX = x + this.tradePanelX;

        // === 1. Прогресс-бар уровня ===
        int level = handler.getProfessionLevel();
        int xpForNext = handler.getXpForNextLevel();
        int experience = handler.getProfessionExperience();
        float fractionalXp = handler.getClientFractionalXp();
        float totalXp = experience + fractionalXp;
        float progress = xpForNext > 0 ? Math.min(1.0f, totalXp / xpForNext) : 1.0f;
        float expectedXp = getExpectedXpForCurrentHover();

        if (progress < 1.0f && xpForNext > 0) {
            int barWidth = GRID_COLS * TRADE_SLOT_STEP;
            int barHeight = 4;
            int barX = villagerX;
            int barY = y + this.panelY - 25;

            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF303030);
            int filledWidth = (int) (barWidth * progress);
            context.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF40FF40);

            if (expectedXp > 0) {
                float progressGain = Math.min(1.0f - progress, expectedXp / xpForNext);
                int blueStartX = barX + filledWidth;
                int blueWidth = (int) (barWidth * progressGain);
                context.fill(blueStartX, barY, blueStartX + blueWidth, barY + barHeight, 0xFF00FFFF);
            }
            String expectedXpText = expectedXp > 0 ? String.format(" +%.2f", expectedXp) : "";
            Text xpText = Text.literal(String.format("%.2f", totalXp) + expectedXpText + " / " + xpForNext + " XP");
            context.drawText(this.textRenderer, xpText, barX, barY - 9, 0x808080, true);
        } else if (level >= 5) {
            Text maxLevelText = Text.translatable("tradeoverhaul.gui.maxlevel").formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD);
            context.drawText(this.textRenderer, maxLevelText, villagerX, y + this.panelY - 25, 0xFFAA00, true);
        }

        // === 2. Уровень и профессия ===
        String levelName = handler.getProfessionLevelName();
        Text levelTitle = switch (levelName) {
            case "apprentice" -> Text.translatable("tradeoverhaul.level.apprentice");
            case "journeyman" -> Text.translatable("tradeoverhaul.level.journeyman");
            case "expert" -> Text.translatable("tradeoverhaul.level.expert");
            case "master" -> Text.translatable("tradeoverhaul.level.master");
            default -> Text.translatable("tradeoverhaul.level.novice");
        };

        Text professionText = Text.empty();
        VillagerEntity villager = handler.getVillagerFromWorld(this.client);
        if (villager != null) {
            var prof = villager.getVillagerData().getProfession();
            if (prof != null && prof != net.minecraft.village.VillagerProfession.NONE) {
                var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(prof);
                if (profId != null) {
                    professionText = Text.literal(" (").append(Text.translatable("entity.minecraft.villager." + profId.getPath())).append(")");
                }
            }
        }

        Text levelText = Text.empty().append(levelTitle).append(professionText)
                .append(" (").append(Text.translatable("tradeoverhaul.gui.level")).append(" ").append(String.valueOf(level)).append(")");
        context.drawText(this.textRenderer, levelText, villagerX, y + this.panelY - 8, 0xFFFFFF, true);

        // === 3. Деньги жителя ===
        int wallet = handler.getSyncedWallet();
        var villagerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
        villagerCurrency.setTotalCopper(wallet);
        context.drawText(this.textRenderer, villagerCurrency.formatMoneyVertical(), villagerX, y + this.panelY + 5, 0xFFFFFF, true);

        // === 4. Подсветка доступных для покупки предметов ===
        //drawBuyableItemHighlights(context, x, y);
    }

    private void drawBuyableItemHighlights(DrawContext context, int x, int y) {
//        int villagerMoney = handler.getSyncedWallet();
//
//        for (int row = 0; row < 6; row++) {
//            for (int col = 0; col < 6; col++) {
//                int gridIndex = row * 6 + col;
//                int slotIndex = VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT + gridIndex;
//                Slot slot = this.handler.getSlot(slotIndex);
//                if (slot != null && slot.hasStack()) {
//                    ItemStack stack = slot.getStack();
//                    if (handler.canVillagerBuyItem(stack)) {
//                        int sellPrice = handler.getClientSellPrice(stack);
//                        int pricePerItem = sellPrice <= 0 ? 1 : sellPrice;
//                        int borderColor = villagerMoney >= pricePerItem ? SLOT_BORDER_CAN_AFFORD : SLOT_BORDER_CANNOT_AFFORD;
//
//                        int slotX = x + this.tradePanelX + col * TRADE_SLOT_STEP;
//                        int slotY = y + this.panelY + row * TRADE_SLOT_STEP - 1;
//
//                        //context.fill(slotX, slotY, slotX + TRADE_SLOT_SIZE - 1, slotY + 1, borderColor);
//                        context.fill(slotX + 1, slotY + TRADE_SLOT_SIZE - 2, slotX + TRADE_SLOT_SIZE - 1, slotY + TRADE_SLOT_SIZE - 1, borderColor);
//                        //context.fill(slotX, slotY + 1, slotX + 1, slotY + TRADE_SLOT_SIZE - 1, borderColor);
//                        context.fill(slotX + TRADE_SLOT_SIZE - 2, slotY + 1, slotX + TRADE_SLOT_SIZE - 1, slotY + TRADE_SLOT_SIZE - 1, borderColor);
//                    }
//                }
//            }
//        }
    }

    // === Расчёт ожидаемого XP для тултипа ===
    private float getExpectedXpForCurrentHover() {
        Slot focused = this.focusedSlot;
        if (focused == null || !focused.hasStack()) return 0f;

        int slotIndex = focused.id;
        ItemStack stack = focused.getStack();
        boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
        boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();

        if (slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
            int price = handler.getClientBuyPrice(slotIndex);
            if (price <= 0) return 0f;
            int wantToBuy = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
            int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
            int maxCanBuy = playerMoney / price;
            int quantity = Math.min(wantToBuy, maxCanBuy);
            int maxSpace = handler.clientHasInventorySpaceForStack(stack, wantToBuy) ? wantToBuy : 0;
            if (maxSpace < quantity) quantity = maxSpace;
            if (quantity <= 0) return 0f;
            return handler.getExpectedXpForBuy(slotIndex, quantity);
        } else if (slotIndex >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX && slotIndex < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
            int wantToSell = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
            int price = handler.getClientSellPrice(stack);
            if (price <= 0) return 0f;
            int villagerMoney = handler.getSyncedWallet();
            int maxCanBuy = villagerMoney / price;
            int quantity = Math.min(wantToSell, maxCanBuy);
            if (quantity <= 0) quantity = 1;
            return handler.getExpectedXpForSell(stack, quantity);
        }
        return 0f;
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            ItemStack stack = this.focusedSlot.getStack();
            int slotIndex = this.focusedSlot.id;
            boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
            boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();
            List<Text> tooltip = this.getTooltipFromItem(this.client, stack);

            // === Слоты торговли (покупка у жителя) ===
            if (slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                int price = handler.getClientBuyPrice(slotIndex);
                if (price > 0) {
                    int pricePerItem = price;
                    int wantToBuy = shift ? stack.getCount() : (ctrl ? Math.min(10, stack.getCount()) : 1);
                    int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player);
                    int maxCanBuy = playerMoney / pricePerItem;
                    int quantity = Math.min(wantToBuy, maxCanBuy);
                    boolean hasInventorySpace = handler.clientHasInventorySpaceForStack(stack, quantity);
                    int maxSpace = hasInventorySpace ? wantToBuy : 0;
                    if (maxSpace < quantity) quantity = maxSpace;
                    boolean canFitAtLeastOne = handler.clientHasInventorySpaceForStack(stack, 1);
                    boolean canAffordOne = playerMoney >= pricePerItem;

                    int displayQuantity, totalCost;
                    String quantityText;
                    net.minecraft.util.Formatting textColor;

                    if (!canFitAtLeastOne) {
                        tooltip.add(Text.translatable("gui.tradeoverhaul.no_inventory_space").formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.ITALIC));
                        displayQuantity = 1; quantityText = " (1)"; totalCost = pricePerItem; textColor = net.minecraft.util.Formatting.RED;
                    } else if (quantity <= 0) {
                        displayQuantity = 1; quantityText = " (1)"; totalCost = pricePerItem; textColor = net.minecraft.util.Formatting.RED;
                    } else {
                        displayQuantity = quantity; quantityText = " (" + displayQuantity + ")"; totalCost = quantity * pricePerItem;
                        textColor = canAffordOne ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED;
                    }

                    if (stack.isDamageable() && stack.getDamage() > 0) {
                        int maxDamage = stack.getMaxDamage();
                        int currentDamage = stack.getDamage();
                        int durabilityPercent = (int) Math.round(((double) (maxDamage - currentDamage) / maxDamage) * 100.0);
                        tooltip.add(Text.translatable("gui.tradeoverhaul.durability", durabilityPercent).formatted(net.minecraft.util.Formatting.GRAY));
                    }

                    if (stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK) {
                        var nbt = stack.getNbt();
                        if (nbt != null && nbt.contains("StoredEnchantments", 10)) {
                            var enchantList = nbt.getList("StoredEnchantments", 10);
                            if (!enchantList.isEmpty()) {
                                var enchantNbt = enchantList.getCompound(0);
                                String enchantId = enchantNbt.getString("id");
                                int enchantLevel = enchantNbt.getShort("lvl");
                                net.minecraft.util.Identifier enchantIdentifier = net.minecraft.util.Identifier.tryParse(enchantId);
                                if (enchantIdentifier != null) {
                                    var enchant = net.minecraft.registry.Registries.ENCHANTMENT.get(enchantIdentifier);
                                    if (enchant != null) {
                                        String enchantName = enchant.getName(enchantLevel).getString();
                                        tooltip.add(Text.literal("§7" + enchantName));
                                    }
                                }
                            }
                        }
                    }
                    tooltip.add(Text.translatable("gui.tradeoverhaul.buy", quantityText, com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.formatMoney(totalCost)).formatted(textColor));
                }
            }
            // === Слоты игрока (продажа жителю) ===
            else if (slotIndex >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX && slotIndex < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
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
                    net.minecraft.util.Formatting textColor = canAffordOne ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED;

                    if (stack.isDamageable() && stack.getDamage() > 0) {
                        int maxDamage = stack.getMaxDamage();
                        int currentDamage = stack.getDamage();
                        int durabilityPercent = (int) Math.round(((double) (maxDamage - currentDamage) / maxDamage) * 100.0);
                        tooltip.add(Text.translatable("gui.tradeoverhaul.durability", durabilityPercent).formatted(net.minecraft.util.Formatting.GRAY));
                    }
                    tooltip.add(Text.translatable("gui.tradeoverhaul.sell", quantityText, com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.formatMoney(totalEarned)).formatted(textColor));
                }
            }
            context.drawTooltip(this.textRenderer, tooltip, x, y);
            return;
        }
        super.drawMouseoverTooltip(context, x, y);
    }

    // === НОВЫЕ МЕТОДЫ: Визуальная индикация торговли ===

    /**
     * Рисует красный полупрозрачный квадрат поверх слота (индикация "не хватает денег")
     */
    private void drawInsufficientFundsOverlay(DrawContext context, int x, int y, Slot slot) {
        int slotX = x + slot.x;
        int slotY = y + slot.y;
        int size = 18;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 0xB3 = ~70% непрозрачности. Красный квадрат будет хорошо читаться поверх предмета
        context.fill(slotX, slotY, slotX + size, slotY + size, 0xB3FF0000);
        RenderSystem.disableBlend();
    }

    /**
     * Рисует затемнение поверх слота (индикация "предмет нельзя продать этому жителю")
     */
    private void drawDimmedSlotOverlay(DrawContext context, int x, int y, Slot slot) {
        int slotX = x + slot.x;
        int slotY = y + slot.y;
        int size = 18;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 0xF5 = ~96% непрозрачности. Чёрный слой поверх текстуры чётко затемнит и иконку, и цифру
        context.fill(slotX, slotY, slotX + size, slotY + size, 0xF5000000);
        RenderSystem.disableBlend();
    }

    /**
     * Отрисовывает визуальные индикаторы для всех слотов на вкладке Trade
     */
    private void drawTradeSlotIndicators(DrawContext context, int x, int y) {
        if (currentTab != TabType.TRADE) return;

        int villagerMoney = handler.getSyncedWallet();
        int playerMoney = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(client.player);

        for (Slot slot : handler.slots) {
            // Пропускаем скрытые/пустые слоты
            if (slot.x < 0 || slot.y < 0) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            int slotIndex = slot.id;

            // === СЛОТЫ ЖИТЕЛЯ (Игрок покупает) ===
            if (slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                int price = handler.getClientBuyPrice(slotIndex);
                if (price > 0 && playerMoney < price) {
                    drawInsufficientFundsOverlay(context, x, y, slot);
                }
            }
            // === СЛОТЫ ИГРОКА + БРОНЯ (Игрок продаёт) ===
            else {
                // Проверяем, разрешён ли предмет для продажи в конфиге
                if (!handler.canVillagerBuyItem(stack)) {
                    drawDimmedSlotOverlay(context, x, y, slot);
                } else {
                    // Предмет разрешён, но проверяем баланс жителя
                    int sellPrice = handler.getClientSellPrice(stack);
                    if (sellPrice > 0 && villagerMoney < sellPrice) {
                        drawInsufficientFundsOverlay(context, x, y, slot);
                    }
                }
            }
        }
    }
}