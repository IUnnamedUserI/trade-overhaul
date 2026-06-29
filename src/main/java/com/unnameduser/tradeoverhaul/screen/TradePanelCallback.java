package com.unnameduser.tradeoverhaul.screen;

import net.minecraft.item.ItemStack;

public interface TradePanelCallback {

    // Вызывается, когда игрок подтверждает покупку/продажу
    // @param slotIndex - индекс слота
    // @param amount - количество
    // @buying - флаг покупки (true) или продажи (false)
    void onTradeConfirm(int slotIndex, int amount, boolean buying);

    // Вызывается при отмене продажи
    void onTradeCancel();

    // Вызывается при изменении количества предметов
    // @param newAmount - новое количество
    // @param totalPrice - итоговая цена
    void onAmountChanged(int newAmount, int totalPrice);

    int getVillagerMoney();
}
