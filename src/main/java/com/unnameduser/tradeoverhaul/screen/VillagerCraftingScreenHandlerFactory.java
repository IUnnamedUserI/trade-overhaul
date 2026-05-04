package com.unnameduser.tradeoverhaul.screen;

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
import org.jetbrains.annotations.Nullable;

public class VillagerCraftingScreenHandlerFactory implements ExtendedScreenHandlerFactory {

    private final Text displayName;
    private final VillagerEntity villager;

    public VillagerCraftingScreenHandlerFactory(Text displayName, VillagerEntity villager) {
        this.displayName = displayName;
        this.villager = villager;
    }

    @Override
    public Text getDisplayName() {
        return displayName;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        // Используем конструктор с VillagerEntity для сервера
        return new VillagerCraftingScreenHandler(syncId, inv, villager);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        // Передаём ID жителя
        buf.writeInt(villager.getId());

        // Передаём ID профессии жителя
        Identifier professionId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
        buf.writeString(professionId.toString());

        // Передаём уровень жителя
        buf.writeInt(villager.getVillagerData().getLevel());
    }
}