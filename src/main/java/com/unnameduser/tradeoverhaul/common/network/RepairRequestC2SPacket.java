package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;

public class RepairRequestC2SPacket {
    private final int syncId;
    private final int slotIndex;

    public RepairRequestC2SPacket(int syncId, int slotIndex) {
        this.syncId = syncId;
        this.slotIndex = slotIndex;
    }

    public static void encode(RepairRequestC2SPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
        buf.writeInt(packet.slotIndex);
    }

    public static RepairRequestC2SPacket decode(PacketByteBuf buf) {
        return new RepairRequestC2SPacket(buf.readInt(), buf.readInt());
    }

    public int getSyncId() { return syncId; }
    public int getSlotIndex() { return slotIndex; }
}