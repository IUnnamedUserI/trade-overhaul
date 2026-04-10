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

	/**
	 * Подготавливает и выполняет ресток для одного жителя.
	 * Вызывается из GlobalRestockTimer для каждого жителя.
	 */
	public static void prepareRestock(VillagerEntity villager) {
		Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		ProfessionTradeFile file = TradeConfigLoader.getProfession(profId);
		TradeOverhaulSettings settings = TradeConfigLoader.getSettings();

		if (file == null) return;

		VillagerTradeData data = (VillagerTradeData) villager;
		VillagerInventoryComponent inv = data.tradeOverhaul$getInventory();
		performRestock(villager, file, settings, inv, data);
	}

	/**
	 * Принудительное обновление торговли (для команды /tradeoverhaul refresh)
	 */
	public static void forceRestock(VillagerEntity villager, ProfessionTradeFile file, TradeOverhaulSettings settings) {
		VillagerTradeData data = (VillagerTradeData) villager;
		VillagerInventoryComponent inv = data.tradeOverhaul$getInventory();
		performRestock(villager, file, settings, inv, data);
	}

	private static void performRestock(VillagerEntity villager, ProfessionTradeFile file, TradeOverhaulSettings settings,
			VillagerInventoryComponent inv, VillagerTradeData data) {
		// Полная очистка инвентаря жителя перед новым restock
		for (int i = 0; i < inv.size(); i++) {
			inv.setStack(i, ItemStack.EMPTY);
		}

		// Применяем деградацию опыта, если рабочий блок потерян
		data.tradeOverhaul$getProfession().applyWorkstationDecay();

		// Сбрасываем репутацию урона (штрафы за урон действуют только до рестокa)
		data.tradeOverhaul$getProfession().resetDamageReputation();

		// Проверяем, потерял ли житель профессию полностью
		if (data.tradeOverhaul$getProfession().shouldLoseProfession()) {
			TradeOverhaulMod.LOGGER.info("Villager {} lost profession entirely due to workstation decay", villager.getUuid());
			villager.setVillagerData(villager.getVillagerData()
				.withProfession(net.minecraft.village.VillagerProfession.NONE)
				.withLevel(1));
			// Сбрасываем компонент профессии
			data.tradeOverhaul$getProfession().resetProfession();
			// Отменяем ресток — у жителя больше нет профессии
			data.tradeOverhaul$setOfferSlots(new int[0]);
			data.tradeOverhaul$setEmptySinceTick(-1L);
			return;
		}

		// Получаем уровень жителя из компонента профессии (после деградации!)
		int villagerLevel = data.tradeOverhaul$getProfession().getLevel();

		// Синхронизируем ванильный уровень с модом (если мод-уровень ниже после деградации)
		int vanillaLevel = villager.getVillagerData().getLevel();
		if (villagerLevel < vanillaLevel) {
			villager.setVillagerData(villager.getVillagerData().withLevel(villagerLevel));
		}
		
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
					pool.add(new LevelChoice(e, file, 1));
				}
				TradeOverhaulMod.LOGGER.info("Added level1Pool: {} items", file.level1Pool.size());
			} else if (villagerLevel >= 1 && file.level1Pool == null) {
				TradeOverhaulMod.LOGGER.warn("villagerLevel >= 1 but level1Pool is null for profession {}", Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()));
			}
			if (villagerLevel >= 2 && file.level2Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level2Pool) {
					pool.add(new LevelChoice(e, file, 2));
				}
				TradeOverhaulMod.LOGGER.info("Added level2Pool: {} items", file.level2Pool.size());
			}
			if (villagerLevel >= 3 && file.level3Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level3Pool) {
					pool.add(new LevelChoice(e, file, 3));
				}
				TradeOverhaulMod.LOGGER.info("Added level3Pool: {} items", file.level3Pool.size());
			}
			if (villagerLevel >= 4 && file.level4Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level4Pool) {
					pool.add(new LevelChoice(e, file, 4));
				}
				TradeOverhaulMod.LOGGER.info("Added level4Pool: {} items", file.level4Pool.size());
			}
			if (villagerLevel >= 5 && file.level5Pool != null) {
				for (ProfessionTradeFile.LevelPoolEntry e : file.level5Pool) {
					pool.add(new LevelChoice(e, file, 5));
				}
				TradeOverhaulMod.LOGGER.info("Added level5Pool: {} items", file.level5Pool.size());
			}
		}

		// Добавляем зачарованные книги для библиотекаря (динамическое количество: 2 на ур. 1, 6 на ур. 5)
		if (file.enchantments != null && !file.enchantments.isEmpty()) {
			int bookCount = villagerLevel + 1; // Уровень 1: 2 книги, Уровень 5: 6 книг
			for (int i = 0; i < bookCount; i++) {
				pool.add(new EnchantmentChoice(file.enchantments, villagerLevel));
			}
			TradeOverhaulMod.LOGGER.info("Added {} enchantment books for villager level {}", bookCount, villagerLevel);
		}

		// Добавляем предметы из buyPool (что житель хочет купить у игрока)
		if (file.buyPool != null && !file.buyPool.isEmpty()) {
			for (ProfessionTradeFile.BuyOnlyEntry b : file.buyPool) {
				pool.add(new BuyOnlyChoice(b));
			}
			TradeOverhaulMod.LOGGER.info("Added buyPool: {} items", file.buyPool.size());
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
	private record LevelChoice(ProfessionTradeFile.LevelPoolEntry entry, ProfessionTradeFile profession, int villagerLevel) implements PoolChoice {
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

				// Обработка зачарованных книг (старая система)
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
				// Новая система: случайные зачарования
				else if (entry.enchant != null && entry.enchant) {
					applyRandomEnchantments(stack, random);
				}
				// Старая система: конкретное зачарование (обратная совместимость)
				else if (entry.enchantment != null) {
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

				ItemStack stack = new ItemStack(pick, count);

				// Новая система: случайные зачарования для предметов из тега
				if (entry.enchant != null && entry.enchant) {
					applyRandomEnchantments(stack, random);
				}

				return stack;
			}

			return ItemStack.EMPTY;
		}

		/**
		 * Применяет случайные зачарования к предмету.
		 * Шанс, уровень и кол-во берутся из entry или из настроек по умолчанию.
		 * Максимальный уровень каждого зачарования берётся из EnchantmentSpec.
		 */
		private void applyRandomEnchantments(ItemStack stack, Random random) {
			if (stack.isEmpty()) return;

			// Определяем параметры зачарования
			int chance = entry.enchantChance != null ? entry.enchantChance : 70;
			int maxCount = entry.enchantMaxCount != null ? entry.enchantMaxCount : 3;
			int minLevel = entry.enchantMinLevel != null ? entry.enchantMinLevel : 1;
			int maxLevel = entry.enchantMaxLevel != null ? entry.enchantMaxLevel : villagerLevel;

			// Проверяем шанс зачарования
			if (random.nextInt(100) >= chance) {
				return; // Предмет остаётся без зачарований
			}

			// Получаем подходящий список зачарований
			List<ProfessionTradeFile.EnchantmentSpec> enchantSpecList = getEnchantmentSpecsForStack(stack);
			if (enchantSpecList == null || enchantSpecList.isEmpty()) return;

			// Определяем количество зачарований (1 до maxCount)
			int numEnchants = random.nextInt(maxCount) + 1;
			numEnchants = Math.min(numEnchants, enchantSpecList.size());

			// Перемешиваем и выбираем случайные зачарования
			List<ProfessionTradeFile.EnchantmentSpec> shuffled = new ArrayList<>(enchantSpecList);
			for (int i = shuffled.size() - 1; i > 0; i--) {
				int j = random.nextInt(i + 1);
				Collections.swap(shuffled, i, j);
			}

			// Применяем зачарования
			for (int i = 0; i < numEnchants && i < shuffled.size(); i++) {
				ProfessionTradeFile.EnchantmentSpec spec = shuffled.get(i);
				Identifier enchantId = Identifier.tryParse(spec.id);
				if (enchantId == null) continue;

				net.minecraft.enchantment.Enchantment enchant = Registries.ENCHANTMENT.get(enchantId);
				if (enchant == null) continue;

				// Определяем уровень зачарования: ограничиваем spec.getEffectiveMaxLevel() и maxLevel из entry
				int effectiveMaxLevel = Math.min(spec.getEffectiveMaxLevel(enchant), maxLevel);
				int level = minLevel == effectiveMaxLevel ? minLevel : random.nextInt(effectiveMaxLevel - minLevel + 1) + minLevel;

				// Ограничиваем уровень максимальным/минимальным для зачарования
				level = Math.min(level, enchant.getMaxLevel());
				level = Math.max(level, enchant.getMinLevel());

				stack.addEnchantment(enchant, level);
			}
		}

		/**
		 * Возвращает список спецификаций зачарований для данного предмета.
		 * Определяет тип предмета (оружие/инструмент/броня/луки/арбалеты) и возвращает соответствующий список.
		 * Для брони учитывается конкретный слот (шлем/нагрудник/поножи/ботинки).
		 */
		private List<ProfessionTradeFile.EnchantmentSpec> getEnchantmentSpecsForStack(ItemStack stack) {
			net.minecraft.item.Item item = stack.getItem();

			// Определяем тип предмета
			boolean isWeapon = isWeaponItem(item);
			boolean isTool = isToolItem(item);
			boolean isArmor = isArmorItem(item);
			boolean isBow = isBowItem(item);
			boolean isCrossbow = isCrossbowItem(item);

			// Возвращаем соответствующий список
			if (isWeapon && profession.weaponEnchantments != null && !profession.weaponEnchantments.isEmpty()) {
				return profession.weaponEnchantments;
			}
			if (isTool && profession.toolEnchantments != null && !profession.toolEnchantments.isEmpty()) {
				return profession.toolEnchantments;
			}
			if (isBow && profession.bowEnchantments != null && !profession.bowEnchantments.isEmpty()) {
				return profession.bowEnchantments;
			}
			if (isCrossbow && profession.crossbowEnchantments != null && !profession.crossbowEnchantments.isEmpty()) {
				return profession.crossbowEnchantments;
			}
			if (isArmor) {
				// Проверяем специфичные зачарования для слота брони
				List<ProfessionTradeFile.EnchantmentSpec> slotSpecific = getArmorSlotEnchantments(item);
				if (slotSpecific != null && !slotSpecific.isEmpty()) {
					return slotSpecific;
				}
				// Fallback на общие зачарования брони
				if (profession.armorEnchantments != null && !profession.armorEnchantments.isEmpty()) {
					return profession.armorEnchantments;
				}
			}

			// Если тип не определён или список пуст — пробуем все доступные
			List<ProfessionTradeFile.EnchantmentSpec> result = new ArrayList<>();
			if (profession.weaponEnchantments != null) result.addAll(profession.weaponEnchantments);
			if (profession.toolEnchantments != null) result.addAll(profession.toolEnchantments);
			if (profession.bowEnchantments != null) result.addAll(profession.bowEnchantments);
			if (profession.crossbowEnchantments != null) result.addAll(profession.crossbowEnchantments);
			if (profession.armorEnchantments != null) result.addAll(profession.armorEnchantments);
			// Добавляем специфичные зачарования брони
			if (profession.helmetEnchantments != null) result.addAll(profession.helmetEnchantments);
			if (profession.chestplateEnchantments != null) result.addAll(profession.chestplateEnchantments);
			if (profession.leggingsEnchantments != null) result.addAll(profession.leggingsEnchantments);
			if (profession.bootsEnchantments != null) result.addAll(profession.bootsEnchantments);
			return result.isEmpty() ? null : result;
		}

		/**
		 * Возвращает список зачарований для конкретного слота брони.
		 * Приоритет: специфичные для слота > null если нет специфичных.
		 */
		private List<ProfessionTradeFile.EnchantmentSpec> getArmorSlotEnchantments(net.minecraft.item.Item item) {
			net.minecraft.item.ArmorItem armorItem = null;
			if (item instanceof net.minecraft.item.ArmorItem) {
				armorItem = (net.minecraft.item.ArmorItem) item;
			} else if (item instanceof net.minecraft.item.ShieldItem || item instanceof net.minecraft.item.ElytraItem) {
				// Щит и элитры не имеют специфичных зачарований, используем общие
				return null;
			}

			if (armorItem == null) return null;

			// Определяем слот брони
			net.minecraft.entity.EquipmentSlot slot = armorItem.getSlotType();

			// Возвращаем специфичные зачарования для слота
			return switch (slot) {
				case HEAD -> profession.helmetEnchantments;
				case CHEST -> profession.chestplateEnchantments;
				case LEGS -> profession.leggingsEnchantments;
				case FEET -> profession.bootsEnchantments;
				default -> null;
			};
		}

		private boolean isWeaponItem(net.minecraft.item.Item item) {
			return item instanceof net.minecraft.item.SwordItem ||
				item instanceof net.minecraft.item.AxeItem ||
				item instanceof net.minecraft.item.TridentItem;
		}

		private boolean isBowItem(net.minecraft.item.Item item) {
			return item instanceof net.minecraft.item.BowItem;
		}

		private boolean isCrossbowItem(net.minecraft.item.Item item) {
			return item instanceof net.minecraft.item.CrossbowItem;
		}

		private boolean isToolItem(net.minecraft.item.Item item) {
			return item instanceof net.minecraft.item.PickaxeItem ||
				item instanceof net.minecraft.item.ShovelItem ||
				item instanceof net.minecraft.item.HoeItem ||
				item instanceof net.minecraft.item.ShearsItem ||
				item instanceof net.minecraft.item.FishingRodItem ||
				item instanceof net.minecraft.item.FlintAndSteelItem;
		}

		private boolean isArmorItem(net.minecraft.item.Item item) {
			return item instanceof net.minecraft.item.ArmorItem ||
				item instanceof net.minecraft.item.ShieldItem ||
				item instanceof net.minecraft.item.ElytraItem;
		}
	}

	/**
	 * Выбор из пула buyPool - предметы, которые житель хочет купить у игрока.
	 * Эти предметы НЕ продаются жителем, а только покупаются.
	 */
	private record BuyOnlyChoice(ProfessionTradeFile.BuyOnlyEntry entry) implements PoolChoice {
		@Override
		public ItemStack createStack(Random random, TradeOverhaulSettings settings) {
			net.minecraft.item.Item pick = null;
			
			// Если указан прямой ID предмета, используем его
			if (entry.item != null) {
				Identifier itemId = Identifier.tryParse(entry.item);
				if (itemId != null) {
					pick = Registries.ITEM.get(itemId);
				}
			}
			
			// Если прямой ID не указан, пробуем найти по тегу
			if (pick == null && entry.tag != null) {
				Identifier tagId = Identifier.tryParse(entry.tag);
				if (tagId != null) {
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
					if (!candidates.isEmpty()) {
						pick = candidates.get(random.nextInt(candidates.size()));
					}
				}
			}
			
			if (pick == null) return ItemStack.EMPTY;
			
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
	}
}
