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

		// ПРИМЕЧАНИЕ: Данные о профессии (уровень, опыт, трекинг) теперь передаются в writeScreenOpeningData

		// Синхронизация инвентаря жителя
		VillagerInventoryComponent inv = ((VillagerTradeData) villager).tradeOverhaul$getInventory();
		buf.writeVarInt(inv.size());
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			buf.writeItemStack(stack);
		}

		TradeOverhaulSettings s = TradeConfigLoader.getSettings();
		if (profession == null) {
			buf.writeVarInt(0); // staticPrices count
			buf.writeVarInt(0); // weaponRules count
			buf.writeVarInt(0); // toolRules count
			buf.writeVarInt(0); // generalRules count
			return;
		}

		// Пишем legacy staticPrices (для обратной совместимости клиента)
		buf.writeVarInt(0);
		// Пишем legacy weaponRules
		buf.writeVarInt(0);
		// Пишем legacy toolRules
		buf.writeVarInt(0);
		// Пишем legacy generalRules
		buf.writeVarInt(0);
	}

	public static class SyncedInventory {
		public final VillagerInventoryComponent inventory;

		public SyncedInventory(VillagerInventoryComponent inventory) {
			this.inventory = inventory;
		}
	}

	public static SyncedInventory readInventory(PacketByteBuf buf) {
		int size = buf.readVarInt();
		VillagerInventoryComponent inv = new VillagerInventoryComponent();
		for (int i = 0; i < size; i++) {
			inv.setStack(i, buf.readItemStack());
		}
		return new SyncedInventory(inv);
	}
}
