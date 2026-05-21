package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class RepairSyncS2CPacket {
    private final int syncId;
    private final int repairedSlot;

    public RepairSyncS2CPacket(int syncId, int repairedSlot) {
        this.syncId = syncId;
        this.repairedSlot = repairedSlot;
    }

    public static void encode(RepairSyncS2CPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
        buf.writeInt(packet.repairedSlot);
    }

    public static RepairSyncS2CPacket decode(PacketByteBuf buf) {
        return new RepairSyncS2CPacket(buf.readInt(), buf.readInt());
    }

    public int getSyncId() { return syncId; }
    public int getRepairedSlot() { return repairedSlot; }
}