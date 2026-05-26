package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.dto.CraftRecipe;
import com.unnameduser.tradeoverhaul.dto.Ingredient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AvailableRecipesPayload {
    public static final Identifier ID = new Identifier("tradeoverhaul", "available_recipes_full");

    private final List<CraftRecipe> recipes;

    public AvailableRecipesPayload(List<CraftRecipe> recipes) {
        this.recipes = recipes;
    }

    public List<CraftRecipe> getRecipes() {
        return recipes;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(recipes.size());
        for (CraftRecipe recipe : recipes) {
            writeRecipe(buf, recipe);
        }
    }

    public static AvailableRecipesPayload read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<CraftRecipe> recipes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            recipes.add(readRecipe(buf));
        }
        return new AvailableRecipesPayload(recipes);
    }

    private static void writeRecipe(PacketByteBuf buf, CraftRecipe recipe) {
        buf.writeString(recipe.getId());
        buf.writeInt(recipe.getRequiredLevel());
        buf.writeInt(recipe.getCost());
        buf.writeBoolean(recipe.shouldCopyNbt());
        buf.writeInt(recipe.getUniqueIngredientIndex());

        buf.writeBoolean(recipe.getProfession() != null);
        if (recipe.getProfession() != null) {
            buf.writeString(recipe.getProfession());
        }

        // Ингредиенты - через NBT
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ing : recipe.getIngredients()) {
            NbtCompound stackNbt = new NbtCompound();
            ing.getItem().writeNbt(stackNbt);
            buf.writeNbt(stackNbt);
            buf.writeInt(ing.getCount());
        }

        // Результат - через NBT
        NbtCompound resultNbt = new NbtCompound();
        recipe.getResult().writeNbt(resultNbt);
        buf.writeNbt(resultNbt);
    }

    private static CraftRecipe readRecipe(PacketByteBuf buf) {
        String id = buf.readString();
        int requiredLevel = buf.readInt();
        int cost = buf.readInt();
        boolean copyNbt = buf.readBoolean();
        int uniqueIndex = buf.readInt();

        String profession = null;
        if (buf.readBoolean()) {
            profession = buf.readString();
        }

        int ingCount = buf.readVarInt();
        List<Ingredient> ingredients = new ArrayList<>(ingCount);
        for (int i = 0; i < ingCount; i++) {
            NbtCompound stackNbt = buf.readNbt();
            ItemStack item = ItemStack.fromNbt(stackNbt);
            int count = buf.readInt();
            ingredients.add(new Ingredient(item, count));
        }

        NbtCompound resultNbt = buf.readNbt();
        ItemStack result = ItemStack.fromNbt(resultNbt);

        return new CraftRecipe(id, requiredLevel, ingredients, result, cost, copyNbt, uniqueIndex, profession);
    }
}