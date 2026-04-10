package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record DamageReputationSyncPayload(int syncId, java.util.Map<String, Float> damageReputation) {

	public static final Identifier ID = new Identifier("tradeoverhaul", "damage_reputation_sync");

	public void write(PacketByteBuf buf) {
		buf.writeVarInt(syncId);
		buf.writeVarInt(damageReputation.size());
		for (java.util.Map.Entry<String, Float> entry : damageReputation.entrySet()) {
			buf.writeString(entry.getKey());
			buf.writeFloat(entry.getValue());
		}
	}

	public static DamageReputationSyncPayload read(PacketByteBuf buf) {
		int syncId = buf.readVarInt();
		int size = buf.readVarInt();
		java.util.Map<String, Float> map = new java.util.HashMap<>();
		for (int i = 0; i < size; i++) {
			map.put(buf.readString(), buf.readFloat());
		}
		return new DamageReputationSyncPayload(syncId, map);
	}
}
