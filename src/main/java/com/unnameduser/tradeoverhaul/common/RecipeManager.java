package com.unnameduser.tradeoverhaul.common;

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
            System.out.println("[TradeOverhaul] Crafts folder not found, creating...");
            try {
                Files.createDirectories(craftConfigDir);
                createExampleCraftConfig(craftConfigDir);
            } catch (IOException e) {
                e.printStackTrace();
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

            // Читаем unique_ingredient_index (по умолчанию -1, если нет индекса)
            int uniqueIndex = -1;
            if (json.has("unique_ingredient_index")) {
                uniqueIndex = json.get("unique_ingredient_index").getAsInt();
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

            System.out.println("[TradeOverhaul] Parsed recipe " + id + " with unique_index=" + uniqueIndex);

            // Используем конструктор с unique_ingredient_index
            return new CraftRecipe(id, requiredLevel, ingredients, result, cost, copyNbt, uniqueIndex);
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

    private ItemStack parseItemStack(String itemString) {
        // Очищаем строку от возможных пробелов
        itemString = itemString.trim();

        // Убираем NBT если есть (пока не поддерживаем)
        if (itemString.contains("{")) {
            itemString = itemString.substring(0, itemString.indexOf("{"));
        }

        Identifier id = Identifier.tryParse(itemString);
        if (id == null) {
            System.err.println("[TradeOverhaul] Invalid item identifier: " + itemString);
            return new ItemStack(Items.AIR);
        }

        var item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            System.err.println("[TradeOverhaul] Unknown item: " + itemString);
        }

        return new ItemStack(item);
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
        // Пока возвращаем все рецепты (без фильтрации по профессии)
        return new ArrayList<>(craftRecipes.values());
    }

    public CraftRecipe getCraftRecipeById(String id) {
        return craftRecipes.get(id);
    }

    public DisassemblyRecipe getDisassemblyRecipeById(String id) {
        return disassemblyRecipes.get(id);
    }
}