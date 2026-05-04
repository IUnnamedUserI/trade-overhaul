package com.unnameduser.tradeoverhaul.dto;

import net.minecraft.item.ItemStack;
import java.util.List;

public class CraftRecipe {
    private String id;
    private int required_level;
    private List<Ingredient> ingredients;
    private ItemStack result;
    private int cost;
    private boolean copy_nbt;
    private int unique_ingredient_index;  // НОВОЕ: индекс уникального ингредиента (0 = первый)

    // Конструктор с unique_ingredient_index
    public CraftRecipe(String id, int required_level, List<Ingredient> ingredients,
                       ItemStack result, int cost, boolean copy_nbt, int unique_ingredient_index) {
        this.id = id;
        this.required_level = required_level;
        this.ingredients = ingredients;
        this.result = result;
        this.cost = cost;
        this.copy_nbt = copy_nbt;
        this.unique_ingredient_index = unique_ingredient_index;
    }

    // Старый конструктор для обратной совместимости (unique = 0)
    public CraftRecipe(String id, int required_level, List<Ingredient> ingredients,
                       ItemStack result, int cost, boolean copy_nbt) {
        this(id, required_level, ingredients, result, cost, copy_nbt, 0);
    }

    // Геттеры
    public String getId() { return id; }
    public int getRequiredLevel() { return required_level; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public ItemStack getResult() { return result; }
    public int getCost() { return cost; }
    public boolean shouldCopyNbt() { return copy_nbt; }
    public int getUniqueIngredientIndex() { return unique_ingredient_index; }
}