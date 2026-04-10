package com.unnameduser.tradeoverhaul.mixin;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.VillagerProfessionComponent;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для отслеживания потери рабочего блока жителем.
 * Если житель потерял рабочий блок (POI был удалён), но с ним уже торговали —
 * он сохраняет профессию, но при каждом рестокe теряет опыт.
 */
@Mixin(VillagerEntity.class)
public class VillagerWorkstationMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void tradeOverhaul$checkWorkstation(CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;
		
		// Проверяем только на серверной стороне
		if (villager.getWorld().isClient) return;
		
		VillagerProfession profession = villager.getVillagerData().getProfession();
		if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return;
		
		VillagerTradeData data = (VillagerTradeData) villager;
		VillagerProfessionComponent prof = data.tradeOverhaul$getProfession();
		
		// Проверяем, есть ли у жителя рабочий блок (JOB_SITE в памяти мозга)
		Brain<?> brain = villager.getBrain();
		java.util.Optional<GlobalPos> jobSiteOpt = brain.getOptionalMemory(MemoryModuleType.JOB_SITE);
		boolean hasJobSite = jobSiteOpt.isPresent();
		
		if (!hasJobSite) {
			// Рабочего блока нет — устанавливаем флаг потери
			if (!prof.hasWorkstationLost()) {
				TradeOverhaulMod.LOGGER.info("Villager {} lost workstation (profession={}, hasEverTraded={})", 
					villager.getUuid(), 
					Registries.VILLAGER_PROFESSION.getId(profession),
					prof.hasEverTraded());
			}
			prof.setWorkstationLost(true);
		} else {
			// Рабочий блок на месте — сбрасываем флаг потери
			if (prof.hasWorkstationLost()) {
				TradeOverhaulMod.LOGGER.info("Villager {} regained workstation", villager.getUuid());
			}
			prof.setWorkstationLost(false);
		}
	}
}
