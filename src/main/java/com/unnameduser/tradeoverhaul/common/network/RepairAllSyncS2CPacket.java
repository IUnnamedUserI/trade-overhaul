package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;

public class RepairAllSyncS2CPacket {
    private final int syncId;

    public RepairAllSyncS2CPacket(int syncId) {
        this.syncId = syncId;
    }

    public static void encode(RepairAllSyncS2CPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
    }

    public static RepairAllSyncS2CPacket decode(PacketByteBuf buf) {
        return new RepairAllSyncS2CPacket(buf.readInt());
    }

    public int getSyncId() { return syncId; }
}