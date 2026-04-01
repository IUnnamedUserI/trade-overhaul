package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TradeRestock {
	private TradeRestock() {}

	public static void tick(VillagerEntity villager) {
		World world = villager.getWorld();
		if (world.isClient) return;

		Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		ProfessionTradeFile file = TradeConfigLoader.getProfession(profId);
		VillagerTradeData data = (VillagerTradeData) villager;
		VillagerInventoryComponent inv = data.tradeOverhaul$getInventory();
		TradeOverhaulSettings settings = TradeConfigLoader.getSettings();

		if (file == null) {
			return;
		}

		int[] offerSlots = data.tradeOverhaul$getOfferSlots();
		if (offerSlots == null) {
			performRestock(villager, file, settings, inv, data);
			return;
		}

		if (allOfferSlotsEmpty(inv, offerSlots)) {
			long since = data.tradeOverhaul$getEmptySinceTick();
			if (since < 0) {
				data.tradeOverhaul$setEmptySinceTick(world.getTime());
			} else {
				long delay = (long) settings.restockDelayGameDays * 24000L;
				if (world.getTime() - since >= delay) {
					performRestock(villager, file, settings, inv, data);
				}
			}
		} else {
			if (data.tradeOverhaul$getEmptySinceTick() >= 0) {
				data.tradeOverhaul$setEmptySinceTick(-1L);
			}
		}
	}

	/**
	 * Принудительное обновление торговли (для команды /tradeoverhaul refresh)
	 */
	public static void forceRestock(VillagerEntity villager, ProfessionTradeFile file, TradeOverhaulSettings settings) {
		VillagerTradeData data = (VillagerTradeData) villager;
		VillagerInventoryComponent inv = data.tradeOverhaul$getInventory();
		performRestock(villager, file, settings, inv, data);
	}

	private static boolean allOfferSlotsEmpty(VillagerInventoryComponent inv, int[] slots) {
		for (int s : slots) {
			if (s >= 0 && s < inv.size() && !inv.getStack(s).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static void performRestock(VillagerEntity villager, ProfessionTradeFile file, TradeOverhaulSettings settings,
			VillagerInventoryComponent inv, VillagerTradeData data) {
		// Полная очистка инвентаря жителя перед новым restock
		for (int i = 0; i < inv.size(); i++) {
			inv.setStack(i, ItemStack.EMPTY);
		}

		// Получаем уровень жителя из компонента профессии
		int villagerLevel = data.tradeOverhaul$getProfession().getLevel();
		
		// Отладка: проверяем уровень
		TradeOverhaulMod.LOGGER.info("=== RESTOCK DEBUG ===");
		TradeOverhaulMod.LOGGER.info("villagerLevel from component: {}", villagerLevel);
		TradeOverhaulMod.LOGGER.info("villager vanilla level: {}", villager.getVillagerData().getLevel());
		TradeOverhaulMod.LOGGER.info("profession: {}", Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()));
		TradeOverhaulMod.LOGGER.info("level1Pool size: {}", file.level1Pool != null ? file.level1Pool.size() : "null");
		TradeOverhaulMod.LOGGER.info("level2Pool size: {}", file.level2Pool != null ? file.level2Pool.size() : "null");

		List<PoolChoice> pool = new ArrayList<>();

		// Проверяем, есть ли в конфиге пулы по уровням
		boolean hasLevelPools = (file.level1Pool != null && !file.level1Pool.isEmpty()) ||
			(file.level2Pool != null && !file.level2Pool.isEmpty()) ||
			(file.level3Pool != null && !file.level3Pool.isEmpty()) ||
			(file.level4Pool != null && !file.level4Pool.isEmpty()) ||
			(file.level5Pool != null && !file.level5Pool.isEmpty());

		if (hasLevelPools) {
			// Используем новую систему с уровнями
			TradeOverhaulMod.LOGGER.info("Using level-based pools for villager level {}", villagerLevel);

			// Уровень 1: только level1Pool
			// Уровень 2: level1Pool + level2Pool
			// и т.д.
			if (villagerLevel >= 1 && file.level1Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level1Pool) {
					pool.add(new LevelChoice(e, 1));
				}
				TradeOverhaulMod.LOGGER.info("Added level1Pool: {} items", file.level1Pool.size());
			} else if (villagerLevel >= 1 && file.level1Pool == null) {
				TradeOverhaulMod.LOGGER.warn("villagerLevel >= 1 but level1Pool is null for profession {}", Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()));
			}
			if (villagerLevel >= 2 && file.level2Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level2Pool) {
					pool.add(new LevelChoice(e, 2));
				}
				TradeOverhaulMod.LOGGER.info("Added level2Pool: {} items", file.level2Pool.size());
			}
			if (villagerLevel >= 3 && file.level3Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level3Pool) {
					pool.add(new LevelChoice(e, 3));
				}
				TradeOverhaulMod.LOGGER.info("Added level3Pool: {} items", file.level3Pool.size());
			}
			if (villagerLevel >= 4 && file.level4Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level4Pool) {
					pool.add(new LevelChoice(e, 4));
				}
				TradeOverhaulMod.LOGGER.info("Added level4Pool: {} items", file.level4Pool.size());
			}
			if (villagerLevel >= 5 && file.level5Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level5Pool) {
					pool.add(new LevelChoice(e, 5));
				}
				TradeOverhaulMod.LOGGER.info("Added level5Pool: {} items", file.level5Pool.size());
			}
		} else {
			// Используем старую систему (обратная совместимость)
			TradeOverhaulMod.LOGGER.info("Restock: using legacy static pools");
			for (ProfessionTradeFile.StaticPoolEntry e : file.staticPool) {
				pool.add(new StaticChoice(e));
			}
			for (ProfessionTradeFile.WeaponPoolEntry w : file.weaponPool) {
				pool.add(new WeaponChoice(w));
			}
			for (ProfessionTradeFile.ToolPoolEntry t : file.toolPool) {
				pool.add(new ToolChoice(t));
			}
			for (ProfessionTradeFile.GeneralPoolEntry g : file.generalPool) {
				pool.add(new GeneralChoice(g));
			}
		}

		// Добавляем зачарованные книги для библиотекаря (2 отдельных слота)
		if (file.enchantments != null && !file.enchantments.isEmpty()) {
			pool.add(new EnchantmentChoice(file.enchantments, villagerLevel));
			pool.add(new EnchantmentChoice(file.enchantments, villagerLevel));
		}

		TradeOverhaulMod.LOGGER.info("Restock: total pool size={}", pool.size());
		TradeOverhaulMod.LOGGER.info("=== END RESTOCK DEBUG ===");
		
		if (pool.isEmpty()) {
			TradeOverhaulMod.LOGGER.warn("Restock: pool is empty! No items will be stocked.");
			data.tradeOverhaul$setOfferSlots(new int[0]);
			data.tradeOverhaul$setEmptySinceTick(-1L);
			return;
		}

		Random random = villager.getRandom();
		shuffleList(pool, random);
		int want = file.offersCount != null ? file.offersCount : 7;
		want = Math.min(want, pool.size());
		want = Math.min(want, inv.size());
		List<PoolChoice> picked = new ArrayList<>(pool.subList(0, want));

		// Генерируем все предметы сначала
		List<ItemStack> generatedStacks = new ArrayList<>();
		for (int i = 0; i < want; i++) {
			ItemStack stack = picked.get(i).createStack(random, settings);
			if (!stack.isEmpty()) {
				generatedStacks.add(stack);
			}
		}

		// Объединяем одинаковые стакающиеся предметы
		List<ItemStack> mergedStacks = mergeSimilarItems(generatedStacks);

		// Распределяем по слотам
		int[] slots = new int[mergedStacks.size()];
		for (int i = 0; i < mergedStacks.size(); i++) {
			slots[i] = i;
			inv.setStack(i, mergedStacks.get(i));
		}

		data.tradeOverhaul$setOfferSlots(slots);
		
		// Пополнение кошелька жителя в зависимости от уровня
		if (data.tradeOverhaul$getCurrency() != null) {
			int currentCopper = data.tradeOverhaul$getCurrency().getTotalCopper();
			
			// Получаем настройки денег для текущего уровня
			int restockMoney = getRestockMoneyForLevel(file, villagerLevel, settings);
			
			// Добавляем деньги только если у жителя меньше положенного по уровню
			if (currentCopper < restockMoney) {
				int moneyToAdd = restockMoney - currentCopper;
				data.tradeOverhaul$getCurrency().addMoney(moneyToAdd);
			}
		}

		data.tradeOverhaul$setEmptySinceTick(-1L);
	}
	
	/**
	 * Возвращает количество денег для рестокa в зависимости от уровня жителя
	 */
	private static int getRestockMoneyForLevel(ProfessionTradeFile file, int level, TradeOverhaulSettings settings) {
		if (file.levelMoneySettings != null) {
			return switch (level) {
				case 2 -> file.levelMoneySettings.level2RestockMoney != null ? 
					file.levelMoneySettings.level2RestockMoney : 700;
				case 3 -> file.levelMoneySettings.level3RestockMoney != null ? 
					file.levelMoneySettings.level3RestockMoney : 1100;
				case 4 -> file.levelMoneySettings.level4RestockMoney != null ? 
					file.levelMoneySettings.level4RestockMoney : 1600;
				case 5 -> file.levelMoneySettings.level5RestockMoney != null ? 
					file.levelMoneySettings.level5RestockMoney : 2500;
				default -> file.levelMoneySettings.level1RestockMoney != null ? 
					file.levelMoneySettings.level1RestockMoney : 400;
			};
		}
		// Значения по умолчанию
		return switch (level) {
			case 2 -> 700;
			case 3 -> 1100;
			case 4 -> 1600;
			case 5 -> 2500;
			default -> 400;
		};
	}

	/**
	 * Объединяет одинаковые стакающиеся предметы в одни слоты.
	 * Нестекающиеся предметы (maxCount=1) остаются раздельными.
	 */
	private static List<ItemStack> mergeSimilarItems(List<ItemStack> stacks) {
		List<ItemStack> result = new ArrayList<>();
		
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) continue;
			
			// Нестекающиеся предметы добавляем как есть
			if (stack.getMaxCount() <= 1) {
				result.add(stack.copy());
				continue;
			}
			
			// Пытаемся добавить к существующему такому же предмету
			boolean merged = false;
			for (ItemStack existing : result) {
				if (ItemStack.canCombine(existing, stack)) {
					int combined = existing.getCount() + stack.getCount();
					if (combined <= existing.getMaxCount()) {
						existing.setCount(combined);
						merged = true;
						break;
					}
				}
			}
			
			// Если не удалось объединить, добавляем как новый слот
			if (!merged) {
				result.add(stack.copy());
			}
		}
		
		return result;
	}

	private static <T> void shuffleList(List<T> list, Random random) {
		for (int i = list.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Collections.swap(list, i, j);
		}
	}

	private interface PoolChoice {
		ItemStack createStack(Random random, TradeOverhaulSettings settings);
	}

	private record StaticChoice(ProfessionTradeFile.StaticPoolEntry entry) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			Identifier id = Identifier.tryParse(entry.item);
			if (id == null) return ItemStack.EMPTY;
			Item item = Registries.ITEM.get(id);
			if (item == Items.AIR) return ItemStack.EMPTY;

			// Получаем количество за 1 изумруд (для кратности)
			int quantity = entry.buyQuantity != null && entry.buyQuantity > 0 ? entry.buyQuantity : 1;

			int min = entry.minStock != null ? entry.minStock : settings.maxStockDefault;
			int max = entry.maxStock != null ? entry.maxStock : settings.maxStockDefault;
			min = Math.max(quantity, min);  // Минимум кратен quantity
			max = Math.max(min, max);

			// Генерируем количество, кратное quantity
			int multiples = (max / quantity) - (min / quantity) + 1;
			int count = (random.nextInt(multiples) + (min / quantity)) * quantity;
			count = Math.max(min, Math.min(max, count));

			ItemStack stack = new ItemStack(item, count);
			
			// Обработка зачарованных книг
			if (item == Items.ENCHANTED_BOOK && entry.enchantment != null) {
				Identifier enchantId = Identifier.tryParse(entry.enchantment);
				if (enchantId != null) {
					net.minecraft.enchantment.Enchantment enchant = Registries.ENCHANTMENT.get(enchantId);
					if (enchant != null) {
						int level = entry.enchantment_level != null ? entry.enchantment_level : 1;
						stack.addEnchantment(enchant, level);
					}
				}
			}
			
			return stack;
		}
	}

	private record WeaponChoice(ProfessionTradeFile.WeaponPoolEntry w) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			if (w.tag == null) return ItemStack.EMPTY;
			Identifier tagId = Identifier.tryParse(w.tag);
			if (tagId == null) return ItemStack.EMPTY;
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
			List<Item> candidates = new ArrayList<>();
			for (Item item : Registries.ITEM.stream().toList()) {
				if (item == Items.EMERALD) continue;
				ItemStack probe = new ItemStack(item);
				if (probe.isIn(tag)) {
					// Исключаем незеритовое оружие
					if (ItemCombatPricing.getMaterialTier(probe) >= 4) continue;
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			int min = w.minStock != null ? w.minStock : settings.maxStockDefault;
			int max = w.maxStock != null ? w.maxStock : settings.maxStockDefault;
			min = Math.max(1, min);
			max = Math.max(min, max);
			// Ограничиваем максимальным стаком предмета
			int maxStack = pick.getMaxCount();
			min = Math.min(min, maxStack);
			max = Math.min(max, maxStack);
			int count = min == max ? min : random.nextInt(max - min + 1) + min;
			return new ItemStack(pick, count);
		}
	}

	private record ToolChoice(ProfessionTradeFile.ToolPoolEntry t) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			if (t.tag == null) return ItemStack.EMPTY;
			Identifier tagId = Identifier.tryParse(t.tag);
			if (tagId == null) return ItemStack.EMPTY;
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
			List<Item> candidates = new ArrayList<>();
			for (Item item : Registries.ITEM.stream().toList()) {
				if (item == Items.EMERALD) continue;
				ItemStack probe = new ItemStack(item);
				if (probe.isIn(tag)) {
					// Исключаем незеритовые инструменты
					if (ItemCombatPricing.getMaterialTier(probe) >= 4) continue;
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			int min = t.minStock != null ? t.minStock : settings.maxStockDefault;
			int max = t.maxStock != null ? t.maxStock : settings.maxStockDefault;
			min = Math.max(1, min);
			max = Math.max(min, max);
			// Ограничиваем максимальным стаком предмета
			int maxStack = pick.getMaxCount();
			min = Math.min(min, maxStack);
			max = Math.min(max, maxStack);
			int count = min == max ? min : random.nextInt(max - min + 1) + min;
			return new ItemStack(pick, count);
		}
	}

	private record GeneralChoice(ProfessionTradeFile.GeneralPoolEntry g) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			if (g.tag == null) return ItemStack.EMPTY;
			Identifier tagId = Identifier.tryParse(g.tag);
			if (tagId == null) return ItemStack.EMPTY;
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
			List<Item> candidates = new ArrayList<>();
			for (Item item : Registries.ITEM.stream().toList()) {
				if (item == Items.EMERALD) continue;
				ItemStack probe = new ItemStack(item);
				if (probe.isIn(tag)) {
					// Исключаем незеритовые предметы
					if (ItemCombatPricing.getMaterialTier(probe) >= 4) continue;
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			
			// Получаем количество за 1 изумруд (для кратности)
			int quantity = g.buyQuantity != null && g.buyQuantity > 0 ? g.buyQuantity : 1;
			
			int min = g.minStock != null ? g.minStock : settings.maxStockDefault;
			int max = g.maxStock != null ? g.maxStock : settings.maxStockDefault;
			min = Math.max(quantity, min);  // Минимум кратен quantity
			max = Math.max(min, max);
			
			// Генерируем количество, кратное quantity
			int multiples = (max / quantity) - (min / quantity) + 1;
			int count = (random.nextInt(multiples) + (min / quantity)) * quantity;
			count = Math.max(min, Math.min(max, count));
			
			return new ItemStack(pick, count);
		}
	}
	
	/**
	 * Генерирует случайную зачарованную книгу для библиотекаря.
	 * Каждая книга генерируется независимо с рандомным зачарованием и уровнем.
	 * Уровень зачарования зависит от уровня жителя.
	 */
	private record EnchantmentChoice(List<ProfessionTradeFile.EnchantmentEntry> enchantments, int villagerLevel) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			if (enchantments == null || enchantments.isEmpty()) return ItemStack.EMPTY;

			// Выбираем случайное зачарование
			ProfessionTradeFile.EnchantmentEntry entry = enchantments.get(random.nextInt(enchantments.size()));

			Identifier enchantId = Identifier.tryParse(entry.enchantment);
			if (enchantId == null) return ItemStack.EMPTY;

			net.minecraft.enchantment.Enchantment enchant = Registries.ENCHANTMENT.get(enchantId);
			if (enchant == null) return ItemStack.EMPTY;

			// Выбираем случайный уровень в зависимости от уровня жителя
			// Уровень 1: только минимальные уровни зачарований
			// Уровень 5: могут быть максимальные уровни
			int minLevel = entry.min_level != null ? entry.min_level : 1;
			int maxLevel = entry.max_level != null ? entry.max_level : 1;
			
			// Ограничиваем максимальный уровень зачарования уровнем жителя
			// Уровень 1: до 20% от maxLevel
			// Уровень 2: до 40% от maxLevel
			// Уровень 3: до 60% от maxLevel
			// Уровень 4: до 80% от maxLevel
			// Уровень 5: 100% от maxLevel
			float levelFactor = switch (villagerLevel) {
				case 2 -> 0.4f;
				case 3 -> 0.6f;
				case 4 -> 0.8f;
				case 5 -> 1.0f;
				default -> 0.2f;
			};
			
			int effectiveMaxLevel = Math.max(minLevel, (int) (minLevel + (maxLevel - minLevel) * levelFactor));
			int level = minLevel == effectiveMaxLevel ? minLevel : random.nextInt(effectiveMaxLevel - minLevel + 1) + minLevel;

			// Создаём зачарованную книгу с правильным NBT (StoredEnchantments)
			ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
			net.minecraft.nbt.NbtCompound nbt = book.getOrCreateNbt();

			// Создаём список зачарований
			net.minecraft.nbt.NbtList enchantList = new net.minecraft.nbt.NbtList();
			net.minecraft.nbt.NbtCompound enchantNbt = new net.minecraft.nbt.NbtCompound();

			// Записываем ID зачарования и уровень
			enchantNbt.putString("id", enchantId.toString());
			enchantNbt.putShort("lvl", (short) level);

			enchantList.add(enchantNbt);
			nbt.put("StoredEnchantments", enchantList);

			book.setNbt(nbt);
			book.setCount(1);

			return book;
		}
	}
	
	/**
	 * Выбор из пула определённого уровня.
	 */
	private record LevelChoice(ProfessionTradeFile.LevelPoolEntry entry, int level) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			// Предмет из статического пула
			if (entry.item != null) {
				Identifier id = Identifier.tryParse(entry.item);
				if (id == null) return ItemStack.EMPTY;
				net.minecraft.item.Item item = Registries.ITEM.get(id);
				if (item == Items.AIR) return ItemStack.EMPTY;

				int min = entry.minStock != null ? entry.minStock : settings.maxStockDefault;
				int max = entry.maxStock != null ? entry.maxStock : settings.maxStockDefault;
				min = Math.max(1, min);
				max = Math.max(min, max);
				int count = min == max ? min : random.nextInt(max - min + 1) + min;
				
				ItemStack stack = new ItemStack(item, count);
				
				// Обработка зачарованных книг
				if (item == Items.ENCHANTED_BOOK && entry.enchantment != null) {
					Identifier enchantId = Identifier.tryParse(entry.enchantment);
					if (enchantId != null) {
						net.minecraft.enchantment.Enchantment enchant = Registries.ENCHANTMENT.get(enchantId);
						if (enchant != null) {
							int enchantLevel = entry.enchantment_level != null ? entry.enchantment_level : 1;
							stack.addEnchantment(enchant, enchantLevel);
						}
					}
				}
				
				return stack;
			}
			
			// Предмет из общего пула (по тегу)
			if (entry.tag != null) {
				Identifier tagId = Identifier.tryParse(entry.tag);
				if (tagId == null) return ItemStack.EMPTY;
				TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
				List<net.minecraft.item.Item> candidates = new ArrayList<>();
				for (net.minecraft.item.Item item : Registries.ITEM.stream().toList()) {
					if (item == Items.EMERALD) continue;
					ItemStack probe = new ItemStack(item);
					if (probe.isIn(tag)) {
						// Исключаем незеритовые предметы
						if (ItemCombatPricing.getMaterialTier(probe) >= 4) continue;
						candidates.add(item);
					}
				}
				if (candidates.isEmpty()) return ItemStack.EMPTY;
				net.minecraft.item.Item pick = candidates.get(random.nextInt(candidates.size()));
				
				int min = entry.minStock != null ? entry.minStock : settings.maxStockDefault;
				int max = entry.maxStock != null ? entry.maxStock : settings.maxStockDefault;
				min = Math.max(1, min);
				max = Math.max(min, max);
				int maxStack = pick.getMaxCount();
				min = Math.min(min, maxStack);
				max = Math.min(max, maxStack);
				int count = min == max ? min : random.nextInt(max - min + 1) + min;
				
				return new ItemStack(pick, count);
			}
			
			return ItemStack.EMPTY;
		}
	}
}
