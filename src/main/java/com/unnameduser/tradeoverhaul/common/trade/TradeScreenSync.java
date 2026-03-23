package com.unnameduser.tradeoverhaul.common.trade;

import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class TradeScreenSync {
	private TradeScreenSync() {}

	public static void write(PacketByteBuf buf, VillagerEntity villager, ProfessionTradeFile profession) {
		VillagerTradeData data = (VillagerTradeData) villager;
		buf.writeVarInt(data.tradeOverhaul$getWalletEmeralds());
		TradeOverhaulSettings s = TradeConfigLoader.getSettings();
		if (profession == null) {
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
	}
}
