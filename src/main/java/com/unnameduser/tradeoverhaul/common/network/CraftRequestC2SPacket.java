package com.unnameduser.tradeoverhaul.common.network;

import net.minecraft.network.PacketByteBuf;

public class CraftRequestC2SPacket {
    private final int syncId;
    private final String recipeId;
    private final int selectedSlot;  // -1 если нет уникального предмета

    public CraftRequestC2SPacket(int syncId, String recipeId, int selectedSlot) {
        this.syncId = syncId;
        this.recipeId = recipeId;
        this.selectedSlot = selectedSlot;
    }

    public static void encode(CraftRequestC2SPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.syncId);
        buf.writeString(packet.recipeId);
        buf.writeInt(packet.selectedSlot);
    }

    public static CraftRequestC2SPacket decode(PacketByteBuf buf) {
        return new CraftRequestC2SPacket(
                buf.readInt(),
                buf.readString(),
                buf.readInt()
        );
    }

    public int getSyncId() { return syncId; }
    public String getRecipeId() { return recipeId; }
    public int getSelectedSlot() { return selectedSlot; }
}