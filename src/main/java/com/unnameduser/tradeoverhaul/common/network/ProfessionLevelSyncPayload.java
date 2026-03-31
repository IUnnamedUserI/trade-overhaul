package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** S2C синхронизация уровня профессии жителя */
public record ProfessionLevelSyncPayload(int syncId, int level, int experience, int tradesCompleted, float fractionalXp, NbtCompound soldItemsTracker) {
	public static final Identifier ID = new Identifier(TradeOverhaulMod.MOD_ID, "profession_level_sync");

	public void write(PacketByteBuf buf) {
		buf.writeInt(syncId);
		buf.writeVarInt(level);
		buf.writeVarInt(experience);
		buf.writeVarInt(tradesCompleted);
		buf.writeFloat(fractionalXp);
		buf.writeNbt(soldItemsTracker);
	}

	public static ProfessionLevelSyncPayload read(PacketByteBuf buf) {
		return new ProfessionLevelSyncPayload(
			buf.readInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readFloat(),
			buf.readNbt()
		);
	}
}
