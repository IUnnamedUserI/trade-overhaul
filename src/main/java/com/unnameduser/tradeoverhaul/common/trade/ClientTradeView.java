package com.unnameduser.tradeoverhaul.common.trade;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientTradeView {
	public final Map<Identifier, PricePair> staticPrices = new HashMap<>();
	public final List<WeaponRule> weaponRules = new ArrayList<>();
	public final List<ToolRule> toolRules = new ArrayList<>();
	public final List<GeneralRule> generalRules = new ArrayList<>();

	public record PricePair(int buy, int sell) {}
	public record WeaponRule(Identifier tag, int maxStock, int buyPerDamage, int sellPerDamage, int buyBase, int sellBase) {}
	public record ToolRule(Identifier tag, int minStock, int maxStock, int buyPerEfficiency, int sellPerEfficiency, int buyBase, int sellBase) {}
	public record GeneralRule(Identifier tag, int minStock, int maxStock, int buyPrice, int sellPrice) {}

	public static ClientTradeView read(PacketByteBuf buf) {
		ClientTradeView v = new ClientTradeView();
		int n = buf.readVarInt();
		for (int i = 0; i < n; i++) {
			Identifier id = buf.readIdentifier();
			int buy = buf.readVarInt();
			int sell = buf.readVarInt();
			v.staticPrices.put(id, new PricePair(buy, sell));
		}
		int w = buf.readVarInt();
		for (int i = 0; i < w; i++) {
			v.weaponRules.add(new WeaponRule(
					buf.readIdentifier(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt()
			));
		}
		int t = buf.readVarInt();
		for (int i = 0; i < t; i++) {
			v.toolRules.add(new ToolRule(
					buf.readIdentifier(),
					buf.readVarInt(),  // minStock
					buf.readVarInt(),  // maxStock
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt()
			));
		}
		int g = buf.readVarInt();
		for (int i = 0; i < g; i++) {
			v.generalRules.add(new GeneralRule(
					buf.readIdentifier(),
					buf.readVarInt(),  // minStock
					buf.readVarInt(),  // maxStock
					buf.readVarInt(),
					buf.readVarInt()
			));
		}
		return v;
	}

	public int buyPrice(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		Identifier id = Registries.ITEM.getId(stack.getItem());
		PricePair p = staticPrices.get(id);
		if (p != null) return Math.max(1, p.buy);
		
		// Weapon pool
		for (WeaponRule rule : weaponRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				float dmg = ItemCombatPricing.attackDamageContribution(stack);
				return Math.max(1, rule.buyBase + Math.round(dmg * rule.buyPerDamage));
			}
		}
		
		// Tool pool
		for (ToolRule rule : toolRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				float eff = ItemCombatPricing.efficiencyContribution(stack);
				return Math.max(1, rule.buyBase + Math.round(eff * rule.buyPerEfficiency));
			}
		}
		
		// General pool
		for (GeneralRule rule : generalRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				return Math.max(1, rule.buyPrice);
			}
		}
		
		return 0;
	}

	public int sellPrice(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		Identifier id = Registries.ITEM.getId(stack.getItem());
		PricePair p = staticPrices.get(id);
		if (p != null) return Math.max(0, Math.min(p.sell, p.buy - 1));
		
		// Weapon pool
		for (WeaponRule rule : weaponRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				float dmg = ItemCombatPricing.attackDamageContribution(stack);
				int buy = buyPrice(stack);
				int sell = Math.max(0, rule.sellBase + Math.round(dmg * rule.sellPerDamage));
				return Math.min(sell, Math.max(0, buy - 1));
			}
		}
		
		// Tool pool
		for (ToolRule rule : toolRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				float eff = ItemCombatPricing.efficiencyContribution(stack);
				int buy = buyPrice(stack);
				int sell = Math.max(0, rule.sellBase + Math.round(eff * rule.sellPerEfficiency));
				return Math.min(sell, Math.max(0, buy - 1));
			}
		}
		
		// General pool
		for (GeneralRule rule : generalRules) {
			TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, rule.tag);
			if (stack.isIn(tag)) {
				int buy = buyPrice(stack);
				int sell = rule.sellPrice;
				return Math.min(sell, Math.max(0, buy - 1));
			}
		}
		
		return 0;
	}
}
