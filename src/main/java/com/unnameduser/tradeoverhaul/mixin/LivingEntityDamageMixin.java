package com.unnameduser.tradeoverhaul.mixin;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerProfessionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для отслеживания урона, нанесённого жителю игроком.
 * Инжектимся в LivingEntity.damage, так как VillagerEntity наследует от LivingEntity.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin extends Entity {

	public LivingEntityDamageMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "damage", at = @At("TAIL"))
	private void tradeOverhaul$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		TradeOverhaulMod.LOGGER.debug("LivingEntityDamageMixin.damage called! entity={}, source={}, amount={}, isClient={}",
			((Entity)(Object)this).getType(), source.getName(), amount, getWorld().isClient);

		// Работаем только на сервере и только с villagers
		if (getWorld().isClient) return;
		if (!((Entity) (Object) this instanceof VillagerEntity villager)) return;

		TradeOverhaulMod.LOGGER.debug("Villager was damaged! Attacker={}", source.getAttacker());

		// Проверяем, что урон нанёс игрок
		if (source.getAttacker() instanceof PlayerEntity player) {
			VillagerTradeData data = (VillagerTradeData) villager;
			VillagerProfessionComponent prof = data.tradeOverhaul$getProfession();
			prof.addDamageReputation(player.getUuidAsString(), amount);
			TradeOverhaulMod.LOGGER.debug("Player {} dealt {} damage to villager {}, total: {}",
				player.getUuidAsString(), amount, villager.getUuid(),
				prof.getDamageReputation(player.getUuidAsString()));

			// Синхронизируем репутацию с клиентом (если игрок смотрит экран торговли)
			if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer
					&& serverPlayer.currentScreenHandler instanceof com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler tradeHandler) {
				com.unnameduser.tradeoverhaul.common.network.ModNetworking.sendDamageReputationSync(
					serverPlayer, tradeHandler.syncId, new java.util.HashMap<>(prof.damageReputation));
			}
		}
	}
}
