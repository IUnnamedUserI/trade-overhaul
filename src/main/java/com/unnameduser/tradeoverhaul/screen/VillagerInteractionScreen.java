package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.network.RepairAllRequestC2SPacket;
import com.unnameduser.tradeoverhaul.common.network.RepairRequestC2SPacket;
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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VillagerInteractionScreen extends HandledScreen<VillagerCraftingScreenHandler> {

    private static final int BG_COLOR_TOP = 0xC6C6C6;
    private static final int BG_COLOR_BOTTOM = 0x8B8B8B;
    private static final int BORDER_COLOR = 0xFF555555;

    private int selectedInventoryIndex = -1;
    private int selectedItemSlot = -1;
    private ItemStack selectedItemStack = ItemStack.EMPTY;
    private boolean hasSelectedItem = false;
    private boolean craftEnabled = false;

    private static final int ARMOR_SLOT_WIDTH = 18;
    private static final int ARMOR_SLOT_COUNT = 5;
    private static final int ARMOR_STEP = 20;
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 6;

    private int armorX;
    private int inventoryX;
    private int inventoryY;
    private int centerX;
    private int recipesX;
    private int panelY;
    private int titleY;

    private Text playerInventoryLabel;
    private TabType currentTab = TabType.TRADE;

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

    private static final Identifier NUM_GOLD = new Identifier("numismatic_overhaul", "textures/item/gold_coin.png");
    private static final Identifier NUM_SILVER = new Identifier("numismatic_overhaul", "textures/item/silver_coin.png");
    private static final Identifier NUM_COPPER = new Identifier("numismatic_overhaul", "textures/item/bronze_coin.png");

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

        this.tradePanelX = this.inventoryX + INV_BG_W + 120; // 50
        this.tradePanelY = inventoryY;

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

        refreshDamagedItems();
        createRepairPanel();
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

        int relInvX = inventoryX - bgX;
        int relInvY = inventoryY - bgY;
        int relTradeX = tradePanelX - bgX;
        int relTradeY = tradePanelY - bgY;

        int invStartX = relInvX + INV_SLOT_START_X;
        int invStartY = relInvY + INV_SLOT_START_Y;

        int armorRowWidth = (5 * SLOT_SIZE) + (4 * SLOT_GAP);
        int armorStartX = relInvX + (INV_BG_W - armorRowWidth) / 2;
        int armorY = invStartY + (GRID_ROWS * SLOT_STEP) + ARMOR_Y_OFFSET;

        int armorSlotIndex = 0;
        int invSlotIndex = 0;
        int tradeSlotIndex = 0;

        for (int i = 0; i < this.handler.slots.size(); i++) {
            var slotObj = this.handler.slots.get(i);

            if (currentTab == TabType.REPAIR) {
                slotObj.x = -1000; slotObj.y = -1000; continue;
            }

            if (i < VillagerCraftingScreenHandler.FIRST_MAIN_GRID_SLOT_INDEX) {
                // Броня +1/+1
                slotObj.x = armorStartX + armorSlotIndex * SLOT_STEP + 1;
                slotObj.y = armorY + 1;
                armorSlotIndex++;
            } else if (i < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                // Инвентарь игрока +1/+1
                int col = invSlotIndex % GRID_COLS;
                int row = invSlotIndex / GRID_COLS;
                slotObj.x = invStartX + col * SLOT_STEP + 1;
                slotObj.y = invStartY + row * SLOT_STEP + 1;
                invSlotIndex++;
            } else {
                if (currentTab == TabType.TRADE) {
                    int col = tradeSlotIndex % GRID_COLS;
                    int row = tradeSlotIndex / GRID_COLS;
                    // ✅ +1 к X и Y, чтобы кликабельная зона совпала со сдвигом текстур
                    slotObj.x = relTradeX + INV_SLOT_START_X + col * SLOT_STEP + 1;
                    slotObj.y = relTradeY + INV_SLOT_START_Y + row * SLOT_STEP + 1;
                    tradeSlotIndex++;
                } else {
                    slotObj.x = -1000; slotObj.y = -1000;
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
        if (currentTab == tab) return;
        currentTab = tab;

        // ✅ Полностью сбрасываем состояние разборки при уходе с вкладки
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
        float expectedXp = getExpectedXpForCurrentHover();

        if (progress < 1.0f && xpForNext > 0) {
            int barWidth = GRID_COLS * SLOT_STEP;
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

        int wallet = handler.getSyncedWallet();
        var villagerCurrency = new com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent();
        villagerCurrency.setTotalCopper(wallet);
        context.drawText(this.textRenderer, villagerCurrency.formatMoneyVertical(), villagerX, y + this.panelY + 5, 0xFFFFFF, true);
    }

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
            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
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
            // ✅ ГРУППИРОВКА ПРЕДМЕТОВ
            // Используем LinkedHashMap, чтобы сохранить относительный порядок ингредиентов
            Map<Item, Integer> itemCounts = new LinkedHashMap<>();
            int totalParts = 0;

            for (net.minecraft.recipe.Ingredient ing : recipe.getIngredients()) {
                ItemStack[] matches = ing.getMatchingStacks();
                if (matches != null && matches.length > 0) {
                    Item item = matches[0].getItem();
                    // Суммируем одинаковые предметы
                    itemCounts.put(item, itemCounts.getOrDefault(item, 0) + 1);
                    totalParts++;
                }
            }

            // Преобразуем карту обратно в список стаков с нужным количеством
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                ItemStack groupStack = new ItemStack(entry.getKey(), entry.getValue());
                disassemblyComponents.add(groupStack);
            }

            // Стоимость = (Всего компонентов) * 5
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
        if (currentTab == TabType.TRADE) {
            this.renderBackground(context);
            super.render(context, mouseX, mouseY, delta);

            int x = (this.width - this.backgroundWidth) / 2;
            int y = (this.height - this.backgroundHeight) / 2;

            this.drawMouseoverTooltip(context, mouseX, mouseY);
            drawTradePanel(context, mouseX, mouseY, delta);

            int playerMoney = NumismaticHelper.getTotalMoney(this.client.player);
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

            // Рисуем центральную панель разборки поверх остального
            renderDisassemblyPanel(context, mouseX, mouseY);
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
                int centeredX = startX - (itemNameWidth / 2);
                context.drawText(this.textRenderer, itemName, centeredX, startY, 0xFFFFFF, false);

                int durabilityPercent = (int) ((1.0 - (double) selected.getCurrentDamage() / selected.getMaxDamage()) * 100);
                String durabilityText = "Durability";
                context.drawText(this.textRenderer, Text.literal(durabilityText), centeredX, startY + 55, 0xCCCCCC, false);
                String durabilityPercentText = String.format("%d%%", durabilityPercent);
                context.drawText(this.textRenderer, Text.literal(durabilityPercentText), centeredX, startY + 65, 0xCCCCCC, false);

                String costText = String.format("Cost: %d copper", selected.getRepairCost());
                context.drawText(this.textRenderer, Text.literal(costText), centeredX - (this.textRenderer.getWidth(costText) / 2), startY + 85, 0xFFFFAA, false);

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
        // x, y — это координаты левого-верхнего угла GUI-фона (передаются из drawBackground)

        // === ФОН ПАНЕЛИ ИГРОКА ===
        int px = inventoryX;
        int py = inventoryY;
        context.drawTexture(INV_BG_TEX, px, py, 0, 0, INV_BG_W, INV_BG_H, INV_BG_W, INV_BG_H);

        // Надпись "INVENTORY" (уменьшенная)
        context.getMatrices().push();
        context.getMatrices().translate(px + INV_LABEL_X, py + INV_LABEL_Y, 0);
        context.getMatrices().scale(0.8f, 0.8f, 1.0f);
        context.drawText(this.textRenderer, Text.literal("INVENTORY"), 0, 0, 0xFFFFFF, false);
        context.getMatrices().pop();

        // === ФОН ПАНЕЛИ ЖИТЕЛЯ (только Trade, на одном уровне с игроком) ===
        if (currentTab == TabType.TRADE) {
            context.drawTexture(INV_BG_TEX, tradePanelX, tradePanelY, 0, 0, INV_BG_W, INV_BG_H, INV_BG_W, INV_BG_H);
        }

        // === БАЗОВЫЕ ТЕКСТУРЫ СЛОТОВ (DEFAULT) ===

        // 1. Броня (5 слотов, центрированы)
        int armorRowWidth = (5 * SLOT_SIZE) + (4 * SLOT_GAP);
        int armorStartX = px + (INV_BG_W - armorRowWidth) / 2;
        int armorY = py + INV_SLOT_START_Y + (GRID_ROWS * SLOT_STEP) + ARMOR_Y_OFFSET;
        for (int i = 0; i < 5; i++) {
            int slotX = armorStartX + i * SLOT_STEP;
            context.drawTexture(SLOT_TEX, slotX, armorY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_TEX_W, SLOT_TEX_H);
        }

        // 2. Инвентарь игрока (6×6)
        int invStartX = px + INV_SLOT_START_X;
        int invStartY = py + INV_SLOT_START_Y;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotX = invStartX + col * SLOT_STEP;
                int slotY = invStartY + row * SLOT_STEP;
                context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_TEX_W, SLOT_TEX_H);
            }
        }

        // 3. ✅ ИНВЕНТАРЬ ЖИТЕЛЯ (6×6, только Trade) — ИСПРАВЛЕНИЕ: теперь рисуется!
        if (currentTab == TabType.TRADE) {
            int tradeStartX = tradePanelX + INV_SLOT_START_X;
            int tradeStartY = tradePanelY + INV_SLOT_START_Y;
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int slotX = tradeStartX + col * SLOT_STEP;
                    int slotY = tradeStartY + row * SLOT_STEP;
                    context.drawTexture(SLOT_TEX, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_TEX_W, SLOT_TEX_H);
                }
            }
        }

        // === СТАТУСНЫЕ ОВЕРЛЕИ (ACTIVE/LOCKED) ===
        for (Slot slot : handler.slots) {
            if (slot.x < 0) continue; // Пропускаем скрытые слоты

            SlotState state = getSlotState(slot.id, slot);
            if (state == SlotState.DEFAULT) continue;

            Identifier tex = (state == SlotState.ACTIVE) ? ACTIVE_SLOT_TEX : LOCKED_SLOT_TEX;

            // ✅ Сдвиг +1/+1: оверлеи рисуются поверх базового слота с компенсацией
            context.drawTexture(tex, x + slot.x - 1, y + slot.y - 1, 0, 0, STATE_SLOT_W, STATE_SLOT_H, STATE_SLOT_W, STATE_SLOT_H);
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
        // ЛКМ обработка
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

            // Обработка кнопки DISASSEMBLE
            if (isDisassemblyActive && disassembleButtonBounds != null) {
                int btnRight = disassembleButtonBounds.getX() + disassembleButtonBounds.getWidth();
                int btnBottom = disassembleButtonBounds.getY() + disassembleButtonBounds.getHeight();
                if (mouseX >= disassembleButtonBounds.getX() && mouseX <= btnRight &&
                        mouseY >= disassembleButtonBounds.getY() && mouseY <= btnBottom) {

                    isDisassembleButtonPressed = true;

                    boolean canAfford = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(client.player) >= disassemblyCost;
                    if (canAfford && disassemblyTargetSlotIndex != -1) {
                        var buf = PacketByteBufs.create();
                        buf.writeInt(handler.syncId);
                        buf.writeInt(disassemblyTargetSlotIndex);
                        ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "disassemble_request"), buf);

                        // ✅ МГНОВЕННЫЙ СБРОС UI
                        isDisassemblyActive = false;
                        disassemblyTargetSlotIndex = -1;
                        disassemblyComponents.clear();
                        disassemblyTarget = ItemStack.EMPTY;
                        return true;
                    }
                }
            }

            if (currentTab == TabType.CRAFT) {
                boolean clickedOnRecipePanel = isPointOverRecipePanel(mouseX, mouseY);
                boolean clickedOnCraftButton = isPointOverCraftButton(mouseX, mouseY);
                boolean clickedOnSlot = getSlotAt(mouseX, mouseY) != null;
                boolean clickedOnSearchField = searchField != null && mouseX >= searchField.getX() && mouseX <= searchField.getX() + searchField.getWidth() &&
                        mouseY >= searchField.getY() && mouseY <= searchField.getY() + searchField.getHeight();
                boolean clickedOnDisPanel = isDisassemblyActive && disassemblyTarget != null;

                if (!clickedOnRecipePanel && !clickedOnCraftButton && !clickedOnSlot && !clickedOnSearchField && !clickedOnDisPanel) {
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

        // ПКМ обработка
        if (button == 1) {
            if (currentTab == TabType.CRAFT) {
                Slot hoveredSlot = getSlotAt(mouseX, mouseY);
                if (hoveredSlot != null && hoveredSlot.id < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                    ItemStack stack = hoveredSlot.getStack();
                    if (!stack.isEmpty()) {
                        // Выбор предмета (всегда)
                        this.selectedItemSlot = hoveredSlot.id;
                        this.selectedItemStack = stack.copy();
                        this.hasSelectedItem = true;
                        this.selectedInventoryIndex = hoveredSlot.getIndex();
                        filterRecipesByItem(stack, hoveredSlot.id); // Фильтр применяется после выбора
                        return true;
                    }
                }
                return true;
            }

            if (currentTab == TabType.DISASSEMBLE) {
                Slot hoveredSlot = getSlotAt(mouseX, mouseY);
                if (hoveredSlot != null && hoveredSlot.id < VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                    ItemStack stack = hoveredSlot.getStack();
                    if (!stack.isEmpty() && DisassemblyConfig.isDisassemblyAllowed(stack.getItem())) {
                        openDisassemblyPreview(stack);
                        return true;
                    } else if (!stack.isEmpty()) {
                        return true;
                    }
                }
            }

            if (currentTab == TabType.TRADE) {
                Slot clickedSlot = getSlotAt(mouseX, mouseY);
                if (clickedSlot != null) {
                    int slotIndex = clickedSlot.id;
                    ItemStack stack = clickedSlot.getStack();
                    if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
                    boolean buying = slotIndex >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT;
                    if (buying && !handler.clientHasInventorySpaceForStack(stack, 1)) return true;
                    boolean shiftPressed = net.minecraft.client.gui.screen.Screen.hasShiftDown();
                    boolean ctrlPressed = net.minecraft.client.gui.screen.Screen.hasControlDown();
                    var buf = PacketByteBufs.create();
                    buf.writeInt(handler.syncId);
                    buf.writeInt(slotIndex);
                    buf.writeBoolean(buying);
                    buf.writeBoolean(shiftPressed && !buying);
                    buf.writeBoolean(shiftPressed && buying);
                    buf.writeBoolean(ctrlPressed);
                    ClientPlayNetworking.send(new Identifier(TradeOverhaulMod.MOD_ID, "trade_action"), buf);
                    return true;
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
        int panelX = x + this.recipesX - 30;
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
        isDisassembleButtonPressed = false;
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
        if (item == null) { repairEnabled = false; return; }
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
        if (!isDisassemblyActive || disassemblyTarget.isEmpty()) return;

        int cx = this.width / 2;
        int cy = this.height / 2; // Центр экрана по Y
        int panelW = 125;
        int panelH = 190;
        int px = cx - panelW / 2;
        int py = cy - panelH / 2;

        // Фон панели (если нужен текстурный, раскомментируй строку ниже)
        context.drawTexture(PANEL_TEX, px, py, 0, 0, panelW, panelH, PANEL_TEX_W, PANEL_TEX_H);

        // === ФОН РАЗБИРАЕМОГО ПРЕДМЕТА ===
        int itemBgX = cx - ITEM_BG_W / 2;
        int itemBgY = py + 22;
        context.drawTexture(ITEM_BG_TEX, itemBgX, itemBgY, 0, 0, ITEM_BG_W, ITEM_BG_H, ITEM_BG_W, ITEM_BG_H);

        // === САМ ПРЕДМЕТ (поверх фона) ===
        context.getMatrices().push();
        context.getMatrices().translate(itemBgX + ITEM_BG_W / 2, itemBgY + ITEM_BG_H / 2, 0);
        context.getMatrices().scale(3.0f, 3.0f, 1.0f); // 16px * 3 = 48px
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

        // === ИНГРЕДИЕНТЫ (Центрирование по строкам) ===
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

        // === КНОПКА (3 состояния: 0=Обычное, 1=Наведение, 2=Нажатие) ===
        int btnW = 95;  // Ширина кнопки = ширине текстуры
        int btnH = 20;  // Высота одного состояния
        int btnX = cx - btnW / 2;
        int btnY = py + panelH - 25;

        boolean canAfford = com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper.getTotalMoney(this.client.player) >= disassemblyCost;
        boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        // Выбор состояния
        int stateIndex = 0;
        if (!canAfford) {
            stateIndex = 2; // Или оставь 0, если хочешь, чтобы заблокированная выглядела как обычная
        } else if (isDisassembleButtonPressed) {
            stateIndex = 2;
        } else if (isHovered) {
            stateIndex = 1;
        }

        int vOffset = stateIndex * BTN_STATE_H;
        // ✅ КЛЮЧЕВОЙ МОМЕНТ: width/height = 95/20, textureWidth/textureHeight = 95/60
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

    @Override
    protected List<Text> getTooltipFromItem(ItemStack stack) {
        List<Text> list = super.getTooltipFromItem(stack);

        if (currentTab == TabType.TRADE && !stack.isEmpty()) {
            // Находим слот, который сейчас под курсором
            for (Slot slot : handler.slots) {
                if (slot.getStack() == stack && slot.x >= 0) {
                    int idx = slot.id;

                    // Предметы жителя -> цена покупки
                    if (idx >= VillagerCraftingScreenHandler.FIRST_VILLAGER_TRADE_SLOT) {
                        int price = handler.getClientBuyPrice(idx);
                        if (price > 0) {
                            list.add(Text.literal("§aBuy: " + price + " copper"));
                        }
                    }
                    // Предметы игрока -> цена продажи
                    else {
                        if (handler.canVillagerBuyItem(stack)) {
                            int price = handler.getClientSellPrice(stack);
                            if (price > 0) {
                                list.add(Text.literal("§eSell: " + price + " copper"));
                            }
                        }
                    }
                    break;
                }
            }
        }
        return list;
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
}