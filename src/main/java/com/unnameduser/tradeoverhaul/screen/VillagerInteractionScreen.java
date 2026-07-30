package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.network.RepairAllRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.network.RepairRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.network.TradeRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import com.unnameduser.tradeoverhaul.common.RecipeManager;
import com.unnameduser.tradeoverhaul.common.config.DisassemblyConfig;
import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.DamagedItem;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class VillagerInteractionScreen extends HandledScreen<VillagerCraftingScreenHandler> {

    private int selectedInventoryIndex = -1;
    private int selectedItemSlot = -1;
    private ItemStack selectedItemStack = ItemStack.EMPTY;
    private boolean hasSelectedItem = false;
    private boolean craftEnabled = false;

    private static final int ARMOR_SLOT_WIDTH = 18;
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 6;

    private int armorX;
    private int inventoryX;
    private int inventoryY;
    private int recipesX;
    private int panelY;
    private int titleY;

    private Text playerInventoryLabel;
    private TabType currentTab = TabType.TRADE;

    private int recipesPanelWidth = 150;
    private int recipesPanelHeight = 200;
    private boolean showRecipesPanel = true;

    private RecipeListPanel recipeListPanel;
    private List<CraftRecipe> availableRecipes = new ArrayList<>();
    private CraftRecipe currentRecipe;
    private int selectedRecipeIndex = -1;

    private TextFieldWidget searchField;
    private List<CraftRecipe> allRecipes = new ArrayList<>();

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

    private boolean renderSlots = true;
    private int tradePanelX;
    private int tradePanelY;

    // Дисасембли
    private ItemStack disassemblyTarget = ItemStack.EMPTY;
    private List<ItemStack> disassemblyComponents = new ArrayList<>();
    private int disassemblyCost = 0;
    private boolean isDisassemblyActive = false;
    private int disassemblyTargetSlotIndex = -1;
    private net.minecraft.client.util.math.Rect2i disassembleButtonBounds;

    private static final Identifier PANEL_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_background.png");
    private static final Identifier SLOT_TEX  = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_slot.png");
    private static final Identifier BUTTON_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_button.png");
    private static final Identifier ITEM_BG_TEX  = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_item.png");
    private static final Identifier INV_BG_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/inventory_background.png");
    private static final Identifier ACTIVE_SLOT_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/active_slot.png");
    private static final Identifier LOCKED_SLOT_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/locked_slot.png");

    private static final int PANEL_TEX_W = 125;
    private static final int PANEL_TEX_H = 190;
    private static final int SLOT_TEX_W  = 18;
    private static final int SLOT_TEX_H  = 18;
    private static final int BUTTON_TEX_W = 95;
    private static final int BUTTON_TEX_H = 60;
    private static final int BTN_STATE_H  = 20; // Высота одного состояния
    private static final int ITEM_BG_W = 48; // Ширина фона предмета
    private static final int ITEM_BG_H = 48; // Высота фона предмета

    private static final int INV_BG_W = 129; // 129
    private static final int INV_BG_H = 190; // 190

    private static final int INV_SLOT_START_X = 8;
    private static final int INV_SLOT_START_Y = 20;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 1;
    private static final int SLOT_STEP = SLOT_SIZE + SLOT_GAP; // 19
    private static final int ARMOR_Y_OFFSET = 4; // Отступ от низа сетки до брони

    private static final int INV_LABEL_X = 8;
    private static final int INV_LABEL_Y = 8;

    private static final int STATE_SLOT_W = 18;
    private static final int STATE_SLOT_H = 18;

    private boolean isDisassembleButtonPressed = false;

    private static final Identifier NUM_GOLD = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/gold_coin.png");
    private static final Identifier NUM_SILVER = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/silver_coin.png");
    private static final Identifier NUM_COPPER = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/bronze_coin.png");

    // Craft panel constants
    private static final Identifier CRAFT_PANEL_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/craft_background.png");
    private static final Identifier REPAIR_RIGHT_PANEL_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/repair_right_panel.png");
    private static final Identifier DISASSEMBLE_PANEL_EMPTY_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/disassemble_empty_background.png");

    private static final int CRAFT_PANEL_W = 166;
    private static final int CRAFT_PANEL_H = 191;
    private static final int CRAFT_ITEM_BG_SIZE = 48;
    private static final int CRAFT_SLOT_SIZE = 24;
    private static final int CRAFT_SLOT_GAP = 3;
    private static final int CRAFT_MAX_PER_ROW = 3;

    private net.minecraft.client.util.math.Rect2i craftPanelBounds;
    private final List<CraftBuySlotInfo> craftBuyableSlots = new ArrayList<>();

    private List<String> serverRecipeIds = new ArrayList<>();
    private boolean recipesReceivedFromServer = false;

    private static final int REPAIR_RIGHT_PANEL_W = 180;
    private static final int REPAIR_RIGHT_PANEL_H = 190;

    // === Кнопки ремонта ===
    private static final Identifier REPAIR_BUTTON_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/repair_button.png");
    private static final int REPAIR_BTN_W = 60;   // Ширина кнопки
    private static final int REPAIR_BTN_H = 20;   // Высота одного состояния
    private static final int REPAIR_BTN_TEX_H = 40; // Общая высота текстуры (2 состояния по 20px)
    private static final float REPAIR_BTN_TEXT_SCALE = 0.8f; // Масштаб текста на кнопке

    private int navY;
    private int navTitleX;
    private List<TabType> tabNavigationOrder;
    private int currentTabIndex;
    private net.minecraft.client.util.math.Rect2i navLeftBounds;
    private net.minecraft.client.util.math.Rect2i navRightBounds;
    private List<net.minecraft.client.util.math.Rect2i> navDotBounds;
    private String leftTabName;
    private String rightTabName;

    private static final int NAV_DOT_GAP = 4;
    private static final int NAV_TOP_OFFSET = 10;
    private static final int NAV_ARROW_X_OFFSET = 60; // Фиксированное смещение стрелок от центра (настраиваемое)
    private static final float NAV_SIDE_TEXT_SCALE = 0.6f;
    private static final int NAV_SIDE_TEXT_OFFSET = -10; // Смещение текста от стрелки (настраиваемое)
    private static final int NAV_DOTS_Y_OFFSET = -9;
    private static final int NAV_TEXT_VERTICAL_OFFSET = 0;
    private static final int NAV_TITLE_GAP = 16;

    private static final Identifier NAV_ARROW_LEFT_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/nav_arrow_left.png");
    private static final Identifier NAV_ARROW_RIGHT_TEX = new Identifier(TradeOverhaulMod.MOD_ID, "textures/gui/nav_arrow_right.png");
    private static final int NAV_ARROW_W = 20;  // Ширина стрелки
    private static final int NAV_ARROW_H = 20;  // Высота одного состояния
    private static final int NAV_ARROW_TEX_H = 10;

    private int slotSize;
    private int slotStep;
    private int invBgW;
    private int invBgH;
    private int invSlotStartX;
    private int invSlotStartY;
    private int invLabelX;
    private int invLabelY;

    private int playerPanelX; // Левая панель (игрок)
    private int playerPanelY;
    private int villagerPanelX; // Правая панель (житель)
    private int villagerPanelY;

    private int centerX;
    private int centerY;

    private static final int BASE_INV_BG_W = 129;
    private static final int BASE_INV_BG_H = 190;
    private static final int BASE_GRID_COLS = 6;
    private static final int BASE_GRID_ROWS = 6;
    private static final int BASE_SLOT_SIZE = 18;
    private static final int BASE_SLOT_GAP = 1;
    private static final int BASE_SLOT_STEP = BASE_SLOT_SIZE + BASE_SLOT_GAP;
    private static final int BASE_INV_SLOT_START_X = 8;
    private static final int BASE_INV_SLOT_START_Y = 20;
    private static final int BASE_INV_LABEL_X = 8;
    private static final int BASE_INV_LABEL_Y = 8;

    private float scaleFactor = 1.0f;

    private int panelWidth;
    private int panelHeight;
    private int gapBetweenPanels;

    private static final int BASE_PANEL_WIDTH = 129;
    private static final int BASE_PANEL_HEIGHT = 190;
    private static final int BASE_GAP_BETWEEN_PANELS = 200;

    // Размеры для разных GUI scale
    private static final int PANEL_WIDTH_GUI2 = 86;   // 129 * 2/3
    private static final int PANEL_HEIGHT_GUI2 = 127; // 190 * 2/3
    private static final int PANEL_WIDTH_GUI3 = 129;  // Базовый
    private static final int PANEL_HEIGHT_GUI3 = 190; // Базовый
    private static final int PANEL_WIDTH_GUI4 = 172;  // 129 * 4/3
    private static final int PANEL_HEIGHT_GUI4 = 253; // 190 * 4/3

    // В разделе с базовыми константами (после BASE_INV_LABEL_Y)
    private static final int BASE_ARMOR_Y_OFFSET = 4; // Отступ от низа сетки до брони

    private int currentGuiScale = 3;

    private TradePanel tradePanel;
    private boolean hasSelectedTradeSlot = false;
    private int selectedTradeSlotIndex = -1;
    private float expectedXp = 0f;

    private int tradeBarX;
    private int tradeBarY;
    private int tradeBarWidth;
    private int tradeBarHeight;
    private int professionTextX;
    private int professionTextY;

    private static record CraftBuySlotInfo(net.minecraft.client.util.math.Rect2i bounds, ItemStack stack, int price) {}

    public VillagerInteractionScreen(VillagerCraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        this.backgroundWidth = (int) (this.client.getWindow().getScaledWidth() * 0.85);
        this.backgroundHeight = (int) (this.client.getWindow().getScaledHeight() * 0.80);

        super.init();
        calculateScaleFactor();
        calculatePanelPositions();
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

        this.armorX = startX - 20;
        this.inventoryX = x + 30;
        this.inventoryY = (this.height - INV_BG_H) / 2;
        this.centerX = inventoryX + inventoryWidth + gapInventoryCenter;
        this.recipesX = centerX + centerWidth + gapCenterRecipes;

        this.panelY = y + 35;
        this.titleY = this.panelY - 20;

        int availableHeight = this.backgroundHeight - this.panelY - 50;
        int maxVisible = Math.min(8, availableHeight / 24);
        recipesPanelHeight = maxVisible * 24 + 55;

        this.tradePanelX = this.inventoryX + INV_BG_W + 220; // 50
        this.tradePanelY = inventoryY;

        positionSlots();
        this.playerInventoryTitleX = inventoryX;
        this.playerInventoryTitleY = titleY + 5;
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;

        loadRecipes();
        createRecipeList();
        createSearchField();

        refreshDamagedItems();
        createRepairPanel();

        setupNavigation();

        tradePanel = new TradePanel(this.textRenderer, new TradePanelCallback() {
            @Override
            public void onTradeConfirm(int slotIndex, int amount, boolean buying) {
                sendTradeRequest(slotIndex, amount, buying);
            }

            @Override
            public void onTradeCancel() {
                hasSelectedTradeSlot = false;
                selectedTradeSlotIndex = -1;
                expectedXp = 0f;
                updateExpectedXp();
            }

            @Override
            public void onAmountChanged(int newAmount, int totalPrice) {
                updateExpectedXp();
            }

            @Override
            public int getVillagerMoney() {
                return handler.getSyncedWallet();
            }
        });
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
        int bgX = (this.width - this.backgroundWidth) / 2;
        int bgY = (this.height - this.backgroundHeight) / 2;

        // Вычисляем масштабированные размеры для слотов с учётом GUI scale
        float scaleFactor = currentGuiScale / 3.0f;
        int scaledSlotSize = (int) (BASE_SLOT_SIZE * scaleFactor);

        // ✅ РАЗНЫЙ ГАП ДЛЯ РАЗНЫХ РАЗМЕРОВ GUI
        int scaledSlotGap;
        switch (currentGuiScale) {
            case 2:
                scaledSlotGap = 8;  // Увеличенный гап для размера 2
                break;
            case 3:
                scaledSlotGap = 1;  // Стандартный гап для размера 3
                break;
            case 4:
                scaledSlotGap = -5;  // Уменьшенный гап для размера 4 (почти вплотную)
                break;
            default:
                scaledSlotGap = Math.max(0, (int) (BASE_SLOT_GAP * scaleFactor));
                break;
        }

        int scaledSlotStep = scaledSlotSize + scaledSlotGap;
        int scaledInvSlotStartX = (int) (BASE_INV_SLOT_START_X * scaleFactor);
        int scaledInvSlotStartY = (int) (BASE_INV_SLOT_START_Y * scaleFactor);
        int scaledInvBgW = panelWidth;
        int scaledArmorYOffset = (int) (BASE_ARMOR_Y_OFFSET * scaleFactor);

        int relPlayerX = playerPanelX - bgX;
        int relPlayerY = playerPanelY - bgY;
        int relVillagerX = villagerPanelX - bgX;
        int relVillagerY = villagerPanelY - bgY;

        int playerStartX = relPlayerX + scaledInvSlotStartX;
        int playerStartY = relPlayerY + scaledInvSlotStartY;
        int villagerStartX = relVillagerX + scaledInvSlotStartX;
        int villagerStartY = relVillagerY + scaledInvSlotStartY;

        int armorSlotIndex = 0;
        int playerSlotIndex = 0;
        int villagerSlotIndex = 0;

        for (int i = 0; i < this.handler.slots.size(); i++) {
            var slotObj = this.handler.slots.get(i);

            if (currentTab == TabType.REPAIR) {
                slotObj.x = -1000;
                slotObj.y = -1000;
                continue;
            }

            if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                int armorRowWidth = (5 * scaledSlotSize) + (4 * (scaledSlotStep - scaledSlotSize));
                int armorStartX = relPlayerX + (scaledInvBgW - armorRowWidth) / 2;
                int armorY = relPlayerY + scaledInvSlotStartY + (BASE_GRID_ROWS * scaledSlotStep) + scaledArmorYOffset;

                slotObj.x = armorStartX + armorSlotIndex * scaledSlotStep + 1;
                slotObj.y = armorY + 1;
                armorSlotIndex++;
            }
            else if (i < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                int col = playerSlotIndex % BASE_GRID_COLS;
                int row = playerSlotIndex / BASE_GRID_COLS;
                slotObj.x = playerStartX + col * scaledSlotStep + 1;
                slotObj.y = playerStartY + row * scaledSlotStep + 1;
                playerSlotIndex++;
            }
            else {
                if (currentTab == TabType.TRADE) {
                    int col = villagerSlotIndex % BASE_GRID_COLS;
                    int row = villagerSlotIndex / BASE_GRID_COLS;
                    slotObj.x = villagerStartX + col * scaledSlotStep + 1;
                    slotObj.y = villagerStartY + row * scaledSlotStep + 1;
                    villagerSlotIndex++;
                } else {
                    slotObj.x = -1000;
                    slotObj.y = -1000;
                    villagerSlotIndex++;
                }
            }
        }
    }

    private void loadRecipes() {
        System.out.println("[TradeOverhaul] loadRecipes called");
        refreshRecipesForCurrentVillager();
    }

    private void createRecipeList() {
        if (recipeListPanel != null) remove(recipeListPanel);

        // Координаты для инициализации виджета
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // ✅ Используем this.recipesX напрямую (абсолютная координата)
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

        // ✅ ИСПРАВЛЕНИЕ: сохраняем ручной выбор во время фильтрации
        if (isFilterActive && !filteredItem.isEmpty()) {
            int uniqueIdx = recipe.getUniqueIngredientIndex();
            if (uniqueIdx >= 0 && uniqueIdx < recipe.getIngredients().size()) {
                Ingredient uniqueIng = recipe.getIngredients().get(uniqueIdx);
                if (ItemStack.areItemsEqual(uniqueIng.getItem(), filteredItem)) {
                    this.selectedItemSlot = filteredSlot;
                    this.selectedItemStack = filteredItem.copy();
                    this.hasSelectedItem = true;
                    this.selectedInventoryIndex = getRealInventoryIndex(filteredSlot);
                }
            }
            return; // Не выполняем код сброса ниже
        }

        // Стандартный сброс только без фильтра
        this.hasSelectedItem = false;
        this.selectedItemStack = ItemStack.EMPTY;
        this.selectedItemSlot = -1;
        this.selectedInventoryIndex = -1;
    }

    private void onTabSelected(TabType tab) {
        currentTab = tab;

        if (currentTab != TabType.TRADE) {
            expectedXp = 0f;
            if (tradePanel != null && tradePanel.isVisible()) {
                tradePanel.close();
            }
        }

        if (currentTab != TabType.REPAIR) {
            resetSelectedRepairItem();
        }

        if (tab != TabType.DISASSEMBLE) {
            isDisassemblyActive = false;
            disassemblyTarget = ItemStack.EMPTY;
            disassemblyTargetSlotIndex = -1;
            disassemblyComponents.clear();
        }

        positionSlots();
        if (tab == TabType.REPAIR) {
            refreshDamagedItems();
            if (repairPanel != null) repairPanel.updateItems(damagedItems);
        }
        updatePanelVisibility();

        TradeOverhaulMod.LOGGER.info(currentTab.name());
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

    private void drawTradePanel(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int villagerX = x + this.tradePanelX;

        int level = handler.getProfessionLevel();
        int xpForNext = handler.getXpForNextLevel();
        int experience = handler.getProfessionExperience();
        float fractionalXp = handler.getClientFractionalXp();
        float totalXp = experience + fractionalXp;
        float progress = xpForNext > 0 ? Math.min(1.0f, totalXp / xpForNext) : 1.0f;

        if (progress < 1.0f && xpForNext > 0) {
            // ✅ Возвращаем старые координаты
            int barWidth = GRID_COLS * SLOT_STEP;
            int barHeight = 4;
            int barX = villagerX - 40;
            int barY = y + this.panelY - 35;

            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF303030);
            int filledWidth = (int) (barWidth * progress);
            context.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF40FF40);

            // ✅ ПРЕДВАРИТЕЛЬНЫЙ ОПЫТ (голубой) - показываем только если expectedXp > 0
            if (expectedXp > 0 && tradePanel != null && tradePanel.isVisible()) {
                float progressGain = Math.min(1.0f - progress, expectedXp / xpForNext);
                int blueStartX = barX + filledWidth;
                int blueWidth = (int) (barWidth * progressGain);
                context.fill(blueStartX, barY, blueStartX + blueWidth, barY + barHeight, 0xFF00FFFF);
            }

            // ✅ ТЕКСТ ОПЫТА с голубым числом при наведении
            context.getMatrices().push();
            context.getMatrices().translate(barX + barWidth + 3, barY - 2, 0);
            float textScale = 0.7f;
            context.getMatrices().scale(textScale, textScale, 1.0f);

            if (expectedXp > 0 && tradePanel != null && tradePanel.isVisible()) {
                // Если панель открыта и есть предварительный опыт - показываем сумму голубым
                float totalWithExpected = totalXp + expectedXp;
                String leftPart = String.format("%.2f", totalWithExpected);
                String rightPart = String.format(" / %d XP", xpForNext);

                context.drawText(this.textRenderer, Text.literal(leftPart), 0, 0, 0x00FFFF, false);
                context.drawText(this.textRenderer, Text.literal(rightPart),
                        this.textRenderer.getWidth(leftPart), 0, 0x808080, false);
            } else {
                // Обычное отображение
                String text = String.format("%.2f / %d XP", totalXp, xpForNext);
                context.drawText(this.textRenderer, Text.literal(text), 0, 0, 0x808080, false);
            }

            context.getMatrices().pop();

            // === ПРОФЕССИЯ И УРОВЕНЬ ЖИТЕЛЯ ===
            VillagerEntity villager = handler.getVillagerFromWorld(this.client);
            if (villager != null) {
                var profession = villager.getVillagerData().getProfession();
                var villagerLevel = villager.getVillagerData().getLevel();

                Text professionName = Text.empty();
                if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE) {
                    var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
                    if (profId != null) {
                        professionName = Text.translatable("entity.minecraft.villager." + profId.getPath());
                    }
                }

                Text levelName = switch (villagerLevel) {
                    case 1 -> Text.translatable("merchant.level.1");
                    case 2 -> Text.translatable("merchant.level.2");
                    case 3 -> Text.translatable("merchant.level.3");
                    case 4 -> Text.translatable("merchant.level.4");
                    case 5 -> Text.translatable("merchant.level.5");
                    default -> Text.translatable("merchant.level.1");
                };

                Text fullText = Text.literal("").append(professionName).append(" - ").append(levelName);

                // ✅ Возвращаем старые координаты
                context.getMatrices().push();
                context.getMatrices().translate(barX, barY - this.textRenderer.fontHeight - 4, 0);
                float professionTextScale = 0.7f;
                context.getMatrices().scale(professionTextScale, professionTextScale, 1.0f);
                context.drawText(this.textRenderer, fullText, 0, 0, 0xFFFFFF, false);
                context.getMatrices().pop();
            }
        } else if (level >= 5) {
            Text maxLevelText = Text.translatable("tradeoverhaul.gui.maxlevel").formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD);
            context.drawText(this.textRenderer, maxLevelText, villagerX, y + this.panelY - 25, 0xFFAA00, true);
        }

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

        // ✅ Lvl. X с новыми координатами (относительно villagerX)
        context.getMatrices().push();
        context.getMatrices().translate(villagerX - 35, y + 47, 0);
        float levelTextScale = 0.7f;
        context.getMatrices().scale(levelTextScale, levelTextScale, 1.0f);
        Text levelText = Text.literal("Lvl. " + level);
        context.drawText(this.textRenderer, levelText, 0, 0, 0x808080, false);
        context.getMatrices().pop();

        int wallet = handler.getSyncedWallet();
        drawCurrencyWithIcons(context, wallet, villagerX + 24, inventoryY + INV_LABEL_Y - 4);
    }

    private void openDisassemblyPreview(ItemStack stack) {
        disassemblyTarget = stack.copy();
        disassemblyComponents.clear();
        disassemblyCost = 0;
        isDisassemblyActive = true;
        disassemblyTargetSlotIndex = -1;

        // Находим индекс слота для сервера
        for (Slot slot : handler.slots) {
            if (slot.getStack() == stack) {
                disassemblyTargetSlotIndex = slot.id;
                break;
            }
        }

        if (client.world == null) return;

        net.minecraft.recipe.CraftingRecipe recipe = findClientRecipe(stack, (net.minecraft.client.world.ClientWorld) client.world);

        if (recipe != null) {
            Map<Item, Integer> itemCounts = new LinkedHashMap<>();
            int totalParts = 0;

            for (net.minecraft.recipe.Ingredient ing : recipe.getIngredients()) {
                ItemStack[] matches = ing.getMatchingStacks();
                if (matches != null && matches.length > 0) {
                    Item item = matches[0].getItem();

                    itemCounts.put(item, itemCounts.getOrDefault(item, 0) + 1);
                    totalParts++;
                }
            }

            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                ItemStack groupStack = new ItemStack(entry.getKey(), entry.getValue());
                disassemblyComponents.add(groupStack);
            }

            disassemblyCost = totalParts * 5;
        }
    }

    private net.minecraft.recipe.CraftingRecipe findClientRecipe(ItemStack item, net.minecraft.client.world.ClientWorld world) {
        net.minecraft.item.Item targetItem = item.getItem();
        for (var recipe : world.getRecipeManager().values()) {
            if (recipe instanceof net.minecraft.recipe.CraftingRecipe craftingRecipe) {
                net.minecraft.item.ItemStack output = craftingRecipe.getOutput(world.getRegistryManager());
                if (output.getItem() == targetItem && output.getCount() == 1) {
                    return craftingRecipe;
                }
            }
        }
        return null;
    }

    private void drawDisassemblyPanel(DrawContext context, int mouseX, int mouseY) {
        if (!isDisassemblyActive || disassemblyTarget.isEmpty()) return;

        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 120; int panelH = 160;
        int px = cx - panelW/2; int py = cy - panelH/2;

        context.fill(px, py, px + panelW, py + panelH, 0xCC000000);
        context.drawBorder(px, py, panelW, panelH, 0xFF666666);

        context.drawItem(disassemblyTarget, cx - 9, py + 10);
        context.drawItemInSlot(textRenderer, disassemblyTarget, cx - 9, py + 10);

        Text costText = Text.literal("Cost: " + disassemblyCost + " copper").formatted(net.minecraft.util.Formatting.YELLOW);
        context.drawText(textRenderer, costText, cx - textRenderer.getWidth(costText)/2, py + 45, 0xFFFFFF, false);

        int compX = px + 10; int compY = py + 65;
        for (int i = 0; i < Math.min(disassemblyComponents.size(), 9); i++) {
            context.drawItem(disassemblyComponents.get(i), compX + (i % 3) * 19, compY + (i / 3) * 19);
        }

        Text maxText = Text.literal("Max: " + disassemblyComponents.size() + " components").formatted(net.minecraft.util.Formatting.GRAY);
        context.drawText(textRenderer, maxText, px + 5, compY + (disassemblyComponents.size() / 3 + 1) * 19 + 10, 0xAAAAAA, false);

        int btnY = py + panelH - 30;
        boolean canAfford = NumismaticHelper.getTotalMoney(client.player) >= disassemblyCost;
        int btnX = px + panelW - 65;
        context.fill(btnX, btnY, btnX + 60, btnY + 20, canAfford ? 0xFF444444 : 0xFF333333);
        context.fill(btnX + 1, btnY + 1, btnX + 59, btnY + 19, canAfford ? 0xFF666666 : 0xFF444444);
        context.drawText(textRenderer, Text.literal("Disassemble"), btnX + 15, btnY + 6, canAfford ? 0xFFFFFF : 0x888888, false);
        disassembleButtonBounds = new net.minecraft.client.util.math.Rect2i(btnX, btnY, 60, 20);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentScale = (int)client.getWindow().getScaleFactor();
        if (currentScale != (int)(scaleFactor * 3)) {
            calculateScaleFactor();
            calculatePanelPositions();
            positionSlots();
        }
        // === ОБЩИЕ ПЕРЕМЕННЫЕ ===
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // === БАЗОВЫЙ ФОН ===
        this.renderBackground(context);

        drawPanels(context);

        // ✅ НАВИГАЦИЯ: рисуется поверх фона для ВСЕХ вкладок
        renderTabNavigation(context, mouseX, mouseY, delta);

        // === ВКЛАДКА: ТОРГОВЛЯ ===
        if (currentTab == TabType.TRADE) {
            drawPanels(context);
            // Рисуем стандартные слоты и предметы
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

            // === РИСУЕМ ФОНЫ ПАНЕЛЕЙ ===
            // Панель игрока (всегда)
            context.drawTexture(INV_BG_TEX,
                    playerPanelX, playerPanelY,
                    0, 0,
                    panelWidth, panelHeight,           // Размер на экране (масштабируемый)
                    BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT // Реальный размер текстуры (всегда базовый)
            );

            // Надпись "INVENTORY"
            context.getMatrices().push();
            context.getMatrices().translate(
                    playerPanelX + (int)(BASE_INV_LABEL_X * scaleFactor),
                    playerPanelY + (int)(BASE_INV_LABEL_Y * scaleFactor),
                    0
            );
            float textScale = 0.8f * scaleFactor;
            context.getMatrices().scale(textScale, textScale, 1.0f);
            context.drawText(this.textRenderer, Text.literal("INVENTORY"), 0, 0, 0xFFFFFF, false);
            context.getMatrices().pop();

            // Панель жителя
            context.drawTexture(INV_BG_TEX,
                    villagerPanelX, villagerPanelY,
                    0, 0,
                    panelWidth, panelHeight,
                    BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT
            );

            String debugText = String.format("P: %d,%d | V: %d,%d | Scale: %.2f | Gap: %d",
                    playerPanelX, playerPanelY, villagerPanelX, villagerPanelY,
                    scaleFactor, (int)(BASE_GAP_BETWEEN_PANELS * scaleFactor));
            context.drawText(this.textRenderer, Text.literal(debugText),
                    10, 10, 0xFFFFFF, false);

            // Интерфейс торговли
            drawTradePanel(context, mouseX, mouseY, delta);

            // Валюта игрока
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            drawCurrencyWithIcons(context, playerMoney,
                    playerPanelX + (int)(4 * scaleFactor),
                    playerPanelY + (int)(BASE_INV_LABEL_Y * scaleFactor) - (int)(4 * scaleFactor)
            );

            drawBackground(context, delta, mouseX, mouseY);

            if (tradePanel != null && tradePanel.isVisible()) {
                tradePanel.render(context, mouseX, mouseY);

                if (hasSelectedTradeSlot && tradePanel.canAfford()) {
                    // ToDo: Показать предварительный опыт
                }
            }
        }

        // === ВКЛАДКА: КРАФТ ===
        else if (currentTab == TabType.CRAFT) {
            // 1. Рисуем стандартные слоты (инвентарь игрока)
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

            int panelX = x + this.recipesX - 30;  // ~410px от левого края экрана
            int panelY = y + this.panelY - 30;

            // 3. Рисуем фон панели рецептов
            if (showRecipesPanel) {
                context.fill(panelX, panelY, panelX + recipesPanelWidth, panelY + recipesPanelHeight, 0xCC000000);
                context.fill(panelX + 1, panelY + 1, panelX + recipesPanelWidth - 1, panelY + recipesPanelHeight - 1, 0xCC333333);
                context.drawBorder(panelX, panelY, recipesPanelWidth, recipesPanelHeight, 0xFFAAAAAA);
                context.drawText(this.textRenderer, Text.literal("Recipes"), panelX + 5, panelY + 5, 0xFFFFFF, false);

                // ✅ Принудительная отрисовка списка рецептов
                if (recipeListPanel != null) {
                    recipeListPanel.setPosition(panelX + 2, panelY + 40);
                    recipeListPanel.render(context, mouseX, mouseY, delta);
                } else {
                    context.drawText(this.textRenderer, Text.literal("Loading..."), panelX + 10, panelY + 50, 0x888888, false);
                }
            }

            if (searchField != null && searchField.isVisible()) {
                searchField.render(context, mouseX, mouseY, delta);
            }

            // 4. Валюта
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            drawCurrencyWithIcons(context, playerMoney, x + this.inventoryX, inventoryY + INV_LABEL_Y - 4);

            // 5. Центральная панель крафта (рисуется ПОСЛЕ списка, чтобы быть поверх)
            renderCraftPanel(context, mouseX, mouseY);

            // 6. Тултипы для покупки
            for (CraftBuySlotInfo info : craftBuyableSlots) {
                if (info.bounds().contains(mouseX, mouseY)) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(info.stack().getName());
                    tooltip.add(Text.literal("§aBuy: " + info.price() + " copper"));
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                }
            }

            // 7. Тултип кнопки Craft
            if (currentRecipe != null && craftButtonBounds != null && isPointOverCraftButton(mouseX, mouseY)) {
                if (isSelectedItemDamaged()) {
                    context.drawTooltip(this.textRenderer, List.of(Text.literal(getDamageWarning())), mouseX, mouseY);
                } else if (!craftEnabled) {
                    context.drawTooltip(this.textRenderer, List.of(Text.literal("§cMissing ingredients or insufficient funds")), mouseX, mouseY);
                }
            }
        }

        // === ВКЛАДКА: РАЗБОРКА ===
        else if (currentTab == TabType.DISASSEMBLE) {
            // Рисуем слоты игрока
            super.render(context, mouseX, mouseY, delta);
            this.drawMouseoverTooltip(context, mouseX, mouseY);

            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            drawCurrencyWithIcons(context, playerMoney, x + this.inventoryX, inventoryY + INV_LABEL_Y - 4);

            // ✅ Панель разборки (рисуется ПОВЕРХ слотов)
            renderDisassemblyPanel(context, mouseX, mouseY);
        }

        // === ВКЛАДКА: РЕМОНТ ===
        else if (currentTab == TabType.REPAIR) {
            // Фон инвентаря
            context.drawTexture(INV_BG_TEX, inventoryX, inventoryY, 0, 0, INV_BG_W, INV_BG_H, INV_BG_W, INV_BG_H);

            // Слоты с повреждёнными предметами
            renderRepairSlots(context, mouseX, mouseY);

            // Правая панель
            int rightPanelX = x + this.inventoryX + (GRID_COLS * SLOT_STEP) + 120;
            int rightPanelY = y + this.panelY - 30;
            int rightPanelWidth = REPAIR_RIGHT_PANEL_W;
            int rightPanelHeight = REPAIR_RIGHT_PANEL_H;

            context.drawTexture(REPAIR_RIGHT_PANEL_TEX, rightPanelX, rightPanelY, 0, 0, rightPanelWidth, rightPanelHeight, rightPanelWidth, rightPanelHeight);

            // === КНОПКИ РЕМОНТА ===
            int buttonX = rightPanelX - 85;
            int buttonY = rightPanelY + 70;
            int btnW = REPAIR_BTN_W;
            int btnH = REPAIR_BTN_H;

            // --- Кнопка Repair ---
            boolean isHoveredRepair = currentRepairItem != null && repairEnabled &&
                    mouseX >= buttonX && mouseX <= buttonX + btnW &&
                    mouseY >= buttonY && mouseY <= buttonY + btnH;
            int stateIndexRepair = isHoveredRepair ? 1 : 0;

            context.drawTexture(REPAIR_BUTTON_TEX, buttonX, buttonY,
                    0, stateIndexRepair * btnH, btnW, btnH, btnW, REPAIR_BTN_TEX_H);

            context.getMatrices().push();
            context.getMatrices().translate(buttonX + btnW / 2f, buttonY + btnH / 2f - 4f, 0);
            context.getMatrices().scale(REPAIR_BTN_TEXT_SCALE, REPAIR_BTN_TEXT_SCALE, 1.0f);
            Text repairText = Text.literal("Repair");
            context.drawText(this.textRenderer, repairText,
                    -this.textRenderer.getWidth(repairText) / 2, 0,
                    repairEnabled ? 0xFFFFFF : 0x888888, false);
            context.getMatrices().pop();

            repairButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, buttonY, btnW, btnH);

            // Кнопка Repair All
            int repairAllButtonY = buttonY + btnH + 5;
            int totalCost = getTotalRepairCost();
            repairAllEnabled = NumismaticHelper.getTotalMoney(this.client.player) >= totalCost && !damagedItems.isEmpty();

            boolean isHoveredRepairAll = repairAllEnabled &&
                    mouseX >= buttonX && mouseX <= buttonX + btnW &&
                    mouseY >= repairAllButtonY && mouseY <= repairAllButtonY + btnH;
            int stateIndexRepairAll = isHoveredRepairAll ? 1 : 0;

            context.drawTexture(REPAIR_BUTTON_TEX, buttonX, repairAllButtonY, 0, stateIndexRepairAll * btnH, btnW, btnH, btnW, REPAIR_BTN_TEX_H);

            context.getMatrices().push();
            context.getMatrices().translate(buttonX + btnW / 2f, repairAllButtonY + btnH / 2f - 4f, 0);
            context.getMatrices().scale(REPAIR_BTN_TEXT_SCALE, REPAIR_BTN_TEXT_SCALE, 1.0f);
            Text repairAllText = Text.literal("Repair All");
            context.drawText(this.textRenderer, repairAllText, -this.textRenderer.getWidth(repairAllText) / 2, 0, repairAllEnabled ? 0xFFFFFF : 0x888888, false);
            context.getMatrices().pop();

            repairAllButtonBounds = new net.minecraft.client.util.math.Rect2i(buttonX, repairAllButtonY, btnW, btnH);

            // Информация о предмете
            if (currentRepairItem != null) {
                DamagedItem selected = currentRepairItem;
                int startX = rightPanelX + rightPanelWidth / 2;
                int startY = rightPanelY + 10;

                // Фон под предметом
                int itemBgX = startX - ITEM_BG_W / 2;
                int itemBgY = startY;
                context.drawTexture(ITEM_BG_TEX, itemBgX, itemBgY, 0, 0, ITEM_BG_W, ITEM_BG_H, ITEM_BG_W, ITEM_BG_H);

                context.getMatrices().push();
                context.getMatrices().translate(startX, startY + ITEM_BG_H / 2f, 0);
                context.getMatrices().scale(2.0f, 2.0f, 1.0f);
                context.drawItem(selected.getStack(), -8, -8);
                context.getMatrices().pop();

                String itemName = selected.getStack().getName().getString();
                int itemNameWidth = this.textRenderer.getWidth(itemName);
                int centeredX = startX - (itemNameWidth / 2);
                context.drawText(this.textRenderer, itemName, centeredX, startY + ITEM_BG_H + 5, 0xFFFFFF, false);

                int durabilityPercent = (int) ((1.0 - (double) selected.getCurrentDamage() / selected.getMaxDamage()) * 100);
                String durabilityText = "Durability";
                context.drawText(this.textRenderer, Text.literal(durabilityText), startX - this.textRenderer.getWidth(durabilityText) / 2, startY + ITEM_BG_H + 20, 0xCCCCCC, false);
                String durabilityPercentText = String.format("%d%%", durabilityPercent);
                context.drawText(this.textRenderer, Text.literal(durabilityPercentText), startX - this.textRenderer.getWidth(durabilityPercentText) / 2, startY + ITEM_BG_H + 30, 0xCCCCCC, false);

                // Цвет цены
                String costText = String.format("Cost: %d copper", selected.getRepairCost());
                int playerMoneyCheck = NumismaticHelper.getTotalMoney(this.client.player);
                int costColor = (playerMoneyCheck >= selected.getRepairCost()) ? 0xFFFFAA : 0x80FF0000;
                context.drawText(this.textRenderer, Text.literal(costText), startX - (this.textRenderer.getWidth(costText) / 2), startY + ITEM_BG_H + 60, costColor, false);
            } else {
                int startX = rightPanelX + rightPanelWidth / 2;
                int startY = rightPanelY + rightPanelHeight / 2;

                Text noSelectedItem = Text.literal("No item selected");
                Text clickToSelect = Text.literal("Click on a damaged item");

                context.drawText(this.textRenderer, noSelectedItem, startX - this.textRenderer.getWidth(noSelectedItem) / 2, startY - 40, 0x8E7D6E, false);
                context.drawText(this.textRenderer, clickToSelect, startX - this.textRenderer.getWidth(clickToSelect) / 2, startY - 25, 0x7E7D6E, false);
            }

            // Деньги игрока
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            drawCurrencyWithIcons(context, playerMoney, x + this.inventoryX, inventoryY + INV_LABEL_Y - 4);

            // Тултипы кнопок
            if (repairButtonBounds != null && currentRepairItem != null &&
                    mouseX >= repairButtonBounds.getX() && mouseX <= repairButtonBounds.getX() + repairButtonBounds.getWidth() &&
                    mouseY >= repairButtonBounds.getY() && mouseY <= repairButtonBounds.getY() + repairButtonBounds.getHeight()) {
                List<Text> tooltip = new ArrayList<>();
                String formattedCost = formatMoney(currentRepairItem.getRepairCost());
                if (repairEnabled) {
                    tooltip.add(Text.literal("§aClick to repair for " + formattedCost));
                } else {
                    tooltip.add(Text.literal("§cNeed " + formattedCost + " to repair"));
                }
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }

            if (repairAllButtonBounds != null && !damagedItems.isEmpty() &&
                    mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                    mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {
                List<Text> tooltip = new ArrayList<>();
                String formattedCost = formatMoney(totalCost);
                if (repairAllEnabled) {
                    tooltip.add(Text.literal("§aClick to repair all items for " + formattedCost));
                } else {
                    tooltip.add(Text.literal("§cNeed " + formattedCost + " to repair all items"));
                }
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        if (currentTab != TabType.REPAIR) {
            drawSlotBackgrounds(context, x, y);
        }
    }

    private void drawSlotBackgrounds(DrawContext context, int x, int y) {
        float scaleFactor = currentGuiScale / 3.0f;
        int scaledSlotSize = (int) (BASE_SLOT_SIZE * scaleFactor);

        // ✅ РАЗНЫЙ ГАП ДЛЯ РАЗНЫХ РАЗМЕРОВ GUI
        int scaledSlotGap;
        switch (currentGuiScale) {
            case 2:
                scaledSlotGap = 8;
                break;
            case 3:
                scaledSlotGap = 1;
                break;
            case 4:
                scaledSlotGap = -5;
                break;
            default:
                scaledSlotGap = Math.max(0, (int) (BASE_SLOT_GAP * scaleFactor));
                break;
        }

        int scaledSlotStep = scaledSlotSize + scaledSlotGap;
        int scaledInvSlotStartX = (int) (BASE_INV_SLOT_START_X * scaleFactor);
        int scaledInvSlotStartY = (int) (BASE_INV_SLOT_START_Y * scaleFactor);
        int scaledInvBgW = panelWidth;
        int scaledInvBgH = panelHeight;
        int scaledArmorYOffset = (int) (BASE_ARMOR_Y_OFFSET * scaleFactor);
        int scaledInvLabelX = (int) (BASE_INV_LABEL_X * scaleFactor);
        int scaledInvLabelY = (int) (BASE_INV_LABEL_Y * scaleFactor);

        // === 1. ФОН ПАНЕЛИ ИГРОКА ===
        context.drawTexture(INV_BG_TEX,
                playerPanelX, playerPanelY,
                0, 0, panelWidth, panelHeight,
                BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT);

        // === 2. НАДПИСЬ "INVENTORY" ===
        context.getMatrices().push();
        context.getMatrices().translate(
                playerPanelX + scaledInvLabelX,
                playerPanelY + scaledInvLabelY,
                0
        );
        float textScale = 0.8f * scaleFactor;
        context.getMatrices().scale(textScale, textScale, 1.0f);
        context.drawText(this.textRenderer, Text.literal("INVENTORY"), 0, 0, 0xFFFFFF, false);
        context.getMatrices().pop();

        // === 3. ФОН ПАНЕЛИ ЖИТЕЛЯ (только для TRADE) ===
        if (currentTab == TabType.TRADE) {
            context.drawTexture(INV_BG_TEX,
                    villagerPanelX, villagerPanelY,
                    0, 0, panelWidth, panelHeight,
                    BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT);
        }

        // === 4. СЛОТЫ ИГРОКА ===
        int playerStartX = playerPanelX + scaledInvSlotStartX;
        int playerStartY = playerPanelY + scaledInvSlotStartY;

        // Броня (5 слотов)
        int armorRowWidth = (5 * scaledSlotSize) + (4 * (scaledSlotStep - scaledSlotSize));
        int armorStartX = playerPanelX + (panelWidth - armorRowWidth) / 2;
        int armorY = playerStartY + (BASE_GRID_ROWS * scaledSlotStep) + scaledArmorYOffset;
        for (int i = 0; i < 5; i++) {
            int slotX = armorStartX + i * scaledSlotStep;
            context.drawTexture(SLOT_TEX, slotX, armorY, 0, 0,
                    SLOT_TEX_W, SLOT_TEX_H, SLOT_TEX_W, SLOT_TEX_H);
        }

        // Инвентарь (6x6)
        for (int row = 0; row < BASE_GRID_ROWS; row++) {
            for (int col = 0; col < BASE_GRID_COLS; col++) {
                int slotX = playerStartX + col * scaledSlotStep;
                int slotY = playerStartY + row * scaledSlotStep;
                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0,
                        SLOT_TEX_W, SLOT_TEX_H, SLOT_TEX_W, SLOT_TEX_H);
            }
        }

        // === 5. СЛОТЫ ЖИТЕЛЯ (только для TRADE) ===
        if (currentTab == TabType.TRADE) {
            int villagerStartX = villagerPanelX + scaledInvSlotStartX;
            int villagerStartY = villagerPanelY + scaledInvSlotStartY;

            // ✅ Получаем актуальный инвентарь жителя из handler'а
            VillagerInventoryComponent villagerInv = handler.getVillagerInventory();

            for (int row = 0; row < BASE_GRID_ROWS; row++) {
                for (int col = 0; col < BASE_GRID_COLS; col++) {
                    int slotX = villagerStartX + col * scaledSlotStep;
                    int slotY = villagerStartY + row * scaledSlotStep;
                    int index = row * BASE_GRID_COLS + col;

                    // Фон слота
                    context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0,
                            scaledSlotSize, scaledSlotSize, SLOT_TEX_W, SLOT_TEX_H);

                    // ✅ Рисуем предмет, если есть
                    ItemStack stack = villagerInv.getStack(index);
                    if (!stack.isEmpty()) {
                        context.drawItem(stack, slotX + 1, slotY + 1);
                        context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);
                    }
                }
            }
        }

        // === 6. СТАТУСНЫЕ ОВЕРЛЕИ (ACTIVE/LOCKED) - только для слотов игрока ===
        // Рисуются ПОВЕРХ всех слотов
        for (Slot slot : handler.slots) {
            // Пропускаем слоты жителя
            if (slot.id >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) continue;
            if (slot.x < 0) continue; // Пропускаем скрытые слоты

            SlotState state = getSlotState(slot.id, slot);
            if (state == SlotState.DEFAULT) continue;

            Identifier tex = (state == SlotState.ACTIVE) ? ACTIVE_SLOT_TEX : LOCKED_SLOT_TEX;

            float guiScaleTexture = 1.0f;
            if (currentGuiScale == 2) guiScaleTexture = 1.5f;
            // Оверлей рисуется поверх слота с небольшим смещением
            context.drawTexture(tex, x + slot.x - 1, y + slot.y - 1, 0, 0,
                    STATE_SLOT_W, STATE_SLOT_H,
                    STATE_SLOT_W, STATE_SLOT_H);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}

    @Override
    public void removed() {
        if (recipeListPanel != null) remove(recipeListPanel);
        if (searchField != null) remove(searchField);
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
        float scaleFactor = currentGuiScale / 3.0f;
        int scaledSlotSize = (int) (BASE_SLOT_SIZE * scaleFactor);
        return mouseX >= slotX && mouseX < slotX + scaledSlotSize &&
                mouseY >= slotY && mouseY < slotY + scaledSlotSize;
    }

    private boolean isPointOverCraftButton(double mouseX, double mouseY) {
        if (craftButtonBounds == null) return false;
        return mouseX >= craftButtonBounds.getX() && mouseX <= craftButtonBounds.getX() + craftButtonBounds.getWidth() &&
                mouseY >= craftButtonBounds.getY() && mouseY <= craftButtonBounds.getY() + craftButtonBounds.getHeight();
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
        // Навигация (ЛКМ)
        if (button == 0 && navLeftBounds != null) {
            if (isOver(mouseX, mouseY, navLeftBounds)) {
                switchTab(currentTabIndex - 1);
                return true;
            }
            if (isOver(mouseX, mouseY, navRightBounds)) {
                switchTab(currentTabIndex + 1);
                return true;
            }
            if (navDotBounds != null) {
                for (int i = 0; i < navDotBounds.size(); i++) {
                    if (isOver(mouseX, mouseY, navDotBounds.get(i))) {
                        switchTab(i);
                        return true;
                    }
                }
            }
        }

        // === ЛКМ (button == 0) ===
        if (button == 0) {
            // 1. СНАЧАЛА проверяем клик по слоту в TRADE (даже если панель открыта)
            if (currentTab == TabType.TRADE) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null && clickedSlot.hasStack()) {
                    int slotIndex = clickedSlot.id;
                    ItemStack stack = clickedSlot.getStack();

                    boolean buying = slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT;
                    int price = buying ? handler.getClientBuyPrice(slotIndex) : handler.getClientSellPrice(stack);

                    if (price > 0) {
                        // Открываем/обновляем панель с новым предметом
                        openTradePanel(clickedSlot);
                        return true;
                    }
                    return true;
                }
            }

            // 2. ПОТОМ проверяем клик по панели торговли
            if (currentTab == TabType.TRADE && tradePanel != null && tradePanel.isVisible()) {
                if (tradePanel.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                // Если клик вне панели И не по слоту - закрываем
                if (!isPointOverTradePanel(mouseX, mouseY)) {
                    tradePanel.close();
                    return true;
                }
            }

            // 3. Обработка поля поиска в CRAFT
            if (currentTab == TabType.CRAFT) {
                if (searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight()) {
                    searchField.setFocused(true);
                    return searchField.mouseClicked(mouseX, mouseY, button);
                } else if (searchField != null) {
                    searchField.setFocused(false);
                }
            }

            // 4. Закрытие панели разборки при клике вне её
            if (currentTab == TabType.DISASSEMBLE && isDisassemblyActive) {
                boolean clickedOnPanel = isPointOverDisassemblyPanel(mouseX, mouseY);
                boolean clickedOnSlot = getSlotAt(mouseX, mouseY) != null;
                if (!clickedOnPanel && !clickedOnSlot) {
                    isDisassemblyActive = false;
                    disassemblyTarget = ItemStack.EMPTY;
                    disassemblyTargetSlotIndex = -1;
                    disassemblyComponents.clear();
                }
            }

            // 5. Выбор предмета для DISASSEMBLE
            if (currentTab == TabType.DISASSEMBLE) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null && clickedSlot.id < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                    ItemStack stack = clickedSlot.getStack();
                    if (!stack.isEmpty()) {
                        if (DisassemblyConfig.isDisassemblyAllowed(stack.getItem())) {
                            openDisassemblyPreview(stack);
                        }
                        return true;
                    }
                }
            }

            // 6. Кнопка CRAFT
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

            // 7. Кнопки REPAIR
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

            // 8. Кнопка DISASSEMBLE
            if (isDisassemblyActive && disassembleButtonBounds != null) {
                int btnRight = disassembleButtonBounds.getX() + disassembleButtonBounds.getWidth();
                int btnBottom = disassembleButtonBounds.getY() + disassembleButtonBounds.getHeight();
                if (mouseX >= disassembleButtonBounds.getX() && mouseX <= btnRight &&
                        mouseY >= disassembleButtonBounds.getY() && mouseY <= btnBottom) {

                    isDisassembleButtonPressed = true;

                    boolean canAfford = NumismaticHelper.getTotalMoney(client.player) >= disassemblyCost;
                    if (canAfford && disassemblyTargetSlotIndex != -1) {
                        var buf = PacketByteBufs.create();
                        buf.writeInt(handler.syncId);
                        buf.writeInt(disassemblyTargetSlotIndex);
                        ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "disassemble_request"), buf);

                        isDisassemblyActive = false;
                        disassemblyTargetSlotIndex = -1;
                        disassemblyComponents.clear();
                        disassemblyTarget = ItemStack.EMPTY;
                        return true;
                    }
                }
            }

            // 9. Сброс фильтров в CRAFT при клике в пустоту
            if (currentTab == TabType.CRAFT) {
                boolean clickedOnRecipePanel = isPointOverRecipePanel(mouseX, mouseY);
                boolean clickedOnCraftButton = isPointOverCraftButton(mouseX, mouseY);
                boolean clickedOnSlot = getSlotAt(mouseX, mouseY) != null;
                boolean clickedOnSearchField = searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight();
                boolean clickedOnCraftPanel = craftPanelBounds != null && craftPanelBounds.contains((int)mouseX, (int)mouseY);

                if (!clickedOnRecipePanel && !clickedOnCraftButton && !clickedOnSlot && !clickedOnSearchField && !clickedOnCraftPanel) {
                    resetAllFilters();
                }
            }

            // 10. Логика REPAIR (выбор предмета)
            if (currentTab == TabType.REPAIR) {
                List<DamagedItem> allDamaged = new ArrayList<>();
                PlayerInventory inv = client.player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > 0) {
                        allDamaged.add(new DamagedItem(stack, i));
                    }
                }
                allDamaged.sort(Comparator.comparingInt(DamagedItem::getSlotIndex));

                int startX = inventoryX + INV_SLOT_START_X;
                int startY = inventoryY + INV_SLOT_START_Y;

                boolean clickedOnSlot = false;
                for (int i = 0; i < Math.min(allDamaged.size(), GRID_ROWS * GRID_COLS); i++) {
                    int row = i / GRID_COLS;
                    int col = i % GRID_COLS;
                    int slotX = startX + col * SLOT_STEP;
                    int slotY = startY + row * SLOT_STEP;

                    if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                            mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                        DamagedItem selected = allDamaged.get(i);
                        if (repairPanel != null) {
                            repairPanel.updateItems(allDamaged);
                            repairPanel.selectItemBySlotIndex(selected.getSlotIndex());
                        }
                        onRepairItemSelected(selected);
                        clickedOnSlot = true;
                        break;
                    }
                }

                if (!clickedOnSlot && !isPointOverRepairButtons(mouseX, mouseY)) {
                    resetSelectedRepairItem();
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        // === ПКМ (button == 1) ===
        if (button == 1) {
            // Покупка предметов в CRAFT панели
            if (currentTab == TabType.CRAFT) {
                for (CraftBuySlotInfo info : craftBuyableSlots) {
                    if (info.bounds().contains((int)mouseX, (int)mouseY)) {
                        var buf = PacketByteBufs.create();
                        buf.writeInt(handler.syncId);
                        buf.writeString(net.minecraft.registry.Registries.ITEM.getId(info.stack().getItem()).toString());
                        buf.writeByte(1);
                        ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "craft_panel_buy_request"), buf);
                        return true;
                    }
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isPointOverRecipePanel(double mouseX, double mouseY) {
        if (recipeListPanel == null) return false;
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = this.recipesX - 30;
        int panelY = y + this.panelY - 30;
        return mouseX >= panelX && mouseX <= panelX + recipesPanelWidth &&
                mouseY >= panelY && mouseY <= panelY + recipesPanelHeight;
    }

    private void filterRecipesByItem(ItemStack item, int slotIndex) {
        if (item.isEmpty()) return;

        // 1. ПРИНУДИТЕЛЬНО выбираем предмет как "уникальный" (подсвечиваем слот)
        this.selectedItemSlot = slotIndex;
        this.selectedItemStack = item.copy();
        this.hasSelectedItem = true;
        this.selectedInventoryIndex = getRealInventoryIndex(slotIndex);

        // 2. Обновляем состояние фильтра
        this.filteredItem = item.copy();
        this.filteredSlot = slotIndex;
        this.isFilterActive = true;

        // 3. Запоминаем текущий рецепт, чтобы попытаться его сохранить
        CraftRecipe previouslySelected = this.currentRecipe;

        // 4. Фильтруем список
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

        // 5. ЛОГИКА ВОССТАНОВЛЕНИЯ:
        // Если рецепт был выбран И он остался в отфильтрованном списке -> оставляем его выбранным.
        // Если его нет в списке -> снимаем выбор.
        if (previouslySelected != null && availableRecipes.contains(previouslySelected)) {
            this.currentRecipe = previouslySelected;
            this.selectedRecipeIndex = availableRecipes.indexOf(previouslySelected);
            if (recipeListPanel != null) recipeListPanel.setSelectedIndex(this.selectedRecipeIndex);
        } else {
            this.currentRecipe = null;
            this.selectedRecipeIndex = -1;
            if (recipeListPanel != null) recipeListPanel.setSelectedIndex(-1);
        }
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        calculateScaleFactor();
        calculatePanelPositions();
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
        // Сначала передаём событие панели торговли
        if (currentTab == TabType.TRADE && tradePanel != null && tradePanel.isVisible()) {
            if (tradePanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        if (button == 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Сначала передаём событие панели торговли
        if (currentTab == TabType.TRADE && tradePanel != null && tradePanel.isVisible()) {
            if (tradePanel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        isDisassembleButtonPressed = false;
        if (button == 0) return super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    private void resetAllFilters() {
        isFilterActive = false;
        filteredItem = ItemStack.EMPTY;
        filteredSlot = -1;

        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);  // allRecipes теперь содержит серверные рецепты

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

        System.out.println("[TradeOverhaul] All filters reset, available recipes: " + availableRecipes.size());
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
                resetSelectedRepairItem();
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
        repairPanel.setVisible(showRepairPanel);
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

    public enum TabType {
        TRADE, CRAFT, DISASSEMBLE, REPAIR
    }

    private void renderDisassemblyPanel(DrawContext context, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 125;
        int panelH = 190;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;

        if (!isDisassemblyActive || disassemblyTarget.isEmpty()) {
            context.drawTexture(DISASSEMBLE_PANEL_EMPTY_TEX, px, py, 0, 0, panelW, panelH, panelW, panelH);

            String hintText = Text.translatable("tradeoverhaul.disassemble.hint").getString();
            String[] lines = hintText.split("\n");

            int lineHeight = 10;
            int totalHeight = lines.length * lineHeight;
            int startY = cy - totalHeight / 2;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineWidth = textRenderer.getWidth(line);
                context.drawText(textRenderer, Text.literal(line),
                        cx - lineWidth / 2,
                        startY + i * lineHeight,
                        0x888888, false);
            }

            return;
        }

        // ✅ Если предмет выбран - рисуем полную информационную панель
        context.drawTexture(PANEL_TEX, px, py, 0, 0, panelW, panelH, PANEL_TEX_W, PANEL_TEX_H);

        // === ФОН РАЗБИРАЕМОГО ПРЕДМЕТА ===
        int itemBgX = cx - ITEM_BG_W / 2;
        int itemBgY = py + 22;
        context.drawTexture(SLOT_TEX, itemBgX, itemBgY, 0, 0, ITEM_BG_W, ITEM_BG_H, ITEM_BG_W, ITEM_BG_H);

        // === САМ ПРЕДМЕТ (поверх фона) ===
        context.getMatrices().push();
        context.getMatrices().translate(itemBgX + ITEM_BG_W / 2, itemBgY + ITEM_BG_H / 2, 0);
        context.getMatrices().scale(2.4f, 2.4f, 1.0f);
        context.drawItem(disassemblyTarget, -8, -8);
        context.drawItemInSlot(this.textRenderer, disassemblyTarget, -8, -8);
        context.getMatrices().pop();

        // Название
        Text itemName = disassemblyTarget.getName();
        context.drawText(this.textRenderer, itemName, cx - this.textRenderer.getWidth(itemName) / 2, py + 10, 0xFFFFFF, false);

        // Стоимость
        Text costLabel = Text.literal("Disassembly cost");
        context.drawText(this.textRenderer, costLabel, cx - this.textRenderer.getWidth(costLabel) / 2, py + 78, 0x8E7D6E, false);
        Text costText = Text.literal(disassemblyCost + " copper");
        context.drawText(this.textRenderer, costText, cx - this.textRenderer.getWidth(costText) / 2, py + 94, 0x8E7D6E, false);

        // === ИНГРЕДИЕНТЫ ===
        int compStartY = py + 115;
        int maxPerRow = 3;
        int totalItems = disassemblyComponents.size();
        int slotGap = 2;
        int slotSize = 18;
        int currentComponentIndex = 0;

        for (int row = 0; currentComponentIndex < totalItems; row++) {
            int itemsInThisRow = Math.min(maxPerRow, totalItems - currentComponentIndex);
            int rowWidth = (itemsInThisRow * slotSize) + ((itemsInThisRow - 1) * slotGap);
            int rowStartX = px + (panelW - rowWidth) / 2;

            for (int i = 0; i < itemsInThisRow; i++) {
                int slotX = rowStartX + i * (slotSize + slotGap);
                int slotY = compStartY + row * (slotSize + slotGap + 4);
                ItemStack comp = disassemblyComponents.get(currentComponentIndex);

                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, slotSize, slotSize, SLOT_TEX_W, SLOT_TEX_H);
                context.drawItem(comp, slotX + 1, slotY + 1);
                context.drawItemInSlot(this.textRenderer, comp, slotX + 1, slotY + 1);

                currentComponentIndex++;
            }
        }

        // === КНОПКА ===
        int btnW = 95;
        int btnH = 20;
        int btnX = cx - btnW / 2;
        int btnY = py + panelH - 25;

        boolean canAfford = NumismaticHelper.getTotalMoney(client.player) >= disassemblyCost;
        boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        int stateIndex = 0;
        if (!canAfford) {
            stateIndex = 2;
        } else if (isDisassembleButtonPressed) {
            stateIndex = 2;
        } else if (isHovered) {
            stateIndex = 1;
        }

        int vOffset = stateIndex * BTN_STATE_H;
        context.drawTexture(BUTTON_TEX, btnX, btnY, 0, vOffset, btnW, btnH, BUTTON_TEX_W, BUTTON_TEX_H);

        Text btnText = Text.literal("DISASSEMBLE");
        context.drawText(this.textRenderer, btnText, cx - this.textRenderer.getWidth(btnText) / 2, btnY + 6, canAfford ? 0xFFFFFF : 0x888888, false);

        disassembleButtonBounds = new net.minecraft.client.util.math.Rect2i(btnX, btnY, btnW, btnH);
    }

    private enum SlotState { DEFAULT, ACTIVE, LOCKED }

    /**
     * Определяет визуальное состояние слота в зависимости от вкладки и условий
     */
    private SlotState getSlotState(int slotIndex, Slot slot) {
        ItemStack stack = slot.getStack();

        if (currentTab == TabType.TRADE) {
            // ✅ Слоты жителя теперь всегда обычные
            if (slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                return SlotState.DEFAULT;
            }
            // Слоты игрока (продажа)
            if (slotIndex < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                if (stack.isEmpty()) return SlotState.DEFAULT;
                boolean canSell = handler.canVillagerBuyItem(stack);
                int sellPrice = handler.getClientSellPrice(stack);
                int villagerMoney = handler.getSyncedWallet();

                if (canSell && villagerMoney >= sellPrice) return SlotState.ACTIVE;
                if (canSell && villagerMoney < sellPrice) return SlotState.LOCKED;
                return SlotState.DEFAULT; // Не в конфиге
            }
        }
        else if (currentTab == TabType.CRAFT) {
            // ✅ Подсвечиваем ВСЕГДА, если слот совпадает с выбранным
            if (selectedItemSlot >= 0 && slotIndex == selectedItemSlot) {
                return SlotState.ACTIVE;
            }
        }
        else if (currentTab == TabType.DISASSEMBLE) {
            if (slotIndex < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                if (slotIndex == disassemblyTargetSlotIndex) return SlotState.ACTIVE;
                if (!stack.isEmpty() && !DisassemblyConfig.isDisassemblyAllowed(stack.getItem())) {
                    return SlotState.LOCKED;
                }
            }
        }
        return SlotState.DEFAULT;
    }

    private boolean isPointOverDisassemblyPanel(double mouseX, double mouseY) {
        if (!isDisassemblyActive) return false;
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 125;
        int panelH = 190;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;
        return mouseX >= px && mouseX <= px + panelW && mouseY >= py && mouseY <= py + panelH;
    }

    // Максимальное место в инвентаре игрока для стака предметов
    private int getMaxInventorySpaceForStack(ItemStack template) {
        int maxStack = template.getMaxCount();
        int space = 0;
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) {
                space += maxStack;
            } else if (ItemStack.areItemsEqual(s, template) && s.getCount() < maxStack) {
                space += maxStack - s.getCount();
            }
            if (space >= 64) break; // reasonable limit
        }
        return space;
    }

    // Максимальное место в инвентаре жителя для стака предметов
    private int getMaxVillagerSpaceForStack(ItemStack template) {
        int maxStack = template.getMaxCount();
        int space = 0;
        var villagerInv = handler.getVillagerInventory();
        if (villagerInv == null) return 0;

        for (int i = 0; i < villagerInv.size(); i++) {
            ItemStack s = villagerInv.getStack(i);
            if (s.isEmpty()) {
                space += maxStack;
            } else if (ItemStack.areItemsEqual(s, template) && s.getCount() < maxStack) {
                space += maxStack - s.getCount();
            }
            if (space >= 64) break;
        }
        return space;
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

    private void drawCurrencyWithIcons(DrawContext context, int totalCopper, int x, int y) {
        int currentX = x + 4;
        int iconSize = 16;
        float scale = 0.7f; // Уменьшенный шрифт

        if (totalCopper == 0) {
            context.drawTexture(NUM_COPPER, currentX, y, 0, 0, iconSize, iconSize, 16, 16);
            drawScaledText(context, "0", currentX + iconSize, y + 6, scale);
            return;
        }

        // Стандартные соотношения Numismatic Overhaul: 1G=100S, 1S=100C => 1G=10000C
        int g = totalCopper / 10000;
        int s = (totalCopper % 10000) / 100;
        int c = totalCopper % 100;

        currentX = x + 4;
        int gap = 4;

        // Золото
        if (g > 0) {
            context.drawTexture(NUM_GOLD, currentX, y, 0, 0, iconSize, iconSize, 16, 16);
            drawScaledText(context, String.valueOf(g), currentX + iconSize, y + 6, scale);
            currentX += iconSize + 7;
        }
        // Серебро
        if (s > 0 || g > 0) {
            context.drawTexture(NUM_SILVER, currentX, y, 0, 0, iconSize, iconSize, 16, 16);
            drawScaledText(context, String.valueOf(s), currentX + iconSize, y + 6, scale);
            currentX += iconSize + 7;
        }
        // Медь
        if (c > 0 || g > 0 || s > 0) {
            context.drawTexture(NUM_COPPER, currentX, y, 0, 0, iconSize, iconSize, 16, 16);
            drawScaledText(context, String.valueOf(c), currentX + iconSize, y + 6, scale);
        }
    }

    private void drawScaledText(DrawContext context, String text, int x, int y, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(this.textRenderer, Text.literal(text), 0, 0, 0xFFFFFF, false);
        context.getMatrices().pop();
    }

    // Подсчёт предметов в инвентаре игрока
    private int countPlayerItems(ItemStack template, boolean excludeSelectedSlot) {
        if (template.isEmpty()) return 0;
        int count = 0;
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (excludeSelectedSlot && i == selectedItemSlot) continue;
            ItemStack stack = inv.getStack(i);
            if (ItemStack.areItemsEqual(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // Цвет текста "have/need"
    private int getIngredientColor(int have, int need, ItemStack template, boolean isUnique) {
        if (isUnique) {
            // Уникальный: зелёный если выбран (1/1), красный если нет (0/1)
            return (hasSelectedItem && selectedItemSlot >= 0) ? 0x55FF55 : 0xFF6666;
        }
        if (have >= need) return 0x55FF55; // Зелёный: хватает

        // ✅ Жёлтый: не хватает, НО житель продаёт этот предмет ПРЯМО СЕЙЧАС
        // Java 17 compatible syntax
        if (handler instanceof VillagerCraftingScreenHandler) {
            VillagerCraftingScreenHandler villagerHandler = (VillagerCraftingScreenHandler) handler;
            if (villagerHandler.hasItemInTradeInventory(template)) {
                return 0xFFFF55;
            }
        }

        return 0xFF6666; // Красный: не хватает и нет в продаже
    }

    private void renderCraftPanel(DrawContext context, int mouseX, int mouseY) {
        craftBuyableSlots.clear();

        int cx = this.centerX + 80; // Подгоняй X-координату панели
        int cy = this.height / 2;
        int panelW = CRAFT_PANEL_W;
        int panelH = CRAFT_PANEL_H;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;

        craftPanelBounds = new net.minecraft.client.util.math.Rect2i(px, py, panelW, panelH);

        // 1. Фон
        context.drawTexture(CRAFT_PANEL_TEX, px, py, 0, 0, panelW, panelH, panelW, panelH);

        if (currentRecipe == null) {
            Text hint = Text.literal("Select a recipe from the list");
            context.drawText(textRenderer, hint, cx - textRenderer.getWidth(hint)/2, cy - 10, 0x888888, false);
            return;
        }

        // 2. Результат (48×48) + РАМКА
        ItemStack result = currentRecipe.getResult();
        int itemX = cx - CRAFT_ITEM_BG_SIZE / 2;
        int itemY = py + 20;

        context.drawTexture(SLOT_TEX, itemX, itemY, 0, 0, 48, 48, 48, 48);

        context.getMatrices().push();
        context.getMatrices().translate(itemX + CRAFT_ITEM_BG_SIZE / 2, itemY + CRAFT_ITEM_BG_SIZE / 2, 0);
        context.getMatrices().scale(2.5f, 2.5f, 1.0f);
        context.drawItem(result, -8, -8);
        context.drawItemInSlot(this.textRenderer, result, -8, -8);
        context.getMatrices().pop();

        Text itemName = result.getName();
        context.drawText(this.textRenderer, itemName, cx - this.textRenderer.getWidth(itemName) / 2, itemY - 12, 0xFFFFFF, false);

        // 3. REQUIREMENTS
        Text reqLabel = Text.literal("REQUIREMENTS");
        context.getMatrices().push();
        context.getMatrices().translate(cx - (this.textRenderer.getWidth(reqLabel) * 0.8f) / 2f, py + 82, 0);
        context.getMatrices().scale(0.8f, 0.8f, 1.0f);
        context.drawText(this.textRenderer, reqLabel, 0, 0, 0x8E7D6E, false);
        context.getMatrices().pop();

        // 4. Ингредиенты + ПЕРЕСЧЁТ craftEnabled
        List<Ingredient> ingredients = currentRecipe.getIngredients();
        int uniqueIdx = currentRecipe.getUniqueIngredientIndex();
        int slotSize = CRAFT_SLOT_SIZE;
        int slotGap = CRAFT_SLOT_GAP;
        int startIngY = py + 92;

        boolean hasAllIngredients = true;
        boolean canAfford = NumismaticHelper.getTotalMoney(client.player) >= currentRecipe.getCost();

        // Уникальный предмет
        if (uniqueIdx >= 0 && uniqueIdx < ingredients.size()) {
            Ingredient uniqueIng = ingredients.get(uniqueIdx);
            ItemStack template = uniqueIng.getItem();
            int need = uniqueIng.getCount();
            boolean isSelected = hasSelectedItem && selectedItemSlot >= 0;
            int have = isSelected ? 1 : 0;

            if (!isSelected) hasAllIngredients = false;

            int color = isSelected ? 0x55FF55 : 0xFF6666;
            int slotX = cx - slotSize / 2;
            int slotY = startIngY;

            context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, slotSize, slotSize, 24, 24);
            context.drawItem(template, slotX + 4, slotY + 4); // +4 центрирует 16px в 24px

            drawScaledCount(context, slotX, slotY, slotSize, have + "/" + need, color, 0.6f);
        }

        // Остальные ингредиенты
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            if (i != uniqueIdx) remaining.add(i);
        }

        for (int row = 0; row * CRAFT_MAX_PER_ROW < remaining.size(); row++) {
            int itemsInRow = Math.min(CRAFT_MAX_PER_ROW, remaining.size() - row * CRAFT_MAX_PER_ROW);
            int rowWidth = itemsInRow * slotSize + (itemsInRow - 1) * slotGap;
            int rowStartX = px + (panelW - rowWidth) / 2;
            int rowY = startIngY + 28 + row * (slotSize + slotGap + 6); // +28/6 компенсация для 24px

            for (int col = 0; col < itemsInRow; col++) {
                int ingIdx = remaining.get(row * CRAFT_MAX_PER_ROW + col);
                Ingredient ing = ingredients.get(ingIdx);
                ItemStack template = ing.getItem();
                int need = ing.getCount();
                int have = countPlayerItems(template, true);

                if (have < need) hasAllIngredients = false;

                int color = getIngredientColor(have, need, template, false);
                int slotX = rowStartX + col * (slotSize + slotGap);
                int slotY = rowY;

                if (color == 0xFFFF55) {
                    int buyPrice = 0;
                    for (Slot sl : handler.slots) {
                        if (sl.id >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT &&
                                ItemStack.areItemsEqual(sl.getStack(), template)) {
                            buyPrice = handler.getClientBuyPrice(sl.id);
                            break;
                        }
                    }
                    if (buyPrice > 0) {
                        // ✅ slotX и slotY УЖЕ абсолютные (рассчитаны от px/py, которые экранные)
                        // НЕ добавляй сюда x или y из render()!
                        craftBuyableSlots.add(new CraftBuySlotInfo(
                                new net.minecraft.client.util.math.Rect2i(slotX, slotY, slotSize, slotSize),
                                template, buyPrice
                        ));
                    }
                }

                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, slotSize, slotSize, 24, 24);
                context.drawItem(template, slotX + 4, slotY + 4);

                drawScaledCount(context, slotX, slotY, slotSize, have + "/" + need, color, 0.6f);
            }
        }

        // 5. Обновляем глобальное состояние кнопки
        craftEnabled = hasAllIngredients && canAfford;

        // 6. Стоимость
        int costY = py + panelH - 38;
        Text costText = Text.literal("Cost: " + currentRecipe.getCost() + " copper");
        context.getMatrices().push();
        context.getMatrices().translate(cx - (this.textRenderer.getWidth(costText) * 0.8f) / 2f, costY, 0);
        context.getMatrices().scale(0.8f, 0.8f, 1.0f);
        context.drawText(this.textRenderer, costText, 0, 0, 0x8E7D6E, false);
        context.getMatrices().pop();

        // 7. Кнопка "CRAFT"
        int btnW = 95;
        int btnH = 20;
        int btnX = cx - btnW / 2;
        int btnY = py + panelH - 28;

        int vOffset = craftEnabled ? 0 : 40; // 0=активна, 40=заблокирована
        context.drawTexture(BUTTON_TEX, btnX, btnY, 0, vOffset, btnW, btnH, BUTTON_TEX_W, BUTTON_TEX_H);
        context.drawText(this.textRenderer, Text.literal("CRAFT"), cx - this.textRenderer.getWidth("CRAFT") / 2, btnY + 6, craftEnabled ? 0xFFFFFF : 0x888888, false);

        craftButtonBounds = new net.minecraft.client.util.math.Rect2i(btnX, btnY, btnW, btnH);
    }

    private void drawScaledCount(DrawContext context, int slotX, int slotY, int slotSize, String text, int color, float scale) {
        context.getMatrices().push();
        // Сдвигаем в правый нижний угол слота
        context.getMatrices().translate(slotX + slotSize - 1, slotY + slotSize - 1, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        // Рисуем текст с отрицательными координатами, чтобы он "прижался" к углу
        context.drawText(this.textRenderer, Text.literal(text), -this.textRenderer.getWidth(text) - 2, -8, color, false);
        context.getMatrices().pop();
    }

    // Пересборка списка рецептов на основе серверных ID
    private void rebuildRecipesFromServerList() {
        System.out.println("[TradeOverhaul] rebuildRecipesFromServerList called");
        System.out.println("[TradeOverhaul] recipesReceivedFromServer: " + recipesReceivedFromServer);
        System.out.println("[TradeOverhaul] serverRecipeIds size: " + serverRecipeIds.size());

        if (!recipesReceivedFromServer || serverRecipeIds.isEmpty()) {
            return;
        }

        RecipeManager manager = RecipeManager.getInstance();
        availableRecipes.clear();

        for (String id : serverRecipeIds) {
            CraftRecipe recipe = manager.getCraftRecipeById(id);
            if (recipe != null) {
                System.out.println("[TradeOverhaul] Found recipe: " + id);
                availableRecipes.add(recipe);
            } else {
                System.out.println("[TradeOverhaul] Recipe NOT found: " + id);
            }
        }

        System.out.println("[TradeOverhaul] Total available recipes: " + availableRecipes.size());

        availableRecipes.sort((a, b) -> {
            int levelCompare = Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
            if (levelCompare != 0) return levelCompare;
            return a.getResult().getName().getString().compareToIgnoreCase(b.getResult().getName().getString());
        });

        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
            System.out.println("[TradeOverhaul] Recipe panel updated");
        } else {
            System.out.println("[TradeOverhaul] Recipe panel is null!");
        }
    }

    private String formatMoney(int copper) {
        int gold = copper / 10000;
        int silver = (copper % 10000) / 100;
        int remainingCopper = copper % 100;

        StringBuilder sb = new StringBuilder();

        if (gold > 0) {
            sb.append(gold).append("g");
            if (silver > 0 || remainingCopper > 0) sb.append(" ");
        }
        if (silver > 0) {
            sb.append(silver).append("s");
            if (remainingCopper > 0) sb.append(" ");
        }
        if (remainingCopper > 0 || (gold == 0 && silver == 0)) {
            sb.append(remainingCopper).append("c");
        }

        return sb.toString();
    }

    // Добавьте этот метод в класс VillagerInteractionScreen

    /**
     * Вызывается при получении списка ID рецептов от сервера
     * @param recipeIds Список ID рецептов, доступных для этого жителя
     */
    public void onAvailableRecipeIdsReceived(List<String> recipeIds) {
        System.out.println("[TradeOverhaul] onAvailableRecipeIdsReceived called with " + recipeIds.size() + " ids");
        if (recipeIds == null || recipeIds.isEmpty()) {
            System.out.println("[TradeOverhaul] Recipe ids list is empty!");
            return;
        }

        this.serverRecipeIds = new ArrayList<>(recipeIds);
        this.recipesReceivedFromServer = true;

        for (String id : recipeIds) {
            System.out.println("[TradeOverhaul] Processing recipe ID: " + id);
        }

        rebuildRecipesFromServerList();
    }

    @Override
    protected List<Text> getTooltipFromItem(ItemStack stack) {
        List<Text> list = super.getTooltipFromItem(stack);

        if (currentTab == TabType.TRADE && !stack.isEmpty()) {
            // Находим слот, который сейчас под курсором
            for (Slot slot : handler.slots) {
                if (slot.getStack() == stack && slot.x >= 0) {
                    int idx = slot.id;
                    boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
                    boolean ctrlPressed = net.minecraft.client.gui.screen.Screen.hasControlDown();

                    // Предметы жителя -> цена покупки
                    if (idx >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                        int pricePerOne = handler.getClientBuyPrice(idx);
                        if (pricePerOne > 0) {
                            int stackSize = stack.getCount();
                            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);

                            // Расчёт максимально возможного количества для покупки
                            int maxCanBuy = playerMoney / pricePerOne;
                            int maxFit = getMaxInventorySpaceForStack(stack);

                            int desiredAmount = 1;
                            if (ctrlPressed) {
                                desiredAmount = Math.min(10, stackSize);
                            } else if (shiftPressed) {
                                desiredAmount = stackSize;
                            }

                            // Ограничиваем деньгами и местом в инвентаре
                            int amount = Math.min(desiredAmount, Math.min(maxCanBuy, maxFit));
                            if (amount <= 0) amount = 1;

                            int totalPrice = pricePerOne * amount;
                            boolean canAfford = playerMoney >= totalPrice;
                            String color = canAfford ? "§a" : "§c";

                            String amountText = "";
                            if (amount == stackSize && (shiftPressed || (ctrlPressed && stackSize <= 10))) {
                                amountText = " (all)";
                            } else if (amount > 1) {
                                amountText = " (" + amount + ")";
                            }

                            String priceText = formatMoney(totalPrice);
                            list.add(Text.literal(color + "Buy" + amountText + ": " + priceText));
                        }
                    }
                    // Предметы игрока -> цена продажи
                    else if (idx >= VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX &&
                            idx < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                        if (handler.canVillagerBuyItem(stack)) {
                            int pricePerOne = handler.getClientSellPrice(stack);
                            if (pricePerOne > 0) {
                                int stackSize = stack.getCount();
                                int villagerMoney = handler.getSyncedWallet();

                                // Расчёт максимально возможного количества для продажи
                                int maxCanBuy = villagerMoney / pricePerOne;
                                int maxFit = getMaxVillagerSpaceForStack(stack);

                                int desiredAmount = 1;
                                if (ctrlPressed) {
                                    desiredAmount = Math.min(10, stackSize);
                                } else if (shiftPressed) {
                                    desiredAmount = stackSize;
                                }

                                // Ограничиваем деньгами жителя и местом в его инвентаре
                                int amount = Math.min(desiredAmount, Math.min(maxCanBuy, maxFit));
                                if (amount <= 0) amount = 1;

                                int totalPrice = pricePerOne * amount;
                                boolean canAfford = villagerMoney >= totalPrice;
                                String color = canAfford ? "§a" : "§c";

                                String amountText = "";
                                if (amount == stackSize && (shiftPressed || (ctrlPressed && stackSize <= 10))) {
                                    amountText = " (all)";
                                } else if (amount > 1) {
                                    amountText = " (" + amount + ")";
                                }

                                String priceText = formatMoney(totalPrice);
                                list.add(Text.literal(color + "Sell" + amountText + ": " + priceText));
                            }
                        }
                    }
                    break;
                }
            }
        }
        return list;
    }

    public void onAvailableRecipesReceived(List<CraftRecipe> recipes) {
        availableRecipes.clear();
        availableRecipes.addAll(recipes);

        availableRecipes.sort((a, b) -> {
            int levelCompare = Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
            if (levelCompare != 0) return levelCompare;
            return a.getResult().getName().getString().compareToIgnoreCase(b.getResult().getName().getString());
        });

        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
        }

        TradeOverhaulMod.LOGGER.info("[TradeOverhaul] Received {} full recipes from server", recipes.size());
    }

    public void refreshRecipesForCurrentVillager() {
        System.out.println("[TradeOverhaul] refreshRecipesForCurrentVillager called");

        RecipeManager manager = RecipeManager.getInstance();
        if (!manager.hasServerRecipes()) {
            System.out.println("[TradeOverhaul] No server recipes yet");
            return;
        }

        String professionId = handler.getProfessionId();

        System.out.println("[TradeOverhaul] Profession: " + professionId);
        System.out.println("[TradeOverhaul] Total server recipes: " + manager.getAllServerRecipes().size());

        allRecipes.clear();
        availableRecipes.clear();

        for (CraftRecipe recipe : manager.getAllServerRecipes()) {
            String recipeProfession = recipe.getProfession();
            // ❌ Убрана проверка на уровень - показываем все рецепты для профессии
            if (recipeProfession == null || recipeProfession.equals(professionId)) {
                allRecipes.add(recipe);
                System.out.println("[TradeOverhaul] Added recipe: " + recipe.getId() + " (level " + recipe.getRequiredLevel() + ")");
            }
        }

        // Сортировка по уровню, затем по имени
        allRecipes.sort((a, b) -> {
            int levelCompare = Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
            if (levelCompare != 0) return levelCompare;
            return a.getResult().getName().getString().compareToIgnoreCase(b.getResult().getName().getString());
        });

        availableRecipes.clear();
        availableRecipes.addAll(allRecipes);

        // Обновляем панель, если она уже создана
        if (recipeListPanel != null) {
            recipeListPanel.updateRecipes(availableRecipes);
            System.out.println("[TradeOverhaul] Updated existing recipeListPanel");
        } else {
            System.out.println("[TradeOverhaul] recipeListPanel is null, will be created later in createRecipeList()");
        }

        System.out.println("[TradeOverhaul] Final available recipes: " + availableRecipes.size());
    }

    private void renderRepairSlots(DrawContext context, int mouseX, int mouseY) {
        // Собираем повреждённые предметы с реальными индексами
        List<DamagedItem> allDamaged = new ArrayList<>();
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() > 0) {
                allDamaged.add(new DamagedItem(stack, i));
            }
        }

        // ✅ СОРТИРУЕМ список (опционально - можно сортировать по имени, урону или индексу)
        // Сортировка по индексу в инвентаре (естественный порядок)
        allDamaged.sort(Comparator.comparingInt(DamagedItem::getSlotIndex));

        int startX = inventoryX + INV_SLOT_START_X;
        int startY = inventoryY + INV_SLOT_START_Y;

        // Рисуем все 36 слотов сетки, но заполняем их ТОЛЬКО повреждёнными предметами по порядку
        int damagedIndex = 0;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotX = startX + col * SLOT_STEP;
                int slotY = startY + row * SLOT_STEP;

                // Фон слота (всегда рисуем)
                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_TEX_W, SLOT_TEX_H);

                // Берём следующий повреждённый предмет, если он есть
                if (damagedIndex < allDamaged.size()) {
                    DamagedItem item = allDamaged.get(damagedIndex);
                    ItemStack stack = item.getStack();

                    context.drawItem(stack, slotX + 1, slotY + 1);
                    context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);

                    // Выделение выбранного
                    if (currentRepairItem != null && currentRepairItem.getSlotIndex() == item.getSlotIndex()) {
                        context.drawTexture(ACTIVE_SLOT_TEX, slotX, slotY, 0, 0, STATE_SLOT_W, STATE_SLOT_H, STATE_SLOT_W, STATE_SLOT_H);
                    }

                    if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                        context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x30FFFFFF);
                        List<Text> tooltip = new ArrayList<>();
                        tooltip.add(stack.getName());

                        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                    }
                }

                damagedIndex++;
            }
        }
    }

    private void resetSelectedRepairItem() {
        currentRepairItem = null;
        repairEnabled = false;
        if (repairPanel != null) {
            repairPanel.resetSelectedItem();
        }
    }

    private boolean isPointOverRepairButtons(double mouseX, double mouseY) {
        if (repairButtonBounds != null &&
                mouseX >= repairButtonBounds.getX() && mouseX <= repairButtonBounds.getX() + repairButtonBounds.getWidth() &&
                mouseY >= repairButtonBounds.getY() && mouseY <= repairButtonBounds.getY() + repairButtonBounds.getHeight()) {
            return true;
        }
        if (repairAllButtonBounds != null &&
                mouseX >= repairAllButtonBounds.getX() && mouseX <= repairAllButtonBounds.getX() + repairAllButtonBounds.getWidth() &&
                mouseY >= repairAllButtonBounds.getY() && mouseY <= repairAllButtonBounds.getY() + repairAllButtonBounds.getHeight()) {
            return true;
        }
        return false;
    }

    // === Инициализация навигации ===
    private void setupNavigation() {
        tabNavigationOrder = new ArrayList<>(List.of(TabType.TRADE, TabType.CRAFT, TabType.DISASSEMBLE, TabType.REPAIR));
        if (!tabNavigationOrder.contains(currentTab)) {
            currentTab = tabNavigationOrder.get(0);
        }
        currentTabIndex = tabNavigationOrder.indexOf(currentTab);
        updateNavBounds();
    }

    // === Обновление координат навигации ===
    private void updateNavBounds() {
        if (tabNavigationOrder == null || tabNavigationOrder.isEmpty()) return;

        navY = NAV_TOP_OFFSET;

        // Получаем названия соседних вкладок
        int leftIndex = currentTabIndex - 1;
        int rightIndex = currentTabIndex + 1;
        if (leftIndex < 0) leftIndex = tabNavigationOrder.size() - 1;
        if (rightIndex >= tabNavigationOrder.size()) rightIndex = 0;

        leftTabName = tabNavigationOrder.get(leftIndex).name();
        rightTabName = tabNavigationOrder.get(rightIndex).name();

        // ✅ ФИКСИРОВАННОЕ РАССТОЯНИЕ ОТ ЦЕНТРА ДО СТРЕЛОК
        //int centerX = this.width / 2;
        int leftArrowX = centerX - NAV_ARROW_X_OFFSET;
        int rightArrowX = centerX + NAV_ARROW_X_OFFSET - 10;

        // Название текущей вкладки (центрируется, но с проверкой на перекрытие)
        String currentTabName = tabNavigationOrder.get(currentTabIndex).name();
        int currentTabWidth = this.textRenderer.getWidth(currentTabName);
        int titleX = centerX - currentTabWidth / 2;

        // Проверяем, не перекрывается ли название с левой стрелкой
        int leftArrowRightEdge = leftArrowX + NAV_ARROW_W + NAV_TITLE_GAP;
        if (titleX < leftArrowRightEdge) {
            titleX = leftArrowRightEdge;
        }

        // Проверяем, не перекрывается ли название с правой стрелкой
        int rightArrowLeftEdge = rightArrowX - NAV_TITLE_GAP;
        if (titleX + currentTabWidth > rightArrowLeftEdge) {
            titleX = rightArrowLeftEdge - currentTabWidth;
        }

        // Сохраняем позиции
        navLeftBounds = new net.minecraft.client.util.math.Rect2i(leftArrowX, navY, NAV_ARROW_W, NAV_ARROW_H);
        navTitleX = titleX;
        navRightBounds = new net.minecraft.client.util.math.Rect2i(rightArrowX, navY, NAV_ARROW_W, NAV_ARROW_H);

        // КРУЖКИ (центрируем относительно центра экрана)
        int dotCount = tabNavigationOrder.size();
        int dotCharWidth = this.textRenderer.getWidth("●");
        int dotCharHeight = this.textRenderer.fontHeight;
        int totalDotsWidth = dotCount * dotCharWidth + (dotCount - 1) * NAV_DOT_GAP;
        int dotStartX = centerX - totalDotsWidth / 2;
        int dotY = navY + NAV_ARROW_H + NAV_DOTS_Y_OFFSET;

        navDotBounds = new ArrayList<>();
        for (int i = 0; i < dotCount; i++) {
            int dotX = dotStartX + i * (dotCharWidth + NAV_DOT_GAP);
            navDotBounds.add(new net.minecraft.client.util.math.Rect2i(dotX, dotY, dotCharWidth, dotCharHeight));
        }
    }

    // === Переключение вкладки ===
    private void switchTab(int newIndex) {
        if (newIndex < 0) newIndex = tabNavigationOrder.size() - 1;
        if (newIndex >= tabNavigationOrder.size()) newIndex = 0;

        currentTabIndex = newIndex;
        currentTab = tabNavigationOrder.get(newIndex);

        updateNavBounds(); // ✅ Обновляем координаты под новую длину названия
        onTabSelected(currentTab);
    }

    // === Отрисовка навигации ===
    private void renderTabNavigation(DrawContext context, int mouseX, int mouseY, float delta) {
        if (tabNavigationOrder == null || tabNavigationOrder.isEmpty()) return;

        String currentTabName = tabNavigationOrder.get(currentTabIndex).name();

        // Левая стрелка
        boolean leftH = isOver(mouseX, mouseY, navLeftBounds);
        int leftVOffset = leftH ? NAV_ARROW_TEX_H : 0; // Смещение в текстуре на высоту одного состояния (20 пикселей)
        context.drawTexture(NAV_ARROW_LEFT_TEX,
                navLeftBounds.getX(), navLeftBounds.getY(),
                0, leftVOffset,                          // Координаты в текстуре
                10, 10,                // Размер на экране
                10, 20); // Реальный размер текстуры

        // Правая стрелка
        boolean rightH = isOver(mouseX, mouseY, navRightBounds);
        int rightVOffset = rightH ? NAV_ARROW_TEX_H : 0;
        context.drawTexture(NAV_ARROW_RIGHT_TEX,
                navRightBounds.getX(), navRightBounds.getY(),
                0, rightVOffset,
                10, 10,
                10, 20);

        // Название текущей вкладки
        context.drawText(textRenderer, Text.literal(currentTabName), navTitleX, navY + 2, 0xFFFFFF, false);

        // Рисуем кружки
        for (int i = 0; i < navDotBounds.size(); i++) {
            var bounds = navDotBounds.get(i);
            boolean isHovered = isOver(mouseX, mouseY, bounds);
            boolean isActive = (i == currentTabIndex);

            int color;
            if (isActive) {
                color = 0xFFD4AF37;
            } else if (isHovered) {
                color = 0xFFAAAAAA;
            } else {
                color = 0xFF444444;
            }

            context.drawText(textRenderer, Text.literal("●"), bounds.getX(), bounds.getY(), color, false);
        }

        // Названия соседних вкладок
        int dotY = navY + NAV_ARROW_H + NAV_DOTS_Y_OFFSET;
        int dotCharHeight = this.textRenderer.fontHeight;
        int textBaseY = dotY + (dotCharHeight - (int)(this.textRenderer.fontHeight * NAV_SIDE_TEXT_SCALE)) / 2 + NAV_TEXT_VERTICAL_OFFSET;

        // Левое название
        if (leftTabName != null && !leftTabName.isEmpty()) {
            int leftTextWidth = this.textRenderer.getWidth(leftTabName);
            int scaledWidth = (int)(leftTextWidth * NAV_SIDE_TEXT_SCALE);
            int textX = navLeftBounds.getX() - scaledWidth - NAV_SIDE_TEXT_OFFSET;

            context.getMatrices().push();
            context.getMatrices().translate(textX, textBaseY, 0);
            context.getMatrices().scale(NAV_SIDE_TEXT_SCALE, NAV_SIDE_TEXT_SCALE, 1.0f);

            int color = 0x8E7D6E;

            context.drawText(textRenderer, Text.literal(leftTabName), 0, 0, color, false);
            context.getMatrices().pop();
        }

        // Правое название
        if (rightTabName != null && !rightTabName.isEmpty()) {
            int rightTextWidth = this.textRenderer.getWidth(rightTabName);
            int scaledWidth = (int)(rightTextWidth * NAV_SIDE_TEXT_SCALE);
            int textX = navRightBounds.getX() + NAV_ARROW_W + NAV_SIDE_TEXT_OFFSET - 10;

            context.getMatrices().push();
            context.getMatrices().translate(textX, textBaseY, 0);
            context.getMatrices().scale(NAV_SIDE_TEXT_SCALE, NAV_SIDE_TEXT_SCALE, 1.0f);

            int color = 0x8E7D6E;

            context.drawText(textRenderer, Text.literal(rightTabName), 0, 0, color, false);
            context.getMatrices().pop();
        }
    }

    // === Хелпер: проверка попадания мыши в прямоугольник ===
    private boolean isOver(double mx, double my, net.minecraft.client.util.math.Rect2i r) {
        return mx >= r.getX() && mx < r.getX() + r.getWidth() && my >= r.getY() && my < r.getY() + r.getHeight();
    }

    private void calculateScaleFactor() {
        currentGuiScale = (int) client.getWindow().getScaleFactor();

        // Базовый размер для GUI scale 3
        int baseWidth = BASE_PANEL_WIDTH;
        int baseHeight = BASE_PANEL_HEIGHT;

        // Коэффициенты для разных GUI scale (относительно scale 3)
        float sizeMultiplier;
        float gapMultiplier;

        switch (currentGuiScale) {
            case 2:
                // На scale 2 делаем панели чуть меньше, чем пропорционально
                sizeMultiplier = 1.0f;  // 129 * 0.6 = 77
                gapMultiplier = 0.8f;   // 200 * 0.6 = 120
                break;
            case 3:
                sizeMultiplier = 1.0f;  // 129 * 1.0 = 129
                gapMultiplier = 1.0f;   // 200 * 1.0 = 200
                break;
            case 4:
                // На scale 4 делаем панели меньше, чем пропорционально (чтобы не занимали весь экран)
                sizeMultiplier = 1.0f;  // 129 * 0.8 = 103
                gapMultiplier = 0.8f;   // 200 * 0.7 = 140
                break;
            default:
                // Для других масштабов используем линейную интерполяцию
                float scaleFactor = currentGuiScale / 3.0f;
                sizeMultiplier = Math.min(1.2f, Math.max(0.5f, scaleFactor * 0.8f));
                gapMultiplier = Math.min(1.2f, Math.max(0.5f, scaleFactor * 0.7f));
                break;
        }

        panelWidth = (int) (baseWidth * sizeMultiplier);
        panelHeight = (int) (baseHeight * sizeMultiplier);
        gapBetweenPanels = (int) (BASE_GAP_BETWEEN_PANELS * gapMultiplier);

        // Минимальные и максимальные размеры (чтобы не было слишком маленько или слишком большо)
        panelWidth = Math.max(70, Math.min(180, panelWidth));
        panelHeight = Math.max(100, Math.min(260, panelHeight));
        gapBetweenPanels = Math.max(60, Math.min(250, gapBetweenPanels));

        // Отладочный вывод
        System.out.println("=== GUI Scale: " + currentGuiScale + " ===");
        System.out.println("Panel size: " + panelWidth + "x" + panelHeight);
        System.out.println("Gap between panels: " + gapBetweenPanels);
    }

    private void calculatePanelPositions() {
        centerX = this.width / 2;
        centerY = this.height / 2;

        // Левая панель (игрок)
        playerPanelX = centerX - panelWidth - gapBetweenPanels / 2;
        playerPanelY = centerY - panelHeight / 2;

        // Правая панель (житель)
        villagerPanelX = centerX + gapBetweenPanels / 2;
        villagerPanelY = centerY - panelHeight / 2;

        // Отладочный вывод
        System.out.println("Player panel: " + playerPanelX + ", " + playerPanelY);
        System.out.println("Villager panel: " + villagerPanelX + ", " + villagerPanelY);
    }

    private void drawPanelBackgrounds(DrawContext context) {
        // Панель игрока
        context.drawTexture(INV_BG_TEX,
                playerPanelX, playerPanelY,
                0, 0, invBgW, invBgH, invBgW, invBgH);

        // Надпись "INVENTORY" на панели игрока
        context.getMatrices().push();
        context.getMatrices().translate(playerPanelX + invLabelX, playerPanelY + invLabelY, 0);
        float textScale = 0.8f * scaleFactor;
        context.getMatrices().scale(textScale, textScale, 1.0f);
        context.drawText(this.textRenderer, Text.literal("INVENTORY"), 0, 0, 0xFFFFFF, false);
        context.getMatrices().pop();

        // Панель жителя (только для вкладки TRADE)
        if (currentTab == TabType.TRADE) {
            context.drawTexture(INV_BG_TEX,
                    villagerPanelX, villagerPanelY,
                    0, 0, invBgW, invBgH, invBgW, invBgH);

            // Рисуем слоты жителя
            int villagerStartX = villagerPanelX + invSlotStartX;
            int villagerStartY = villagerPanelY + invSlotStartY;
            for (int row = 0; row < BASE_GRID_ROWS; row++) {
                for (int col = 0; col < BASE_GRID_COLS; col++) {
                    int slotX = villagerStartX + col * slotStep;
                    int slotY = villagerStartY + row * slotStep;
                    context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0,
                            slotSize, slotSize, SLOT_TEX_W, SLOT_TEX_H);
                }
            }
        }

        // Рисуем слоты игрока (всегда)
        int playerStartX = playerPanelX + invSlotStartX;
        int playerStartY = playerPanelY + invSlotStartY;

        // Броня
        int armorRowWidth = (5 * slotSize) + (4 * (slotStep - slotSize));
        int armorStartX = playerPanelX + (invBgW - armorRowWidth) / 2;
        int armorY = playerStartY + (BASE_GRID_ROWS * slotStep) + (int) (4 * scaleFactor);
        for (int i = 0; i < 5; i++) {
            int slotX = armorStartX + i * slotStep;
            context.drawTexture(SLOT_TEX, slotX, armorY, 0, 0,
                    slotSize, slotSize, SLOT_TEX_W, SLOT_TEX_H);
        }

        // Инвентарь
        for (int row = 0; row < BASE_GRID_ROWS; row++) {
            for (int col = 0; col < BASE_GRID_COLS; col++) {
                int slotX = playerStartX + col * slotStep;
                int slotY = playerStartY + row * slotStep;
                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0,
                        slotSize, slotSize, SLOT_TEX_W, SLOT_TEX_H);
            }
        }
    }

    private void drawPanels(DrawContext context) {
        // ТОЛЬКО ФОНЫ ПАНЕЛЕЙ, БЕЗ СЛОТОВ!

        // Панель игрока
        context.drawTexture(INV_BG_TEX,
                playerPanelX, playerPanelY,
                0, 0, panelWidth, panelHeight,
                BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT);

        // Надпись "INVENTORY"
        context.getMatrices().push();
        context.getMatrices().translate(
                playerPanelX + (int)(BASE_INV_LABEL_X * scaleFactor),
                playerPanelY + (int)(BASE_INV_LABEL_Y * scaleFactor),
                0
        );
        float textScale = 0.8f * scaleFactor;
        context.getMatrices().scale(textScale, textScale, 1.0f);
        context.drawText(this.textRenderer, Text.literal("INVENTORY"), 0, 0, 0xFFFFFF, false);
        context.getMatrices().pop();

        // Панель жителя (только для TRADE)
        if (currentTab == TabType.TRADE) {
            context.drawTexture(INV_BG_TEX,
                    villagerPanelX, villagerPanelY,
                    0, 0, panelWidth, panelHeight,
                    BASE_PANEL_WIDTH, BASE_PANEL_HEIGHT);
        }
    }

    private void sendTradeRequest(int slotIndex, int amount, boolean buying) {
        var buf = PacketByteBufs.create();
        buf.writeInt(handler.syncId);
        buf.writeInt(slotIndex);
        buf.writeInt(amount);
        buf.writeBoolean(buying);
        ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "trade_request"), buf);
    }

    // Метод для обновления предварительного опыта
    private void updateExpectedXp() {
        if (tradePanel != null && tradePanel.isVisible()) {
            int slotIndex = tradePanel.getSlotIndex();
            int amount = tradePanel.getCurrentAmount();
            if (tradePanel.isBuying()) {
                this.expectedXp = handler.getExpectedXpForBuy(slotIndex, amount);
            } else {
                ItemStack stack = handler.getSlot(slotIndex).getStack();
                this.expectedXp = handler.getExpectedXpForSell(stack, amount);
            }
        } else {
            this.expectedXp = 0f;
        }
    }

    private boolean isPointOverTradePanel(double mouseX, double mouseY) {
        if (tradePanel == null || !tradePanel.isVisible()) return false;

        // Получаем границы панели
        int panelX = tradePanel.getX();
        int panelY = tradePanel.getY();
        int panelWidth = tradePanel.getWidth();
        int panelHeight = tradePanel.getHeight();

        return mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= panelY && mouseY <= panelY + panelHeight;
    }

    private void openTradePanel(Slot slot) {
        if (slot == null || !slot.hasStack()) return;

        int slotIndex = slot.id;
        ItemStack stack = slot.getStack();

        boolean buying = slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT;
        int price = buying ? handler.getClientBuyPrice(slotIndex) : handler.getClientSellPrice(stack);

        if (price <= 0) return;

        int maxAmount = stack.getCount();
        if (buying) {
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
            maxAmount = Math.min(maxAmount, playerMoney / price);
        } else {
            int villagerMoney = handler.getSyncedWallet();
            maxAmount = Math.min(maxAmount, villagerMoney / price);
        }

        hasSelectedTradeSlot = true;
        selectedTradeSlotIndex = slotIndex;

        int panelCenterX = (villagerPanelX + playerPanelX + panelWidth) / 2;
        int panelCenterY = this.height / 2;

        tradePanel.open(stack, slotIndex, price, maxAmount, buying);
        tradePanel.setPosition(panelCenterX, panelCenterY);

        updateExpectedXp();
    }

    // В VillagerInteractionScreen.java
    public void refreshVillagerSlots() {
        // Принудительно обновляем слоты жителя
        if (handler != null) {
            handler.sendContentUpdates();
            // Перерисовываем слоты через positionSlots
            positionSlots();
        }
    }
}