package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;

public class RepairAllRequestC2SPacket {
    private final int syncId;

    public RepairAllRequestC2SPacket(int syncId) {
        this.syncId = syncId;
    }

    public static void encode(RepairAllRequestC2SPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
    }

    public static RepairAllRequestC2SPacket decode(PacketByteBuf buf) {
        return new RepairAllRequestC2SPacket(buf.readInt());
    }

    public int getSyncId() { return syncId; }
}