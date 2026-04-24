package com.unnameduser.tradeoverhaul.mixin;

import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandlerFactory;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "beginTradeWith(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    private void tradeOverhaul$onBeginTradeWith(PlayerEntity player, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerProfession profession = villager.getVillagerData().getProfession();

        if (profession != null && profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT) {
            if (!player.getWorld().isClient) {
                // 1. Устанавливаем активного трейдера (интерфейс уже реализован в VillagerEntityMixin)
                ((VillagerTradeData) this).tradeOverhaul$setActiveTrader(player);

                // 2. Немедленно блокируем ИИ жителя и заставляем смотреть на игрока
                villager.getNavigation().stop();
                villager.getLookControl().lookAt(player);
                villager.setVelocity(0, villager.getVelocity().y, 0);

                // 3. Открываем наше GUI
                player.openHandledScreen(new VillagerTradeScreenHandlerFactory(villager.getDisplayName(), villager));

                // 4. Отменяем ванильное/МКА торговое меню
                ci.cancel();
            }
        }
    }
}