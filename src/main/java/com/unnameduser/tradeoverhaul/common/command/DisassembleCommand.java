package com.unnameduser.tradeoverhaul.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.unnameduser.tradeoverhaul.common.config.DisassemblyConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

public class DisassembleCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tradeoverhaul")
                .requires(src -> src.hasPermissionLevel(2))
                .then(literal("generate_disassembly_whitelist")
                        .executes(ctx -> generateList(true, ctx.getSource())))
                .then(literal("generate_repair_whitelist")
                        .executes(ctx -> generateList(false, ctx.getSource())))
        );
    }

    public static int generateList(boolean forDisassembly, ServerCommandSource src) {
        List<String> ids = new ArrayList<>();
        var server = src.getServer();
        var registryManager = server.getRegistryManager();

        if (forDisassembly) {
            // ✅ ГЕНЕРАЦИЯ ПО РЕЦЕПТАМ: ищем все предметы, которые крафтятся по 1 шт в верстаке/2x2
            Set<Item> craftableItems = new HashSet<>();

            // В 1.20.1 RecipeManager.values() возвращает Collection<Recipe<?>> напрямую
            for (Recipe<?> recipe : server.getRecipeManager().values()) {
                if (recipe instanceof CraftingRecipe craftingRecipe) {
                    ItemStack output = craftingRecipe.getOutput(registryManager);
                    // Берём только рецепты с выходом 1 предмета
                    if (output.getCount() == 1 && !output.isEmpty()) {
                        craftableItems.add(output.getItem());
                    }
                }
            }

            // Конвертируем в список ID
            for (Item item : craftableItems) {
                ids.add(Registries.ITEM.getId(item).toString());
            }
        } else {
            // ✅ ГЕНЕРАЦИЯ ДЛЯ РЕМОНТА: все предметы с прочностью
            for (Item item : Registries.ITEM) {
                if (item.isDamageable()) {
                    ids.add(Registries.ITEM.getId(item).toString());
                }
            }
        }

        File targetFile = forDisassembly ? DisassemblyConfig.DISASSEMBLY_LIST : DisassemblyConfig.REPAIR_LIST;
        DisassemblyConfig.saveList(targetFile, ids);

        src.sendFeedback(() -> Text.literal("§a[TradeOverhaul] Generated " + ids.size() + " items to " + targetFile.getName()), true);
        return ids.size();
    }
}