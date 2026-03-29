package com.unnameduser.tradeoverhaul.common.component;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

/**
 * Компонент для хранения данных о скидке от Bulletin Board.
 */
public class BulletinBoardComponent {
    private long discountEndTime = 0;
    private int discountPercent = 0;
    
    public boolean hasActiveDiscount() {
        return discountEndTime > System.currentTimeMillis();
    }
    
    public void setDiscount(long endTime, int percent) {
        this.discountEndTime = endTime;
        this.discountPercent = percent;
    }
    
    public void clearDiscount() {
        this.discountEndTime = 0;
        this.discountPercent = 0;
    }
    
    public long getEndTime() {
        return discountEndTime;
    }
    
    public int getPercent() {
        return discountPercent;
    }
    
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("BulletinBoardDiscountEndTime")) {
            this.discountEndTime = nbt.getLong("BulletinBoardDiscountEndTime");
        }
        if (nbt.contains("BulletinBoardDiscountPercent")) {
            this.discountPercent = nbt.getInt("BulletinBoardDiscountPercent");
        }
    }
    
    public void writeNbt(NbtCompound nbt) {
        if (discountEndTime > 0) {
            nbt.putLong("BulletinBoardDiscountEndTime", discountEndTime);
            nbt.putInt("BulletinBoardDiscountPercent", discountPercent);
        }
    }
}
