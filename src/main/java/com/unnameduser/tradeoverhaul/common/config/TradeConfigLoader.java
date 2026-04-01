package com.unnameduser.tradeoverhaul.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class TradeConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static TradeOverhaulSettings settings = new TradeOverhaulSettings();
	private static Map<Identifier, ProfessionTradeFile> professions = Collections.emptyMap();

	public static void load(Logger log) {
		Path root = FabricLoader.getInstance().getConfigDir().resolve("tradeoverhaul");
		try {
			Files.createDirectories(root.resolve("professions"));
			copyDefaultIfMissing(root.resolve("settings.json"), "tradeoverhaul-defaults/settings.json");
			copyDefaultIfMissing(root.resolve("professions/farmer.json"), "tradeoverhaul-defaults/professions/farmer.json");
			copyDefaultIfMissing(root.resolve("professions/leatherworker.json"), "tradeoverhaul-defaults/professions/leatherworker.json");
			copyDefaultIfMissing(root.resolve("professions/armourer.json"), "tradeoverhaul-defaults/professions/armourer.json");
			copyDefaultIfMissing(root.resolve("professions/weaponsmith.json"), "tradeoverhaul-defaults/professions/weaponsmith.json");
			copyDefaultIfMissing(root.resolve("professions/fletcher.json"), "tradeoverhaul-defaults/professions/fletcher.json");
			copyDefaultIfMissing(root.resolve("professions/librarian.json"), "tradeoverhaul-defaults/professions/librarian.json");

			settings = readSettings(root.resolve("settings.json"));

			Map<Identifier, ProfessionTradeFile> map = new HashMap<>();
			Path profDir = root.resolve("professions");
			if (Files.isDirectory(profDir)) {
				try (Stream<Path> stream = Files.list(profDir)) {
					stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
						try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
							ProfessionTradeFile file = GSON.fromJson(r, ProfessionTradeFile.class);
							if (file != null && file.profession != null) {
								Identifier id = Identifier.tryParse(file.profession);
								if (id != null) {
									log.info("Loaded profession {}: staticPool={}, level1Pool={}, level2Pool={}, level3Pool={}, level4Pool={}, level5Pool={}", 
										id, 
										file.staticPool != null ? file.staticPool.size() : "null",
										file.level1Pool != null ? file.level1Pool.size() : "null",
										file.level2Pool != null ? file.level2Pool.size() : "null",
										file.level3Pool != null ? file.level3Pool.size() : "null",
										file.level4Pool != null ? file.level4Pool.size() : "null",
										file.level5Pool != null ? file.level5Pool.size() : "null");
									if (file.staticPool == null) file.staticPool = new ArrayList<>();
									if (file.weaponPool == null) file.weaponPool = new ArrayList<>();
									if (file.toolPool == null) file.toolPool = new ArrayList<>();
									if (file.generalPool == null) file.generalPool = new ArrayList<>();
									if (file.buyPool == null) file.buyPool = new ArrayList<>();
									if (file.enchantments == null) file.enchantments = new ArrayList<>();
									if (file.level1Pool == null) file.level1Pool = new ArrayList<>();
									if (file.level2Pool == null) file.level2Pool = new ArrayList<>();
									if (file.level3Pool == null) file.level3Pool = new ArrayList<>();
									if (file.level4Pool == null) file.level4Pool = new ArrayList<>();
									if (file.level5Pool == null) file.level5Pool = new ArrayList<>();
									if (file.levelMoneySettings == null) file.levelMoneySettings = new ProfessionTradeFile.LevelMoneySettings();
									sanitizeProfession(file, id, log);
									map.put(id, file);
								}
							}
						} catch (IOException e) {
							log.error("Failed to read profession file {}", path, e);
						}
					});
				}
			}
			professions = Collections.unmodifiableMap(map);
			log.info("Trade Overhaul: loaded {} profession trade file(s)", map.size());
		} catch (IOException e) {
			log.error("Trade Overhaul: config load failed", e);
		}
	}

	private static void sanitizeProfession(ProfessionTradeFile file, Identifier professionId, Logger log) {
		Identifier emeraldId = new Identifier("minecraft", "emerald");
		
		// Sanitize static pool
		file.staticPool.removeIf(e -> {
			if (e.item == null) {
				log.warn("Removing static entry without item in {}", professionId);
				return true;
			}
			Identifier itemId = Identifier.tryParse(e.item);
			if (itemId == null || !Registries.ITEM.containsId(itemId)) {
				log.warn("Removing unknown item {} in {}", e.item, professionId);
				return true;
			}
			if (itemId.equals(emeraldId)) {
				log.warn("Removing emerald from villager sell list in {}", professionId);
				return true;
			}
			if (e.sell >= e.buy) {
				log.warn("Removing {} in {}: sell ({}) must be < buy ({})", e.item, professionId, e.sell, e.buy);
				return true;
			}
			return false;
		});
		
		// Sanitize weapon pool
		file.weaponPool.removeIf(w -> {
			if (w.tag == null) {
				log.warn("Removing weapon entry without tag in {}", professionId);
				return true;
			}
			return false;
		});
		
		// Sanitize tool pool
		file.toolPool.removeIf(t -> {
			if (t.tag == null) {
				log.warn("Removing tool entry without tag in {}", professionId);
				return true;
			}
			return false;
		});
		
		// Sanitize general pool
		file.generalPool.removeIf(g -> {
			if (g.tag == null) {
				log.warn("Removing general entry without tag in {}", professionId);
				return true;
			}
			return false;
		});
		
		// Sanitize buy pool
		file.buyPool.removeIf(b -> {
			if (b.tag == null) {
				log.warn("Removing buy-only entry without tag in {}", professionId);
				return true;
			}
			return false;
		});

		// Sanitize enchantments (for librarian)
		if (file.enchantments != null) {
			file.enchantments.removeIf(e -> {
				if (e.enchantment == null) {
					log.warn("Removing enchantment entry without enchantment ID in {}", professionId);
					return true;
				}
				Identifier enchantId = Identifier.tryParse(e.enchantment);
				if (enchantId == null) {
					log.warn("Removing enchantment entry with invalid ID {} in {}", e.enchantment, professionId);
					return true;
				}
				// Проверяем, существует ли такое зачарование в реестре
				if (!net.minecraft.registry.Registries.ENCHANTMENT.containsId(enchantId)) {
					log.warn("Removing unknown enchantment {} in {}", e.enchantment, professionId);
					return true;
				}
				// Проверяем корректность уровней
				if (e.min_level == null || e.min_level < 1) {
					e.min_level = 1;
					log.warn("Fixed min_level for {} in {} to 1", e.enchantment, professionId);
				}
				if (e.max_level == null || e.max_level < e.min_level) {
					e.max_level = e.min_level;
					log.warn("Fixed max_level for {} in {} to {}", e.enchantment, professionId, e.max_level);
				}
				// Проверяем цены
				if (e.base_price == null || e.base_price < 0) {
					e.base_price = 400;
					log.warn("Fixed base_price for {} in {} to 400", e.enchantment, professionId);
				}
				if (e.price_per_level == null || e.price_per_level < 0) {
					e.price_per_level = 0;
					log.warn("Fixed price_per_level for {} in {} to 0", e.enchantment, professionId);
				}
				return false;
			});
			log.info("Loaded {} enchantments for {}", file.enchantments.size(), professionId);
		}
		
		// Инициализация настроек денег по умолчанию
		if (file.levelMoneySettings != null) {
			if (file.levelMoneySettings.level1StartMoney == null) file.levelMoneySettings.level1StartMoney = 300;
			if (file.levelMoneySettings.level2StartMoney == null) file.levelMoneySettings.level2StartMoney = 500;
			if (file.levelMoneySettings.level3StartMoney == null) file.levelMoneySettings.level3StartMoney = 800;
			if (file.levelMoneySettings.level4StartMoney == null) file.levelMoneySettings.level4StartMoney = 1200;
			if (file.levelMoneySettings.level5StartMoney == null) file.levelMoneySettings.level5StartMoney = 2000;
			
			if (file.levelMoneySettings.level1RestockMoney == null) file.levelMoneySettings.level1RestockMoney = 400;
			if (file.levelMoneySettings.level2RestockMoney == null) file.levelMoneySettings.level2RestockMoney = 700;
			if (file.levelMoneySettings.level3RestockMoney == null) file.levelMoneySettings.level3RestockMoney = 1100;
			if (file.levelMoneySettings.level4RestockMoney == null) file.levelMoneySettings.level4RestockMoney = 1600;
			if (file.levelMoneySettings.level5RestockMoney == null) file.levelMoneySettings.level5RestockMoney = 2500;
			
			log.info("Loaded level money settings for {}", professionId);
		}
	}

	private static TradeOverhaulSettings readSettings(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			return new TradeOverhaulSettings();
		}
		try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			TradeOverhaulSettings s = GSON.fromJson(r, TradeOverhaulSettings.class);
			return s != null ? s : new TradeOverhaulSettings();
		}
	}

	private static void copyDefaultIfMissing(Path target, String resourcePath) throws IOException {
		if (Files.isRegularFile(target)) return;
		try (InputStream in = TradeOverhaulMod.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new IOException("Missing classpath resource: " + resourcePath);
			}
			Files.createDirectories(target.getParent());
			Files.copy(in, target);
		}
	}

	public static TradeOverhaulSettings getSettings() {
		return settings;
	}

	public static ProfessionTradeFile getProfession(Identifier professionId) {
		return professions.get(professionId);
	}

}
