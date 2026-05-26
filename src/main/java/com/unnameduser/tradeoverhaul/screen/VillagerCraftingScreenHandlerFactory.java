package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
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

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new VillagerCraftingScreenHandler(syncId, inv, villager);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeInt(villager.getId());
        buf.writeString(Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString());
        buf.writeInt(villager.getVillagerData().getLevel());

        // Отправляем инвентарь жителя
        if (villager instanceof VillagerTradeData data) {
            VillagerInventoryComponent inventory = data.tradeOverhaul$getInventory();
            buf.writeInt(inventory.size());
            for (int i = 0; i < inventory.size(); i++) {
                buf.writeItemStack(inventory.getStack(i));
            }
        } else {
            buf.writeInt(0);
        }
    }
}