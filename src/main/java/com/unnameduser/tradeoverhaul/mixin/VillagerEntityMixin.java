package com.unnameduser.tradeoverhaul.mixin;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandlerFactory;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.component.BulletinBoardComponent;
import com.unnameduser.tradeoverhaul.common.component.VillagerCurrencyComponent;
import com.unnameduser.tradeoverhaul.common.component.VillagerInventoryComponent;
import com.unnameduser.tradeoverhaul.common.component.VillagerProfessionComponent;
import com.unnameduser.tradeoverhaul.common.trade.TradeRestock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin implements VillagerTradeData {

	@Unique
	private VillagerInventoryComponent tradeOverhaul_inventory;

	@Unique
	private int tradeOverhaul_walletEmeralds;

	@Unique
	private VillagerCurrencyComponent tradeOverhaul_currency;
	
	@Unique
	private VillagerProfessionComponent tradeOverhaul_profession;

	@Unique
	private int[] tradeOverhaul_offerSlots;

	@Unique
	private long tradeOverhaul_emptySinceTick = -1L;

	@Unique
	@Nullable
	private PlayerEntity tradeOverhaul_activeTrader;

	@Unique
	private BulletinBoardComponent tradeOverhaul_bulletinBoardDiscount;

	@Unique
	private net.minecraft.village.VillagerProfession tradeOverhaul_lastProfession;

	@Override
	public int tradeOverhaul$getWalletEmeralds() {
		return tradeOverhaul_walletEmeralds;
	}

	@Override
	public void tradeOverhaul$setWalletEmeralds(int amount) {
		tradeOverhaul_walletEmeralds = Math.max(0, amount);
	}

	@Override
	public VillagerCurrencyComponent tradeOverhaul$getCurrency() {
		if (tradeOverhaul_currency == null) {
			tradeOverhaul_currency = new VillagerCurrencyComponent();
		}
		return tradeOverhaul_currency;
	}
	
	@Override
	public VillagerProfessionComponent tradeOverhaul$getProfession() {
		if (tradeOverhaul_profession == null) {
			tradeOverhaul_profession = new VillagerProfessionComponent();
			// Синхронизируем с ванильным уровнем при первой инициализации
			VillagerEntity villager = (VillagerEntity) (Object) this;
			tradeOverhaul_profession.setLevel(villager.getVillagerData().getLevel());
		}
		return tradeOverhaul_profession;
	}

	@Override
	public int[] tradeOverhaul$getOfferSlots() {
		return tradeOverhaul_offerSlots;
	}

	@Override
	public void tradeOverhaul$setOfferSlots(int[] slots) {
		tradeOverhaul_offerSlots = slots;
	}

	@Override
	public long tradeOverhaul$getEmptySinceTick() {
		return tradeOverhaul_emptySinceTick;
	}

	@Override
	public void tradeOverhaul$setEmptySinceTick(long worldTick) {
		tradeOverhaul_emptySinceTick = worldTick;
	}

	@Override
	public VillagerInventoryComponent tradeOverhaul$getInventory() {
		if (tradeOverhaul_inventory == null) {
			tradeOverhaul_inventory = new VillagerInventoryComponent();
		}
		return tradeOverhaul_inventory;
	}

	@Override
	@Nullable
	public PlayerEntity tradeOverhaul$getActiveTrader() {
		return tradeOverhaul_activeTrader;
	}

	@Override
	public void tradeOverhaul$setActiveTrader(@Nullable PlayerEntity player) {
		tradeOverhaul_activeTrader = player;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void tradeOverhaul$onTick(CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;

		// Отслеживаем смену профессии: если житель получил профессию после NONE — сбрасываем компонент и делаем ресток
		net.minecraft.village.VillagerProfession currentProf = villager.getVillagerData().getProfession();
		if (tradeOverhaul_lastProfession == null) {
			tradeOverhaul_lastProfession = currentProf; // Первая инициализация
		}
		if (tradeOverhaul_lastProfession == net.minecraft.village.VillagerProfession.NONE
				&& currentProf != net.minecraft.village.VillagerProfession.NONE
				&& currentProf != net.minecraft.village.VillagerProfession.NITWIT) {
			// Житель получил новую профессию — сбрасываем компонент
			tradeOverhaul$getProfession().resetProfession();

			// Сразу делаем ресток, чтобы у жителя были товары на продажу
			Identifier profId = Registries.VILLAGER_PROFESSION.getId(currentProf);
			com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile file =
				com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader.getProfession(profId);
			if (file != null) {
				TradeOverhaulMod.LOGGER.info("Villager {} got new profession {} — performing initial restock",
					villager.getUuid(), profId);
				TradeRestock.forceRestock(villager, file,
					com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader.getSettings());
			}
		}
		tradeOverhaul_lastProfession = currentProf;

		// Синхронизируем уровень профессии с ванильным уровнем жителя ПЕРЕД рестокoм
		// ВАЖНО: используем МАКСИМУМ из ванильного и мод-уровней, чтобы не терять прогресс
		VillagerProfessionComponent profession = this.tradeOverhaul$getProfession();
		int vanillaLevel = villager.getVillagerData().getLevel();
		int modLevel = profession.getLevel();

		// Если ванильный уровень выше (например, после загрузки мира), используем его
		// Если мод-уровень выше (например, после торговли), НЕ сбрасываем его
		if (vanillaLevel > modLevel) {
			profession.setLevel(vanillaLevel);
		}
		// Если мод-уровень выше ванильного, обновляем ванильный
		else if (modLevel > vanillaLevel && modLevel <= 5) {
			villager.setVillagerData(villager.getVillagerData().withLevel(modLevel));
		}

		// Ресток теперь управляется глобальным таймером (GlobalRestockTimer)

		// Проверяем, активен ли трейдер (не закрыл ли он GUI)
		if (tradeOverhaul_activeTrader != null) {
			// Проверяем, открыт ли у трейдера экран торговли с этим жителем
			boolean stillTrading = false;
			if (tradeOverhaul_activeTrader.isAlive() && tradeOverhaul_activeTrader.currentScreenHandler != null) {
				if (tradeOverhaul_activeTrader.currentScreenHandler instanceof com.unnameduser.tradeoverhaul.client.gui.VillagerTradeScreenHandler tradeHandler) {
					// Проверяем, что это тот же житель
					if (tradeHandler.getVillager() == villager) {
						stillTrading = true;
					}
				}
			}
			
			if (!stillTrading) {
				// Игрок закрыл GUI или умер - очищаем трейдера
				tradeOverhaul_activeTrader = null;
			} else {
				// Make villager look at active trader
				villager.getLookControl().lookAt(tradeOverhaul_activeTrader);
				// Stop villager movement while trading
				villager.getNavigation().stop();
				villager.setVelocity(0, villager.getVelocity().y, 0);
			}
		}
	}

	@Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
	private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		VillagerEntity villager = (VillagerEntity) (Object) this;

		var profession = villager.getVillagerData().getProfession();
		if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE
				&& profession != net.minecraft.village.VillagerProfession.NITWIT) {

			// Check if another player is already trading
			if (tradeOverhaul_activeTrader != null && tradeOverhaul_activeTrader != player) {
				cir.setReturnValue(ActionResult.FAIL);
				return;
			}

			if (!villager.getWorld().isClient) {
				tradeOverhaul_activeTrader = player;
				// Инициализируем компонент валюты (создаётся при первом вызове getCurrency)
				var profId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
				com.unnameduser.tradeoverhaul.TradeOverhaulMod.LOGGER.info("Opening trade screen for villager {} (profession: {}, level: {})", 
					villager.getUuid(), profId, villager.getVillagerData().getLevel());
				player.openHandledScreen(new VillagerTradeScreenHandlerFactory(villager.getDisplayName(), villager));
			}

			cir.setReturnValue(ActionResult.success(villager.getWorld().isClient));
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		nbt.putInt("TradeOverhaulWallet", tradeOverhaul_walletEmeralds);
		if (tradeOverhaul_offerSlots != null) {
			nbt.putIntArray("TradeOverhaulOfferSlots", tradeOverhaul_offerSlots);
		}
		nbt.putLong("TradeOverhaulEmptySince", tradeOverhaul_emptySinceTick);
		if (tradeOverhaul_inventory != null) {
			NbtCompound inventoryNbt = new NbtCompound();
			tradeOverhaul_inventory.writeNbt(inventoryNbt);
			nbt.put("TradeOverhaulInventory", inventoryNbt);
		}
		// Сохраняем валюту жителя
		if (tradeOverhaul_currency != null) {
			NbtCompound currencyNbt = new NbtCompound();
			tradeOverhaul_currency.writeNbt(currencyNbt);
			nbt.put("TradeOverhaulCurrency", currencyNbt);
		}
		// Сохраняем мастерство жителя
		if (tradeOverhaul_profession != null) {
			NbtCompound professionNbt = new NbtCompound();
			tradeOverhaul_profession.writeNbt(professionNbt);
			nbt.put("TradeOverhaulProfession", professionNbt);
		}
		// Don't save active trader - it's session-only
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("TradeOverhaulWallet")) {
			tradeOverhaul_walletEmeralds = Math.max(0, nbt.getInt("TradeOverhaulWallet"));
		}
		if (nbt.contains("TradeOverhaulOfferSlots")) {
			int[] fromNbt = nbt.getIntArray("TradeOverhaulOfferSlots");
			tradeOverhaul_offerSlots = Arrays.copyOf(fromNbt, fromNbt.length);
		}
		if (nbt.contains("TradeOverhaulEmptySince")) {
			tradeOverhaul_emptySinceTick = nbt.getLong("TradeOverhaulEmptySince");
		}
		if (nbt.contains("TradeOverhaulInventory")) {
			tradeOverhaul$getInventory().readNbt(nbt.getCompound("TradeOverhaulInventory"));
		}
		if (nbt.contains("TradeOverhaulCurrency")) {
			tradeOverhaul$getCurrency().readNbt(nbt.getCompound("TradeOverhaulCurrency"));
		}
		if (nbt.contains("TradeOverhaulProfession")) {
			tradeOverhaul$getProfession().readNbt(nbt.getCompound("TradeOverhaulProfession"));
		}

		// Синхронизируем уровень профессии с ванильным уровнем при загрузке
		// Это нужно для корректной работы до первого tick()
		if (tradeOverhaul_profession != null) {
			int vanillaLevel = ((VillagerEntity)(Object)this).getVillagerData().getLevel();
			tradeOverhaul_profession.setLevel(vanillaLevel);
		}
		
		// Active trader is always null after load
		tradeOverhaul_activeTrader = null;
	}
}
