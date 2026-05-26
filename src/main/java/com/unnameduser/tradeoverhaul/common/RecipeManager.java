package com.unnameduser.tradeoverhaul.common;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.dto.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.item.Items;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RecipeManager {
    private static RecipeManager instance;
    private Map<String, CraftRecipe> craftRecipes = new HashMap<>();
    private Map<String, DisassemblyRecipe> disassemblyRecipes = new HashMap<>();
    private Map<String, List<CraftRecipe>> professionCrafts = new HashMap<>();
    private Map<String, List<DisassemblyRecipe>> professionDisassembly = new HashMap<>();

    private List<CraftRecipe> allServerRecipes = new ArrayList<>();
    private boolean serverRecipesLoaded = false;

    private RecipeManager() {}

    public static RecipeManager getInstance() {
        if (instance == null) instance = new RecipeManager();
        return instance;
    }

    public void loadRecipes(MinecraftServer server) {
        craftRecipes.clear();
        disassemblyRecipes.clear();
        professionCrafts.clear();
        professionDisassembly.clear();

        // Путь для крафтов
        Path craftConfigDir = server.getRunDirectory().toPath()
                .resolve("config/tradeoverhaul/crafts");

        // Загружаем рецепты крафта
        if (!Files.exists(craftConfigDir)) {
            TradeOverhaulMod.LOGGER.info("[TradeOverhaul] Crafts folder not found on client, creating...");
            try {
                Files.createDirectories(craftConfigDir);
                createExampleCraftConfig(craftConfigDir);
            } catch (IOException e) {
                TradeOverhaulMod.LOGGER.error("[TradeOverhaul] Failed to create client craft config dir", e);
            }
        } else {
            loadCraftRecipesFromDir(craftConfigDir);
        }

        // Путь для разборки (пока не используется)
        Path disassemblyConfigDir = server.getRunDirectory().toPath()
                .resolve("config/tradeoverhaul/disassembly");

        if (!Files.exists(disassemblyConfigDir)) {
            try {
                Files.createDirectories(disassemblyConfigDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            loadDisassemblyRecipesFromDir(disassemblyConfigDir);
        }

        System.out.println("[TradeOverhaul] Loaded " + craftRecipes.size() + " craft recipes, "
                + disassemblyRecipes.size() + " disassembly recipes");
    }

    private void loadCraftRecipesFromDir(Path configDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*.json")) {
            for (Path path : stream) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    String content = Files.readString(path);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();

                    CraftRecipe recipe = parseCraftRecipe(json, path);
                    if (recipe != null) {
                        craftRecipes.put(recipe.getId(), recipe);
                        // Для крафтов не привязываем к профессии пока
                        System.out.println("[TradeOverhaul] Loaded craft recipe: " + recipe.getId());
                    }
                } catch (Exception e) {
                    System.err.println("[TradeOverhaul] Failed to load craft recipe " + path.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDisassemblyRecipesFromDir(Path configDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*.json")) {
            for (Path path : stream) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    String content = Files.readString(path);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();

                    DisassemblyRecipe recipe = parseDisassemblyRecipe(json, path);
                    if (recipe != null) {
                        disassemblyRecipes.put(recipe.getId(), recipe);
                        System.out.println("[TradeOverhaul] Loaded disassembly recipe: " + recipe.getId());
                    }
                } catch (Exception e) {
                    System.err.println("[TradeOverhaul] Failed to load disassembly recipe " + path.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createExampleCraftConfig(Path configDir) throws IOException {
        String bowUpgrade = """
    {
        "id": "bow_upgrade",
        "required_level": 1,
        "ingredients": [
            {
                "item": "minecraft:bow",
                "count": 1
            },
            {
                "item": "minecraft:string",
                "count": 3
            },
            {
                "item": "minecraft:iron_ingot",
                "count": 2
            }
        ],
        "result": "minecraft:bow",
        "cost": 50,
        "copy_nbt": true,
        "unique_ingredient_index": 0
    }
    """;

        String ironSword = """
    {
        "id": "iron_sword",
        "required_level": 1,
        "ingredients": [
            {
                "item": "minecraft:iron_ingot",
                "count": 2
            },
            {
                "item": "minecraft:stick",
                "count": 1
            }
        ],
        "result": "minecraft:iron_sword",
        "cost": 30,
        "copy_nbt": false,
        "unique_ingredient_index": -1
    }
    """;

        Files.writeString(configDir.resolve("bow_upgrade.json"), bowUpgrade);
        Files.writeString(configDir.resolve("iron_sword.json"), ironSword);
    }

    private CraftRecipe parseCraftRecipe(JsonObject json, Path path) {
        try {
            String id = json.get("id").getAsString();
            int requiredLevel = json.get("required_level").getAsInt();
            int cost = json.get("cost").getAsInt();
            boolean copyNbt = json.has("copy_nbt") && json.get("copy_nbt").getAsBoolean();

            int uniqueIndex = -1;
            if (json.has("unique_ingredient_index")) {
                uniqueIndex = json.get("unique_ingredient_index").getAsInt();
            }

            // НОВОЕ: читаем профессию (может отсутствовать)
            String profession = null;
            if (json.has("profession")) {
                profession = json.get("profession").getAsString();
            }

            // Парсим ингредиенты
            List<Ingredient> ingredients = new ArrayList<>();
            json.get("ingredients").getAsJsonArray().forEach(elem -> {
                JsonObject ingJson = elem.getAsJsonObject();
                ItemStack item = parseItemStack(ingJson.get("item").getAsString());
                int count = ingJson.get("count").getAsInt();
                ingredients.add(new Ingredient(item, count));
            });

            ItemStack result = parseItemStack(json.get("result").getAsString());

            System.out.println("[TradeOverhaul] Parsed recipe " + id + " with unique_index=" + uniqueIndex +
                    ", profession=" + (profession != null ? profession : "any"));

            return new CraftRecipe(id, requiredLevel, ingredients, result, cost, copyNbt, uniqueIndex, profession);
        } catch (Exception e) {
            System.err.println("[TradeOverhaul] Error parsing craft recipe " + path.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private DisassemblyRecipe parseDisassemblyRecipe(JsonObject json, Path path) {
        try {
            String id = json.get("id").getAsString();
            int requiredLevel = json.get("required_level").getAsInt();
            int cost = json.get("cost").getAsInt();
            ItemStack targetItem = parseItemStack(json.get("target_item").getAsString());

            List<YieldTier> tiers = new ArrayList<>();
            json.get("yield_tiers").getAsJsonArray().forEach(elem -> {
                JsonObject tierJson = elem.getAsJsonObject();
                double minDurability = tierJson.get("min_durability_pct").getAsDouble();

                List<ItemStack> drops = new ArrayList<>();
                tierJson.get("drops").getAsJsonArray().forEach(dropElem -> {
                    JsonObject dropJson = dropElem.getAsJsonObject();
                    String itemId = dropJson.get("item").getAsString();
                    int count = dropJson.get("count").getAsInt();
                    ItemStack dropStack = parseItemStack(itemId);
                    dropStack.setCount(count);
                    drops.add(dropStack);
                });

                tiers.add(new YieldTier(minDurability, drops));
            });

            return new DisassemblyRecipe(id, requiredLevel, targetItem, tiers, cost);
        } catch (Exception e) {
            System.err.println("[TradeOverhaul] Error parsing disassembly recipe " + path.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private String extractProfessionId(Path path) {
        String filename = path.getFileName().toString();
        return filename.replace(".json", "");
    }

    private void createExampleConfig(Path configDir) throws IOException {
        String exampleCraft = """
        {
            "id": "example_bow_upgrade",
            "required_level": 1,
            "ingredients": [
                {
                    "item": "minecraft:bow",
                    "count": 1
                },
                {
                    "item": "minecraft:string",
                    "count": 3
                }
            ],
            "result": "minecraft:bow",
            "cost": 50,
            "copy_nbt": true
        }
        """;

        Files.writeString(configDir.resolve("example_blacksmith.json"), exampleCraft);
    }

    public List<CraftRecipe> getCraftRecipesForProfession(String professionId, int villagerLevel) {
        List<CraftRecipe> result = new ArrayList<>();
        for (CraftRecipe recipe : craftRecipes.values()) {
            // Проверяем профессию: если у рецепта нет профессии (null) или она совпадает с профессией жителя
            String recipeProfession = recipe.getProfession();
            if (recipeProfession == null || recipeProfession.equals(professionId)) {
                result.add(recipe);
            }
        }
        return result;
    }

    public CraftRecipe getCraftRecipeById(String id) {
        return craftRecipes.get(id);
    }

    public DisassemblyRecipe getDisassemblyRecipeById(String id) {
        return disassemblyRecipes.get(id);
    }

    // ✅ Проверка, загружен ли рецепт (для отладки)
    public boolean hasCraftRecipe(String recipeId) {
        return craftRecipes.containsKey(recipeId);
    }

    // ✅ Клиентская загрузка конфигов (вызывается при подключении)
    public void loadRecipesClient() {
        craftRecipes.clear();
        disassemblyRecipes.clear();
        professionCrafts.clear();
        professionDisassembly.clear();

        java.nio.file.Path craftConfigDir = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("tradeoverhaul/crafts");

        if (!java.nio.file.Files.exists(craftConfigDir)) {
            try {
                java.nio.file.Files.createDirectories(craftConfigDir);
            } catch (java.io.IOException e) {
                TradeOverhaulMod.LOGGER.error("[TradeOverhaul] Failed to create client craft config dir", e);
            }
        } else {
            loadCraftRecipesFromDir(craftConfigDir);
        }

        TradeOverhaulMod.LOGGER.info("[TradeOverhaul] Client loaded {} craft recipes", craftRecipes.size());
    }

    public java.util.Map<String, CraftRecipe> getAllCraftRecipes() {
        return new java.util.HashMap<>(craftRecipes);
    }

    // В RecipeManager.java добавьте:

    public void clearRecipes() {
        craftRecipes.clear();
    }

    public void addCraftRecipe(CraftRecipe recipe) {
        craftRecipes.put(recipe.getId(), recipe);
    }

    private boolean hasServerRecipes = false;

    public void loadRecipesFromIds(List<String> recipeIds) {
        craftRecipes.clear();

        for (String id : recipeIds) {
            // Загружаем рецепт из конфига по ID
            CraftRecipe recipe = loadRecipeFromConfig(id);
            if (recipe != null) {
                craftRecipes.put(recipe.getId(), recipe);
                TradeOverhaulMod.LOGGER.info("[TradeOverhaul] Loaded recipe from config: {}", id);
            } else {
                TradeOverhaulMod.LOGGER.warn("[TradeOverhaul] Recipe not found in config: {}", id);
            }
        }
    }

    private CraftRecipe loadRecipeFromConfig(String recipeId) {
        // Путь к конфигу на клиенте
        java.nio.file.Path configPath = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("tradeoverhaul/crafts/" + recipeId + ".json");

        if (!java.nio.file.Files.exists(configPath)) {
            TradeOverhaulMod.LOGGER.warn("[TradeOverhaul] Config file not found: {}", configPath);
            return null;
        }

        try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(configPath)) {
            String content = java.nio.file.Files.readString(configPath);
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            return parseCraftRecipeFromJson(json, recipeId);
        } catch (Exception e) {
            TradeOverhaulMod.LOGGER.error("[TradeOverhaul] Failed to load recipe {}: {}", recipeId, e.getMessage());
            return null;
        }
    }

    private CraftRecipe parseCraftRecipeFromJson(com.google.gson.JsonObject json, String id) {
        try {
            int requiredLevel = json.get("required_level").getAsInt();
            int cost = json.get("cost").getAsInt();
            boolean copyNbt = json.has("copy_nbt") && json.get("copy_nbt").getAsBoolean();

            int uniqueIndex = -1;
            if (json.has("unique_ingredient_index")) {
                uniqueIndex = json.get("unique_ingredient_index").getAsInt();
            }

            String profession = null;
            if (json.has("profession")) {
                profession = json.get("profession").getAsString();
            }

            List<Ingredient> ingredients = new ArrayList<>();
            json.get("ingredients").getAsJsonArray().forEach(elem -> {
                com.google.gson.JsonObject ingJson = elem.getAsJsonObject();
                ItemStack item = parseItemStack(ingJson.get("item").getAsString());
                int count = ingJson.get("count").getAsInt();
                ingredients.add(new Ingredient(item, count));
            });

            ItemStack result = parseItemStack(json.get("result").getAsString());

            return new CraftRecipe(id, requiredLevel, ingredients, result, cost, copyNbt, uniqueIndex, profession);
        } catch (Exception e) {
            TradeOverhaulMod.LOGGER.error("[TradeOverhaul] Error parsing recipe {}: {}", id, e.getMessage());
            return null;
        }
    }

    private ItemStack parseItemStack(String itemString) {
        itemString = itemString.trim();
        if (itemString.contains("{")) {
            itemString = itemString.substring(0, itemString.indexOf("{"));
        }
        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(itemString);
        if (id == null) {
            return new ItemStack(net.minecraft.item.Items.AIR);
        }
        var item = net.minecraft.registry.Registries.ITEM.get(id);
        return new ItemStack(item);
    }

    private List<CraftRecipe> serverRecipes = new ArrayList<>();

    public void setServerRecipes(List<CraftRecipe> recipes) {
        this.serverRecipes = new ArrayList<>(recipes);
        this.hasServerRecipes = true;
        TradeOverhaulMod.LOGGER.info("[TradeOverhaul] Saved {} recipes from server", recipes.size());
    }

    public List<CraftRecipe> getServerRecipes() {
        return serverRecipes;
    }

    public boolean hasServerRecipes() {
        boolean has = serverRecipesLoaded && !allServerRecipes.isEmpty();
        TradeOverhaulMod.LOGGER.info("[TradeOverhaul] hasServerRecipes: {} (loaded={}, size={})", has, serverRecipesLoaded, allServerRecipes.size());
        return has;
    }

    public void clearServerRecipes() {
        serverRecipes.clear();
        hasServerRecipes = false;
    }

    public void setAllServerRecipes(List<CraftRecipe> recipes) {
        this.allServerRecipes = new ArrayList<>(recipes);
        this.serverRecipesLoaded = true;
        TradeOverhaulMod.LOGGER.info("[TradeOverhaul] setAllServerRecipes called, size: {}, serverRecipesLoaded: {}", recipes.size(), serverRecipesLoaded);
    }

    public List<CraftRecipe> getAllServerRecipes() {
        return allServerRecipes;
    }
}