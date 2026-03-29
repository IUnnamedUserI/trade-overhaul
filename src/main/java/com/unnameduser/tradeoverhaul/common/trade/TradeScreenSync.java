package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import com.unnameduser.tradeoverhaul.common.numismatic.NumismaticHelper;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class TradeScreenSync {
	private TradeScreenSync() {}

	public static void write(PacketByteBuf buf, VillagerEntity villager, ProfessionTradeFile profession) {
		// Синхронизация кошелька Numismatic
		buf.writeVarInt(NumismaticHelper.getTotalMoney(villager));

		// Синхронизация инвентаря жителя с buyQuantity и sellQuantity
		VillagerInventoryComponent inv = ((VillagerTradeData) villager).tradeOverhaul$getInventory();
		buf.writeVarInt(inv.size());
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			buf.writeItemStack(stack);
			// Пишем buyQuantity и sellQuantity для этого предмета
			int buyQty = 1;
			int sellQty = 1;
			if (profession != null && !stack.isEmpty()) {
				buyQty = TradePricing.getBuyQuantity(stack, villager, profession);
				sellQty = TradePricing.getSellQuantity(stack, profession);
			}
			buf.writeVarInt(buyQty);
			buf.writeVarInt(sellQty);
		}
		
		TradeOverhaulSettings s = TradeConfigLoader.getSettings();
		if (profession == null) {
			buf.writeVarInt(0);
			buf.writeVarInt(0);
			buf.writeVarInt(0);
			buf.writeVarInt(0);
			return;
		}
		buf.writeVarInt(profession.staticPool.size());
		for (ProfessionTradeFile.StaticPoolEntry e : profession.staticPool) {
			Identifier itemId = Identifier.tryParse(e.item);
			if (itemId == null) {
				buf.writeIdentifier(new Identifier("minecraft", "air"));
				buf.writeVarInt(1);
				buf.writeVarInt(0);
				continue;
			}
			buf.writeIdentifier(itemId);
			buf.writeVarInt(e.buy);
			buf.writeVarInt(e.sell);
		}
		buf.writeVarInt(profession.weaponPool.size());
		for (ProfessionTradeFile.WeaponPoolEntry w : profession.weaponPool) {
			Identifier tagId = Identifier.tryParse(w.tag != null ? w.tag : "minecraft:air");
			buf.writeIdentifier(tagId);
			buf.writeVarInt(w.maxStock != null ? w.maxStock : s.maxStockDefault);
			buf.writeVarInt(w.buyPerDamage != null ? w.buyPerDamage : s.weaponBuyPerDamage);
			buf.writeVarInt(w.sellPerDamage != null ? w.sellPerDamage : s.weaponSellPerDamage);
			buf.writeVarInt(w.buyBase != null ? w.buyBase : s.weaponBuyBase);
			buf.writeVarInt(w.sellBase != null ? w.sellBase : s.weaponSellBase);
		}
		buf.writeVarInt(profession.toolPool.size());
		for (ProfessionTradeFile.ToolPoolEntry t : profession.toolPool) {
			Identifier tagId = Identifier.tryParse(t.tag != null ? t.tag : "minecraft:air");
			buf.writeIdentifier(tagId);
			buf.writeVarInt(t.minStock != null ? t.minStock : s.maxStockDefault);
			buf.writeVarInt(t.maxStock != null ? t.maxStock : s.maxStockDefault);
			buf.writeVarInt(t.buyPerEfficiency != null ? t.buyPerEfficiency : s.toolBuyPerEfficiency);
			buf.writeVarInt(t.sellPerEfficiency != null ? t.sellPerEfficiency : s.toolSellPerEfficiency);
			buf.writeVarInt(t.buyBase != null ? t.buyBase : s.toolBuyBase);
			buf.writeVarInt(t.sellBase != null ? t.sellBase : s.toolSellBase);
		}
		buf.writeVarInt(profession.generalPool.size());
		for (ProfessionTradeFile.GeneralPoolEntry g : profession.generalPool) {
			Identifier tagId = Identifier.tryParse(g.tag != null ? g.tag : "minecraft:air");
			buf.writeIdentifier(tagId);
			buf.writeVarInt(g.minStock != null ? g.minStock : s.maxStockDefault);
			buf.writeVarInt(g.maxStock != null ? g.maxStock : s.maxStockDefault);
			buf.writeVarInt(g.buyPrice != null ? g.buyPrice : 1);
			buf.writeVarInt(g.sellPrice != null ? g.sellPrice : 1);
		}
	}

	public static class SyncedInventory {
		public final VillagerInventoryComponent inventory;
		public final int[] buyQuantities;
		public final int[] sellQuantities;

		public SyncedInventory(VillagerInventoryComponent inventory, int[] buyQuantities, int[] sellQuantities) {
			this.inventory = inventory;
			this.buyQuantities = buyQuantities;
			this.sellQuantities = sellQuantities;
		}
	}

	public static SyncedInventory readInventory(PacketByteBuf buf) {
		int size = buf.readVarInt();
		VillagerInventoryComponent inv = new VillagerInventoryComponent();
		int[] buyQuantities = new int[size];
		int[] sellQuantities = new int[size];
		for (int i = 0; i < size; i++) {
			inv.setStack(i, buf.readItemStack());
			buyQuantities[i] = buf.readVarInt();
			sellQuantities[i] = buf.readVarInt();
		}
		return new SyncedInventory(inv, buyQuantities, sellQuantities);
	}
}
