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
		clearPreviousOffers(inv, data.tradeOverhaul$getOfferSlots());

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

		int[] slots = new int[want];
		for (int i = 0; i < want; i++) {
			slots[i] = i;
		}
		for (int i = 0; i < want; i++) {
			ItemStack stack = picked.get(i).createStack(random, settings);
			if (!stack.isEmpty()) {
				inv.setStack(slots[i], stack);
			}
		}

		data.tradeOverhaul$setOfferSlots(slots);
		data.tradeOverhaul$setWalletEmeralds(settings.walletAfterRestock);
		data.tradeOverhaul$setEmptySinceTick(-1L);
	}

	private static void clearPreviousOffers(VillagerInventoryComponent inv, int[] previous) {
		if (previous == null) return;
		for (int s : previous) {
			if (s >= 0 && s < inv.size()) {
				inv.setStack(s, ItemStack.EMPTY);
			}
		}
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
			int max = entry.maxStock != null ? entry.maxStock : settings.maxStockDefault;
			max = Math.max(1, max);
			return new ItemStack(item, max);
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
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			int max = w.maxStock != null ? w.maxStock : settings.maxStockDefault;
			max = Math.max(1, max);
			return new ItemStack(pick, max);
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
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			int max = t.maxStock != null ? t.maxStock : settings.maxStockDefault;
			max = Math.max(1, max);
			return new ItemStack(pick, max);
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
					candidates.add(item);
				}
			}
			if (candidates.isEmpty()) return ItemStack.EMPTY;
			Item pick = candidates.get(random.nextInt(candidates.size()));
			int max = g.maxStock != null ? g.maxStock : settings.maxStockDefault;
			max = Math.max(1, max);
			return new ItemStack(pick, max);
		}
	}
}
