package com.unnameduser.tradeoverhaul.client.gui;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.trade.TradeScreenSync;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VillagerTradeScreenHandlerFactory implements ExtendedScreenHandlerFactory {
	private final Text name;
	private final VillagerEntity villager;

	public VillagerTradeScreenHandlerFactory(Text name, VillagerEntity villager) {
		this.name = name;
		this.villager = villager;
	}

	@Override
	public Text getDisplayName() {
		return name;
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
		VillagerTradeScreenHandler handler = new VillagerTradeScreenHandler(syncId, inv, villager);
		// Для сервера также нужно установить villager (на случай если createMenu вызывается на сервере)
		handler.setVillager(villager);
		return handler;
	}

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		Identifier pid = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		// Пишем ID профессии для клиента
		buf.writeVarInt(Registries.VILLAGER_PROFESSION.getRawId(villager.getVillagerData().getProfession()));
		TradeScreenSync.write(buf, villager, TradeConfigLoader.getProfession(pid));
	}
}
