package com.unnameduser.tradeoverhaul.screen;

import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.component.VillagerProfessionComponent;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
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
        // ID жителя
        buf.writeInt(villager.getId());

        // Профессия
        buf.writeString(Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString());

        // Уровень
        buf.writeInt(villager.getVillagerData().getLevel());

        // ===== ОТПРАВЛЯЕМ ДАННЫЕ ОПЫТА =====
        if (villager instanceof VillagerTradeData data) {
            VillagerProfessionComponent profession = data.tradeOverhaul$getProfession();

            // Уровень из компонента (мод-уровень)
            buf.writeInt(profession.getLevel());
            // Опыт
            buf.writeInt(profession.getExperience());
            // Количество сделок
            buf.writeInt(profession.getTradesCompleted());
            // Дробный XP
            buf.writeFloat(profession.getFractionalXpAccumulator());

            // Трекер проданных предметов
            NbtCompound soldTracker = new NbtCompound();
            for (var entry : profession.soldItemsTracker.entrySet()) {
                soldTracker.putInt(entry.getKey(), entry.getValue());
            }
            buf.writeNbt(soldTracker);

            // Репутация урона
            NbtCompound damageRep = new NbtCompound();
            for (var entry : profession.damageReputation.entrySet()) {
                damageRep.putFloat(entry.getKey(), entry.getValue());
            }
            buf.writeNbt(damageRep);
        } else {
            // Заглушки, если данные недоступны
            buf.writeInt(1);
            buf.writeInt(0);
            buf.writeInt(0);
            buf.writeFloat(0f);
            buf.writeNbt(new NbtCompound());
            buf.writeNbt(new NbtCompound());
        }

        // Инвентарь жителя
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