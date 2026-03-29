package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** C2S торговля через {@link PacketByteBuf} (1.20.1). */
public record TradePayload(int syncId, int slot, boolean isBuying, boolean sellWholeStack, boolean buyWholeStack) {
	public static final Identifier ID = new Identifier(TradeOverhaulMod.MOD_ID, "trade");

	public void write(PacketByteBuf buf) {
		buf.writeInt(syncId);
		buf.writeInt(slot);
		buf.writeBoolean(isBuying);
		buf.writeBoolean(sellWholeStack);
		buf.writeBoolean(buyWholeStack);
	}

	public static TradePayload read(PacketByteBuf buf) {
		return new TradePayload(buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
	}
}
