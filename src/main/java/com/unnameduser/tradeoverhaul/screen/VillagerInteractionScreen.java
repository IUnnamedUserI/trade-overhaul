package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreen;
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

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler;

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
    private RecipeListPanel recipeListPanel;
    private List<CraftRecipe> availableRecipes = new ArrayList<>();
    private CraftRecipe currentRecipe;
    private int selectedRecipeIndex = -1;

    // Поле поиска
    private TextFieldWidget searchField;
    private List<CraftRecipe> allRecipes = new ArrayList<>();

    // Панель торговли
    private VillagerTradeScreenHandler tradeHandler;
    private VillagerTradeScreen tradeScreen;

    // Фильтрация
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
        initTradeScreen();

        refreshDamagedItems();
        createRepairPanel();
    }

    private void createSearchField() {
        if (searchField != null) {
            remove(searchField);
        }

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

        for (int i = 0; i < this.handler.slots.size(); i++) {
            var slotObj = this.handler.slots.get(i);

            // Для вкладки REPAIR перемещаем слоты за пределы экрана
            if (currentTab == TabType.REPAIR) {
                slotObj.x = -1000;
                slotObj.y = -1000;
                continue;
            }

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
        if (recipeListPanel != null) {
            remove(recipeListPanel);
        }

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + this.recipesX - 30;
        int panelY = y + this.panelY - 30;

        int listWidth = recipesPanelWidth - 4;
        int listHeight = recipesPanelHeight - 42;

        recipeListPanel = new RecipeListPanel(this, panelX + 2, panelY + 40, listWidth, listHeight, availableRecipes,
                () -> onRecipeSelected(recipeListPanel.getSelectedRecipe())
        );

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
                    System.out.println("[TradeOverhaul] Auto-selected filtered item as unique for recipe: " + recipe.getId());
                    return;
                }
            }
        }

        this.hasSelectedItem = false;
        this.selectedItemStack = ItemStack.EMPTY;
        this.selectedItemSlot = -1;
        this.selectedInventoryIndex = -1;
        System.out.println("[TradeOverhaul] Selected recipe: " + recipe.getId());
    }

    private void onTabSelected(TabType tab) {
        if (currentTab == tab) return;
        currentTab = tab;

        // Перепозиционируем слоты при смене вкладки
        positionSlots();

        if (tab == TabType.TRADE && tradeScreen == null) {
            initTradeScreen();
        }

        if (tab == TabType.REPAIR) {
            refreshDamagedItems();
            if (repairPanel != null) {
                repairPanel.updateItems(damagedItems);
            }
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

        if (recipeListPanel != null) {
            recipeListPanel.setVisible(showRecipesPanel);
        }
        if (searchField != null) {
            searchField.setVisible(showRecipesPanel);
        }
        if (repairPanel != null) {
            repairPanel.setVisible(showRepairPanel);
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

        boolean itemNotDamaged = true;
        if (uniqueIndex >= 0 && hasSelectedItem && !selectedItemStack.isEmpty()) {
            itemNotDamaged = !selectedItemStack.isDamageable() || selectedItemStack.getDamage() == 0;
        }

        boolean canCraft = hasAllIngredients && (uniqueIndex < 0 || itemNotDamaged);
        craftEnabled = canCraft;

        int buttonY;
        if (uniqueIndex >= 0) {
            buttonY = resultY + 50;
            context.drawText(this.textRenderer, Text.literal("Selected Item:"), startX, resultY + 28, 0xCCCCCC, false);
            if (hasSelectedItem && !selectedItemStack.isEmpty()) {
                String selectedName = selectedItemStack.getItem().getName().getString();
                String selectedText = selectedName + " x" + selectedItemStack.getCount();
                if (!itemNotDamaged) {
                    selectedText += " §c(Damaged!)";
                }
                context.drawText(this.textRenderer, Text.literal(selectedText), startX + 5, resultY + 40, hasUniqueSelected ? 0x55FF55 : 0xFF6666, false);
            } else {
                context.drawText(this.textRenderer, Text.literal("None (RMB click on item)"), startX + 5, resultY + 40, 0xFF6666, false);
            }
        } else {
            buttonY = resultY + 40;
        }

        int buttonX = startX;
        context.fill(buttonX, buttonY, buttonX + 60, buttonY + 20, canCraft ? 0xFF444444 : 0xFF333333);
        context.fill(buttonX + 1, buttonY + 1, buttonX + 59, buttonY + 19, canCraft ? 0xFF666666 : 0xFF444444);
        context.drawText(this.textRenderer, Text.literal("Craft"), buttonX + 15, buttonY + 6, canCraft ? 0xFFFFFF : 0x888888, false);

        craftButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, buttonY, 60, 20);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (currentTab == TabType.TRADE) {
            if (tradeScreen != null) {
                tradeScreen.render(context, mouseX, mouseY, delta);
            }
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

            // Тултип для кнопки Craft
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
            // Пока пусто
            this.renderBackground(context);
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);
        } else if (currentTab == TabType.REPAIR) {
            this.renderBackground(context);

            int x = (this.width - this.backgroundWidth) / 2;
            int y = (this.height - this.backgroundHeight) / 2;

            // Отрисовка панели повреждённых предметов
            if (repairPanel != null) {
                repairPanel.render(context, mouseX, mouseY, delta);
            }

            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

            // Правая панель информации
            int rightPanelX = x + this.inventoryX + (GRID_COLS * SLOT_STEP) + 120;
            int rightPanelY = y + this.panelY - 20;
            int rightPanelWidth = 180;
            int rightPanelHeight = 200;

            context.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + rightPanelHeight, 0xCC000000);
            context.fill(rightPanelX + 1, rightPanelY + 1, rightPanelX + rightPanelWidth - 1, rightPanelY + rightPanelHeight - 1, 0xCC333333);
            context.drawBorder(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight, 0xFF666666);

            // Кнопки между панелями
            int buttonX = rightPanelX - 85;
            int buttonY = rightPanelY + 70;
            int buttonWidth = 60;
            int buttonHeight = 20;

            // Кнопка Repair
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, repairEnabled ? 0xFF444444 : 0xFF333333);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1, repairEnabled ? 0xFF666666 : 0xFF444444);
            context.drawText(this.textRenderer, Text.literal("Repair"), buttonX + (buttonWidth / 2) - (this.textRenderer.getWidth("Repair") / 2), buttonY + 6, repairEnabled ? 0xFFFFFF : 0x888888, false);
            repairButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, buttonY, buttonWidth, buttonHeight);

            // Кнопка Repair All
            int repairAllButtonY = buttonY + buttonHeight + 5;
            int totalCost = getTotalRepairCost();
            repairAllEnabled = NumismaticHelper.getTotalMoney(this.client.player) >= totalCost && !damagedItems.isEmpty();

            context.fill(buttonX, repairAllButtonY, buttonX + buttonWidth, repairAllButtonY + buttonHeight, repairAllEnabled ? 0xFF444444 : 0xFF333333);
            context.fill(buttonX + 1, repairAllButtonY + 1, buttonX + buttonWidth - 1, repairAllButtonY + buttonHeight - 1, repairAllEnabled ? 0xFF666666 : 0xFF444444);
            context.drawText(this.textRenderer, Text.literal("Repair All"), buttonX + (buttonWidth / 2) - (this.textRenderer.getWidth("Repair All") / 2), repairAllButtonY + 6, repairAllEnabled ? 0xFFFFFF : 0x888888, false);
            repairAllButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, repairAllButtonY, buttonWidth, buttonHeight);

            // Информация о выбранном предмете
            if (repairPanel != null && repairPanel.getSelectedItem() != null) {
                DamagedItem selected = repairPanel.getSelectedItem();
                int startX = rightPanelX + rightPanelWidth / 2;
                int startY = rightPanelY + 10;

                // Увеличенная иконка (32x32)
                context.getMatrices().push();
                context.getMatrices().scale(2.0f, 2.0f, 1.0f);
                context.drawItem(selected.getStack(), (startX) / 2 - 8, (startY) / 2 + 10);
                context.getMatrices().pop();

                String itemName = selected.getStack().getName().getString();
                int itemNameWidth = this.textRenderer.getWidth(itemName);
                int centerX = startX;  // ← здесь укажи нужную X координату центра
                int centeredX = centerX - (itemNameWidth / 2);
                context.drawText(this.textRenderer, itemName, centeredX, startY, 0xFFFFFF, false);

                int durabilityPercent = (int) ((1.0 - (double) selected.getCurrentDamage() / selected.getMaxDamage()) * 100);
                String durabilityText = String.format("Durability", durabilityPercent);
                context.drawText(this.textRenderer, Text.literal(durabilityText), centerX - (this.textRenderer.getWidth(durabilityText) / 2), startY + 55, 0xCCCCCC, false);
                String durabilityPercentText = String.format("%d%%", durabilityPercent);
                context.drawText(this.textRenderer, Text.literal(durabilityPercentText), centerX - (this.textRenderer.getWidth(durabilityPercentText) / 2), startY + 65, 0xCCCCCC, false);

                String costText = String.format("Cost: %d copper", selected.getCurrentDamage() * 2);
                context.drawText(this.textRenderer, Text.literal(costText), centerX - (this.textRenderer.getWidth(costText) / 2), startY + 85, 0xFFFFAA, false);

                // Тултип для Repair
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

            // Тултип для Repair All
            if (!repairAllEnabled && repairAllButtonBounds != null && !damagedItems.isEmpty() &&
                    mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                    mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal("§cNeed " + totalCost + " copper to repair all"));
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }

            // Деньги игрока
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

        // Рисуем слоты только для CRAFT и TRADE (не для REPAIR)
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

        // Подсветка выбранного фильтрованного предмета
        if (isFilterActive && filteredSlot >= 0) {
            for (int i = 0; i < this.handler.slots.size(); i++) {
                var slotObj = this.handler.slots.get(i);
                if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                    continue;
                }
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
        if (recipeListPanel != null) {
            remove(recipeListPanel);
        }
        if (searchField != null) {
            remove(searchField);
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
        // ЛКМ (button == 0)
        if (button == 0) {
            // === ВКЛАДКА CRAFT: обработка поля поиска ===
            if (currentTab == TabType.CRAFT) {
                if (searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight()) {
                    searchField.setFocused(true);
                    return searchField.mouseClicked(mouseX, mouseY, button);
                } else if (searchField != null) {
                    searchField.setFocused(false);
                }
            }

            // === ВКЛАДКА CRAFT: кнопка Craft ===
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

            // === ВКЛАДКА REPAIR: кнопка Repair ===
            if (currentTab == TabType.REPAIR && repairPanel != null && repairPanel.getSelectedItem() != null &&
                    repairButtonBounds != null && mouseX >= repairButtonBounds.getX() && mouseX <= repairButtonBounds.getX() + repairButtonBounds.getWidth() &&
                    mouseY >= repairButtonBounds.getY() && mouseY <= repairButtonBounds.getY() + repairButtonBounds.getHeight()) {

                if (!repairEnabled) {
                    return true;
                }

                DamagedItem selected = repairPanel.getSelectedItem();
                RepairRequestC2SPacket packet = new RepairRequestC2SPacket(handler.syncId, selected.getSlotIndex());
                PacketByteBuf buf = PacketByteBufs.create();
                RepairRequestC2SPacket.encode(packet, buf);
                ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "repair_request"), buf);

                // НЕ вызываем refreshDamagedItems() здесь — ждём ответ от сервера

                return true;
            }

// === ВКЛАДКА REPAIR: кнопка Repair All ===
            if (currentTab == TabType.REPAIR && repairAllButtonBounds != null && !damagedItems.isEmpty() &&
                    mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                    mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {

                if (!repairAllEnabled) {
                    return true;
                }

                RepairAllRequestC2SPacket packet = new RepairAllRequestC2SPacket(handler.syncId);
                PacketByteBuf buf = PacketByteBufs.create();
                RepairAllRequestC2SPacket.encode(packet, buf);
                ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "repair_all_request"), buf);

                // НЕ вызываем refreshDamagedItems() здесь — ждём ответ от сервера

                return true;
            }

            // === СБРОС ФИЛЬТРОВ (для CRAFT) ===
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

            // === ВКЛАДКА REPAIR: обновление списка при клике на предмет в инвентаре (для мгновенного появления в списке) ===
            // Этот блок позволяет обновить список повреждённых предметов при клике ЛКМ по инвентарю
            if (currentTab == TabType.REPAIR) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null) {
                    // Задержка для обновления списка (чтобы предмет успел переместиться)
                    this.client.execute(() -> {
                        refreshDamagedItems();
                        if (repairPanel != null) {
                            repairPanel.updateItems(damagedItems);
                            // Если выбранный предмет был починен и исчез, сбрасываем выбор
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

            // Передаём дальше для обычной обработки ЛКМ (перетаскивание предметов)
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // === ПКМ (button == 1) ===
        if (button == 1) {
            // Только для вкладки CRAFT
            if (currentTab == TabType.CRAFT) {
                Slot hoveredSlot = getSlotAt(mouseX, mouseY);
                if (hoveredSlot != null) {
                    int slotIndex = hoveredSlot.id;
                    if (slotIndex >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                        ItemStack stack = hoveredSlot.getStack();
                        if (!stack.isEmpty()) {
                            // Если есть выбранный рецепт и предмет подходит как уникальный
                            if (currentRecipe != null && currentRecipe.getUniqueIngredientIndex() >= 0) {
                                int uniqueIdx = currentRecipe.getUniqueIngredientIndex();
                                if (uniqueIdx < currentRecipe.getIngredients().size()) {
                                    Ingredient uniqueIng = currentRecipe.getIngredients().get(uniqueIdx);
                                    if (ItemStack.areItemsEqual(stack, uniqueIng.getItem())) {
                                        this.selectedItemSlot = slotIndex;
                                        this.selectedItemStack = stack.copy();
                                        this.hasSelectedItem = true;
                                        this.selectedInventoryIndex = getRealInventoryIndex(slotIndex);
                                        System.out.println("[TradeOverhaul] Selected unique item for craft: " + stack.getItem().getName().getString());
                                        return true;
                                    }
                                }
                            }
                            // Иначе — фильтруем рецепты
                            filterRecipesByItem(stack, slotIndex);
                            return true;
                        }
                    }
                }
                return true;
            }

            // Для вкладки REPAIR: ПКМ выбирает предмет в списке? Нет, пока не нужно
            return true;
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
        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
        }
        currentRecipe = null;
        selectedRecipeIndex = -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == 69) {
                return true;
            }
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            if (searchField.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public VillagerCraftingScreenHandler getCraftingHandler() {
        return this.handler;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (currentTab == TabType.TRADE) {
            return tradeScreen.mouseScrolled(mouseX, mouseY, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (tradeScreen != null) {
            tradeScreen.resize(client, width, height);
        }
    }

    private void initTradeScreen() {
        if (tradeScreen != null) return;

        VillagerEntity villager = handler.getVillagerFromWorld(client);
        if (villager != null) {
            tradeHandler = new VillagerTradeScreenHandler(handler.syncId, this.client.player.getInventory(), villager);
            tradeScreen = new VillagerTradeScreen(tradeHandler, this.client.player.getInventory(), this.title);
        } else {
            System.err.println("[TradeOverhaul] Could not find villager with ID " + handler.getVillagerEntityId());
        }
    }

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

        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
        }

        currentRecipe = null;
        selectedRecipeIndex = -1;
        hasSelectedItem = false;
        selectedItemStack = ItemStack.EMPTY;
        selectedItemSlot = -1;
        selectedInventoryIndex = -1;

        // Если после фильтрации остался 1 рецепт, выбираем его
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
                    System.out.println("[TradeOverhaul] Auto-selected item as unique for recipe: " + currentRecipe.getId());
                }
            }
        }

        System.out.println("[TradeOverhaul] Filtered recipes by item: " + item.getItem().getName().getString() + " (" + availableRecipes.size() + " recipes)");
    }

    private void resetFilter() {
        this.isFilterActive = false;
        this.filteredItem = ItemStack.EMPTY;
        this.filteredSlot = -1;

        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);

        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
        }

        System.out.println("[TradeOverhaul] Filter reset");
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return true;
    }

    private void resetAllFilters() {
        isFilterActive = false;
        filteredItem = ItemStack.EMPTY;
        filteredSlot = -1;

        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);
        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
        }

        currentRecipe = null;
        selectedRecipeIndex = -1;
        if (recipeListPanel != null) {
            recipeListPanel.setSelectedIndex(-1);
        }

        hasSelectedItem = false;
        selectedItemStack = ItemStack.EMPTY;
        selectedItemSlot = -1;
        selectedInventoryIndex = -1;

        if (searchField != null) {
            searchField.setText("");
        }

        System.out.println("[TradeOverhaul] All filters reset");
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

        // Принудительно обновляем панель
        if (repairPanel != null) {
            repairPanel.updateItems(damagedItems);
        }

        // Сбрасываем выбранный предмет, если он был починен
        if (currentRepairItem != null) {
            boolean stillDamaged = damagedItems.stream().anyMatch(d -> d.getSlotIndex() == currentRepairItem.getSlotIndex());
            if (!stillDamaged) {
                currentRepairItem = null;
                repairEnabled = false;
                if (repairPanel != null) {
                    repairPanel.updateItems(damagedItems);
                }
            }
        }
    }

    private void createRepairPanel() {
        if (repairPanel != null) {
            remove(repairPanel);
        }

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Левая панель с повреждёнными предметами (на месте инвентаря)
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
        for (DamagedItem item : damagedItems) {
            total += item.getRepairCost();
        }
        return total;
    }
}