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
									if (file.staticPool == null) file.staticPool = new ArrayList<>();
									if (file.weaponPool == null) file.weaponPool = new ArrayList<>();
									if (file.toolPool == null) file.toolPool = new ArrayList<>();
									if (file.generalPool == null) file.generalPool = new ArrayList<>();
									if (file.buyPool == null) file.buyPool = new ArrayList<>();
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
