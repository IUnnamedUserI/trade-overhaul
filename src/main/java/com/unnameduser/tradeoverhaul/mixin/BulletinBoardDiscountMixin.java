package com.unnameduser.tradeoverhaul.mixin;

import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.unnameduser.tradeoverhaul.common.trade.TradePricing;
import net.minecraft.item.ItemStack;

/**
 * Mixin для применения скидок от Bulletin Board Mod.
 * Проверяет NBT жителя на наличие активной скидки и применяет её.
 */
@Mixin(TradePricing.class)
public class BulletinBoardDiscountMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("TradeOverhaul|BulletinBoard");
    
    static {
        LOGGER.warn("BulletinBoardDiscountMixin class loaded!");
    }
    
    /**
     * Применяет скидку к цене покупки если у жителя есть активная скидка от Bulletin Board.
     * Bulletin Board сохраняет:
     * - BulletinBoardDiscountEndTime (long) - время окончания скидки в мс
     * - BulletinBoardDiscountPercent (int) - процент скидки (50 = 50%)
     */
    @Inject(
            method = "buyPriceForStack",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void applyBulletinBoardDiscount(
            ItemStack stack, 
            VillagerEntity villager,
            ProfessionTradeFile profession,
            CallbackInfoReturnable<Integer> cir
    ) {
        LOGGER.warn("buyPriceForStack mixin called! villager={}, stack={}", 
            villager != null ? villager.getName().getString() : "null", 
            stack != null ? stack.getTranslationKey() : "null");
            
        if (villager == null || stack.isEmpty()) {
            LOGGER.warn("Early return: villager={}, stack empty={}", 
                villager != null, stack != null && stack.isEmpty());
            return;
        }
        
        // Проверяем наличие скидки от Bulletin Board
        NbtCompound nbt = villager.writeNbt(new NbtCompound());
        
        LOGGER.warn("NBT contains BulletinBoardDiscountEndTime: {}", nbt.contains("BulletinBoardDiscountEndTime"));
        LOGGER.warn("NBT contains BulletinBoardDiscountPercent: {}", nbt.contains("BulletinBoardDiscountPercent"));
        
        if (nbt.contains("BulletinBoardDiscountEndTime") && nbt.contains("BulletinBoardDiscountPercent")) {
            long endTime = nbt.getLong("BulletinBoardDiscountEndTime");
            int discountPercent = nbt.getInt("BulletinBoardDiscountPercent");
            long currentTime = System.currentTimeMillis();
            
            LOGGER.warn("Discount found: {}%, ends at {}, current time: {}", discountPercent, endTime, currentTime);
            
            if (currentTime < endTime) {
                // Скидка активна - применяем
                int originalPrice = cir.getReturnValue();
                int discountedPrice = Math.max(1, originalPrice * (100 - discountPercent) / 100);
                LOGGER.warn("Applied {}% discount: {} -> {}", discountPercent, originalPrice, discountedPrice);
                cir.setReturnValue(discountedPrice);
            } else {
                LOGGER.warn("Discount expired ({} < {})", currentTime, endTime);
            }
        } else {
            LOGGER.warn("No discount NBT found in villager");
        }
    }
    
    /**
     * Применяет скидку к цене продажи если у жителя есть активная скидка от Bulletin Board.
     * При продаже игрок получает БОЛЬШЕ изумрудов (скидка в его пользу).
     */
    @Inject(
            method = "sellPriceForStack",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void applyBulletinBoardDiscountSell(
            ItemStack stack,
            VillagerEntity villager,
            ProfessionTradeFile profession,
            CallbackInfoReturnable<Integer> cir
    ) {
        LOGGER.warn("sellPriceForStack mixin called! villager={}, stack={}", 
            villager != null ? villager.getName().getString() : "null",
            stack != null ? stack.getTranslationKey() : "null");
            
        if (villager == null || stack.isEmpty()) {
            LOGGER.warn("Early return: villager={}, stack empty={}", 
                villager != null, stack != null && stack.isEmpty());
            return;
        }
        
        NbtCompound nbt = villager.writeNbt(new NbtCompound());
        
        LOGGER.warn("NBT contains BulletinBoardDiscountEndTime: {}", nbt.contains("BulletinBoardDiscountEndTime"));
        LOGGER.warn("NBT contains BulletinBoardDiscountPercent: {}", nbt.contains("BulletinBoardDiscountPercent"));
        
        if (nbt.contains("BulletinBoardDiscountEndTime") && nbt.contains("BulletinBoardDiscountPercent")) {
            long endTime = nbt.getLong("BulletinBoardDiscountEndTime");
            int discountPercent = nbt.getInt("BulletinBoardDiscountPercent");
            long currentTime = System.currentTimeMillis();
            
            LOGGER.warn("Discount found: {}%, ends at {}, current time: {}", discountPercent, endTime, currentTime);
            
            if (currentTime < endTime) {
                // Скидка активна - игрок получает больше изумрудов
                int originalPrice = cir.getReturnValue();
                int increasedPrice = Math.max(1, originalPrice * (100 + discountPercent) / 100);
                LOGGER.warn("Applied {}% sell bonus: {} -> {}", discountPercent, originalPrice, increasedPrice);
                cir.setReturnValue(increasedPrice);
            } else {
                LOGGER.warn("Discount expired ({} < {})", currentTime, endTime);
            }
        } else {
            LOGGER.warn("No discount NBT found in villager");
        }
    }
}
