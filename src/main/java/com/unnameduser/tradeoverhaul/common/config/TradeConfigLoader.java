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
import java.util.*;
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
			copyDefaultIfMissing(root.resolve("professions/butcher.json"), "tradeoverhaul-defaults/professions/butcher.json");
			copyDefaultIfMissing(root.resolve("professions/cartographer.json"), "tradeoverhaul-defaults/professions/cartographer.json");
			copyDefaultIfMissing(root.resolve("professions/cleric.json"), "tradeoverhaul-defaults/professions/cleric.json");
			copyDefaultIfMissing(root.resolve("professions/fisherman.json"), "tradeoverhaul-defaults/professions/fisherman.json");
			copyDefaultIfMissing(root.resolve("professions/mason.json"), "tradeoverhaul-defaults/professions/mason.json");
			copyDefaultIfMissing(root.resolve("professions/shepherd.json"), "tradeoverhaul-defaults/professions/shepherd.json");
			copyDefaultIfMissing(root.resolve("professions/toolsmith.json"), "tradeoverhaul-defaults/professions/toolsmith.json");

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
									log.info("Loaded profession {}: level1Pool={}, level2Pool={}, level3Pool={}, level4Pool={}, level5Pool={}, buyPool={}, playerSellPool={}", 
										id, 
										file.level1Pool != null ? file.level1Pool.size() : "null",
										file.level2Pool != null ? file.level2Pool.size() : "null",
										file.level3Pool != null ? file.level3Pool.size() : "null",
										file.level4Pool != null ? file.level4Pool.size() : "null",
										file.level5Pool != null ? file.level5Pool.size() : "null",
										file.buyPool != null ? file.buyPool.size() : "null",
										file.playerSellPool != null ? file.playerSellPool.size() : "null");
									if (file.buyPool == null) file.buyPool = new ArrayList<>();
									if (file.playerSellPool == null) file.playerSellPool = new ArrayList<>();
									if (file.enchantments == null) file.enchantments = new ArrayList<>();
									if (file.level1Pool == null) file.level1Pool = new ArrayList<>();
									if (file.level2Pool == null) file.level2Pool = new ArrayList<>();
									if (file.level3Pool == null) file.level3Pool = new ArrayList<>();
									if (file.level4Pool == null) file.level4Pool = new ArrayList<>();
									if (file.level5Pool == null) file.level5Pool = new ArrayList<>();
									if (file.helmetEnchantments == null) file.helmetEnchantments = new ArrayList<>();
									if (file.chestplateEnchantments == null) file.chestplateEnchantments = new ArrayList<>();
									if (file.leggingsEnchantments == null) file.leggingsEnchantments = new ArrayList<>();
									if (file.bootsEnchantments == null) file.bootsEnchantments = new ArrayList<>();
									if (file.bowEnchantments == null) file.bowEnchantments = new ArrayList<>();
									if (file.crossbowEnchantments == null) file.crossbowEnchantments = new ArrayList<>();
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

	// ========== МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ КОНФИГОВ ==========

	/**
	 * Возвращает копию всех загруженных профессий (для отправки клиенту)
	 */
	public static Map<Identifier, ProfessionTradeFile> getAllProfessions() {
		Map<Identifier, ProfessionTradeFile> copy = new HashMap<>();
		for (Map.Entry<Identifier, ProfessionTradeFile> entry : professions.entrySet()) {
			// Глубокая копия через JSON, чтобы избежать модификации оригинала
			try {
				String json = GSON.toJson(entry.getValue());
				ProfessionTradeFile cloned = GSON.fromJson(json, ProfessionTradeFile.class);
				copy.put(entry.getKey(), cloned);
			} catch (Exception e) {
				TradeOverhaulMod.LOGGER.warn("Failed to clone profession config for {}", entry.getKey(), e);
			}
		}
		return Collections.unmodifiableMap(copy);
	}

	/**
	 * Временно заменяет конфиг профессии (для клиентской синхронизации)
	 * @return предыдущий конфиг или null
	 */
	public static ProfessionTradeFile setProfessionTemp(Identifier professionId, ProfessionTradeFile file) {
		if (professions instanceof HashMap) {
			return ((HashMap<Identifier, ProfessionTradeFile>) professions).put(professionId, file);
		}
		// Если map неизменяемый — создаём новый
		HashMap<Identifier, ProfessionTradeFile> mutable = new HashMap<>(professions);
		ProfessionTradeFile old = mutable.put(professionId, file);
		professions = Collections.unmodifiableMap(mutable);
		return old;
	}

	/**
	 * Восстанавливает конфиг профессии из временного хранилища
	 */
	public static void restoreProfession(Identifier professionId, ProfessionTradeFile original) {
		if (original != null) {
			setProfessionTemp(professionId, original);
		} else {
			if (professions instanceof HashMap) {
				((HashMap<Identifier, ProfessionTradeFile>) professions).remove(professionId);
			} else {
				HashMap<Identifier, ProfessionTradeFile> mutable = new HashMap<>(professions);
				mutable.remove(professionId);
				professions = Collections.unmodifiableMap(mutable);
			}
		}
	}

	/**
	 * Генерирует хеш всех конфигов для быстрой проверки синхронизации
	 */
	public static String getConfigsHash() {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
			
			// Хеш настроек
			md.update(GSON.toJson(settings).getBytes(StandardCharsets.UTF_8));
			
			// Хеш профессий (сортируем по ID для детерминизма)
			List<Identifier> sortedIds = new ArrayList<>(professions.keySet());
			sortedIds.sort(Identifier::compareTo);
			for (Identifier id : sortedIds) {
				md.update(id.toString().getBytes(StandardCharsets.UTF_8));
				md.update(GSON.toJson(professions.get(id)).getBytes(StandardCharsets.UTF_8));
			}
			
			byte[] hash = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			TradeOverhaulMod.LOGGER.warn("Failed to generate config hash", e);
			return "error";
		}
	}

	/**
	 * Сериализует конфиг профессии в JSON-строку
	 */
	public static String professionToJson(ProfessionTradeFile file) {
		return GSON.toJson(file);
	}

	/**
	 * Десериализует конфиг профессии из JSON-строки
	 */
	public static ProfessionTradeFile professionFromJson(String json) {
		return GSON.fromJson(json, ProfessionTradeFile.class);
	}

	/**
	 * Сериализует настройки в JSON-строку
	 */
	public static String settingsToJson() {
		return GSON.toJson(settings);
	}

	/**
	 * Десериализует настройки из JSON-строки
	 */
	public static TradeOverhaulSettings settingsFromJson(String json) {
		TradeOverhaulSettings s = GSON.fromJson(json, TradeOverhaulSettings.class);
		return s != null ? s : new TradeOverhaulSettings();
	}

	public static void setSettings(TradeOverhaulSettings newSettings) {
		settings = newSettings;
	}
}
