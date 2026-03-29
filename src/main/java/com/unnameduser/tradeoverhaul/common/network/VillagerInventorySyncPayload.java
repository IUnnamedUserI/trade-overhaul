package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** S2C синхронизация инвентаря жителя */
public record VillagerInventorySyncPayload(int syncId, ItemStack[] inventory) {
	public static final Identifier ID = new Identifier(TradeOverhaulMod.MOD_ID, "villager_inventory_sync");

	public void write(PacketByteBuf buf) {
		buf.writeVarInt(syncId);
		buf.writeVarInt(inventory.length);
		for (ItemStack stack : inventory) {
			buf.writeItemStack(stack);
		}
	}

	public static VillagerInventorySyncPayload read(PacketByteBuf buf) {
		int syncId = buf.readVarInt();
		int size = buf.readVarInt();
		ItemStack[] inventory = new ItemStack[size];
		for (int i = 0; i < size; i++) {
			inventory[i] = buf.readItemStack();
		}
		return new VillagerInventorySyncPayload(syncId, inventory);
	}
}
