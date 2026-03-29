package com.unnameduser.tradeoverhaul.common.trade;

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

		List<PoolChoice> pool = new ArrayList<>();
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
		if (pool.isEmpty()) {
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
		// Рандомизация изумрудов: обновляем только если у жителя меньше минимума
		int currentEmeralds = data.tradeOverhaul$getWalletEmeralds();
		if (currentEmeralds < settings.walletAfterRestockMin) {
			int newEmeralds = settings.walletAfterRestockMin + random.nextInt(
				settings.walletAfterRestockMax - settings.walletAfterRestockMin + 1
			);
			data.tradeOverhaul$setWalletEmeralds(newEmeralds);
		}
		// Если изумрудов >= walletAfterRestockMin, не трогаем кошелёк (житель "богатый")
		
		// Пополнение монет жителя (3-6 серебряных + 0-99 медных)
		if (data.tradeOverhaul$getCurrency() != null) {
			int currentCopper = data.tradeOverhaul$getCurrency().getTotalCopper();
			// Добавляем только если у жителя меньше 3 серебряных (300 медных)
			if (currentCopper < 300) {
				int silverToAdd = 3 + random.nextInt(4); // 3-6 серебряных = 300-600 медных
				int copperToAdd = random.nextInt(100);  // 0-99 медных
				int totalToAdd = silverToAdd * 100 + copperToAdd;
				data.tradeOverhaul$getCurrency().addMoney(totalToAdd);
			}
		}
		
		data.tradeOverhaul$setEmptySinceTick(-1L);
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
			
			return new ItemStack(item, count);
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
}
