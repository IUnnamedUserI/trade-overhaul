package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import com.unnameduser.tradeoverhaul.TradeOverhaulMod;

public class TradeRequestC2SPacket {
    public final int syncId;
    public final int slotIndex;
    public final int amount;
    public final boolean buying;

    public TradeRequestC2SPacket(int syncId, int slotIndex, int amount, boolean buying) {
        this.syncId = syncId;
        this.slotIndex = slotIndex;
        this.amount = amount;
        this.buying = buying;
    }

    public static void encode(TradeRequestC2SPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
        buf.writeInt(packet.slotIndex);
        buf.writeInt(packet.amount);
        buf.writeBoolean(packet.buying);
    }

    public static TradeRequestC2SPacket decode(PacketByteBuf buf) {
        return new TradeRequestC2SPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }
}