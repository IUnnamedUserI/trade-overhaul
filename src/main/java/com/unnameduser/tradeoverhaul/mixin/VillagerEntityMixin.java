package com.unnameduser.tradeoverhaul.mixin;

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
		
		// Синхронизируем уровень профессии с ванильным уровнем жителя ПЕРЕД рестокoм
		// Это критически важно для правильной фильтрации товаров по уровням
		VillagerProfessionComponent profession = this.tradeOverhaul$getProfession();
		int vanillaLevel = villager.getVillagerData().getLevel();
		int modLevel = profession.getLevel();
		if (vanillaLevel != modLevel) {
			// Используем ванильный уровень как приоритетный
			profession.setLevel(vanillaLevel);
		}
		
		// Теперь вызываем ресток с правильным уровнем
		TradeRestock.tick(villager);

		// Check if active trader is too far or disconnected
		if (tradeOverhaul_activeTrader != null) {
			if (!tradeOverhaul_activeTrader.isAlive() ||
				villager.squaredDistanceTo(tradeOverhaul_activeTrader) > 100.0) { // 10 blocks squared
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
