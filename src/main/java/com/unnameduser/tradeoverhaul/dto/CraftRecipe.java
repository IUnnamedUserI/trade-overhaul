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
    private int unique_ingredient_index;
    private String profession;  // НОВОЕ: профессия жителя (null = доступно всем)

    // Полный конструктор
    public CraftRecipe(String id, int required_level, List<Ingredient> ingredients,
                       ItemStack result, int cost, boolean copy_nbt, int unique_ingredient_index, String profession) {
        this.id = id;
        this.required_level = required_level;
        this.ingredients = ingredients;
        this.result = result;
        this.cost = cost;
        this.copy_nbt = copy_nbt;
        this.unique_ingredient_index = unique_ingredient_index;
        this.profession = profession;
    }

    // Конструктор для обратной совместимости (без профессии)
    public CraftRecipe(String id, int required_level, List<Ingredient> ingredients,
                       ItemStack result, int cost, boolean copy_nbt, int unique_ingredient_index) {
        this(id, required_level, ingredients, result, cost, copy_nbt, unique_ingredient_index, null);
    }

    // Геттеры
    public String getId() { return id; }
    public int getRequiredLevel() { return required_level; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public ItemStack getResult() { return result; }
    public int getCost() { return cost; }
    public boolean shouldCopyNbt() { return copy_nbt; }
    public int getUniqueIngredientIndex() { return unique_ingredient_index; }
    public String getProfession() { return profession; }
}