package com.unnameduser.tradeoverhaul.client.gui;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.trade.TradeScreenSync;
import com.unnameduser.tradeoverhaul.screen.VillagerCraftingScreenHandler;
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
	public Text getDisplayName() { return name; }

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
		VillagerCraftingScreenHandler handler = new VillagerCraftingScreenHandler(syncId, inv, villager);
		handler.setVillager(villager);
		return handler;
	}

	// В классе VillagerTradeScreenHandlerFactory
	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		// ✓ Порядок ЗАПИСИ должен в точности совпадать с порядком ЧТЕНИЯ в клиентском конструкторе:

		// 1. Уровень жителя (varInt)
		buf.writeVarInt(villager.getVillagerData().getLevel());

		// 2. ID профессии (String)
		Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		buf.writeString(profId != null ? profId.toString() : "minecraft:none");

		// 3. Entity ID жителя (varInt)
		buf.writeVarInt(villager.getId());

		// ✓ БОЛЬШЕ НИЧЕГО НЕ ПИШЕМ СЮДА — расширенные данные синхронизируются отдельно
		// через пакеты ModNetworking.sendProfessionLevelSync() после открытия экрана
	}
}
