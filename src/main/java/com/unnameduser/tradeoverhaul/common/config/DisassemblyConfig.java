package com.unnameduser.tradeoverhaul.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DisassemblyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config/tradeoverhaul");
    public static final File DISASSEMBLY_LIST = new File(CONFIG_DIR, "disassembly_whitelist.json");
    public static final File REPAIR_LIST = new File(CONFIG_DIR, "repair_whitelist.json");

    private static Set<String> disassemblyWhitelist = new HashSet<>();
    private static Set<String> repairWhitelist = new HashSet<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        CONFIG_DIR.mkdirs();

        // ✅ Если файла нет (первый запуск или удаление) — восстанавливаем эталон из ресурсов
        if (!DISASSEMBLY_LIST.exists()) {
            copyDefaultDisassemblyConfig();
        }

        disassemblyWhitelist = loadList(DISASSEMBLY_LIST);
        repairWhitelist = loadList(REPAIR_LIST);
        loaded = true;

        TradeOverhaulMod.LOGGER.info("[Disassembly] Loaded {} disassembly & {} repair items",
                disassemblyWhitelist.size(), repairWhitelist.size());
    }

    public static boolean isLoaded() { return loaded; }

    private static void copyDefaultDisassemblyConfig() {
        try {
            var modContainer = FabricLoader.getInstance().getModContainer(TradeOverhaulMod.MOD_ID).orElseThrow();
            var resourcePath = modContainer.findPath("assets/tradeoverhaul/default_disassembly_whitelist.json");

            if (resourcePath.isPresent() && Files.exists(resourcePath.get())) {
                Files.copy(resourcePath.get(), DISASSEMBLY_LIST.toPath(), StandardCopyOption.REPLACE_EXISTING);
                TradeOverhaulMod.LOGGER.info("[Disassembly] Restored default disassembly config from mod resources.");
            } else {
                TradeOverhaulMod.LOGGER.warn("[Disassembly] Default resource not found. Creating empty fallback.");
                saveList(DISASSEMBLY_LIST, List.of());
            }
        } catch (Exception e) {
            TradeOverhaulMod.LOGGER.error("[Disassembly] Failed to copy default config", e);
            saveList(DISASSEMBLY_LIST, List.of());
        }
    }

    private static Set<String> loadList(File file) {
        if (!file.exists()) return new HashSet<>();
        try (FileReader reader = new FileReader(file)) {
            String[] items = GSON.fromJson(reader, String[].class);
            return items != null ? new HashSet<>(List.of(items)) : new HashSet<>();
        } catch (Exception e) {
            TradeOverhaulMod.LOGGER.error("[Disassembly] Failed to load {}", file.getName(), e);
            return new HashSet<>();
        }
    }

    public static void saveList(File file, List<String> ids) {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(ids, writer);
                TradeOverhaulMod.LOGGER.info("[Disassembly] Saved {} items to {}", ids.size(), file.getName());
            }
        } catch (Exception e) {
            TradeOverhaulMod.LOGGER.error("[Disassembly] Failed to save {}", file.getName(), e);
        }
    }

    public static boolean isDisassemblyAllowed(Item item) {
        if (!loaded) load();
        return disassemblyWhitelist.contains(Registries.ITEM.getId(item).toString());
    }

    public static boolean isRepairAllowed(Item item) {
        if (!loaded) load();
        return repairWhitelist.contains(Registries.ITEM.getId(item).toString());
    }

    public static int getDisassemblyWhitelistSize() {
        if (!loaded) load();
        return disassemblyWhitelist.size();
    }
}