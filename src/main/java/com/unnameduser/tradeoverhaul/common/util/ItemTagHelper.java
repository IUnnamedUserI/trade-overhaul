package com.unnameduser.tradeoverhaul.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Утилиты для работы с тегами предметов
 */
public class ItemTagHelper {
	
	private static final String PLAYER_SOLD_TAG = "PlayerSold";
	
	/**
	 * Помечает предмет как проданный игроком
	 */
	public static void markAsPlayerSold(ItemStack stack) {
		if (stack.isEmpty()) return;
		
		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.putBoolean(PLAYER_SOLD_TAG, true);
		stack.setNbt(nbt);
	}
	
	/**
	 * Проверяет, был ли предмет продан игроком
	 */
	public static boolean isPlayerSold(ItemStack stack) {
		if (stack.isEmpty()) return false;
		
		NbtCompound nbt = stack.getNbt();
		if (nbt == null) return false;
		
		return nbt.getBoolean(PLAYER_SOLD_TAG);
	}
	
	/**
	 * Снимает метку "продано игроком"
	 */
	public static void clearPlayerSoldTag(ItemStack stack) {
		if (stack.isEmpty()) return;
		
		NbtCompound nbt = stack.getNbt();
		if (nbt != null && nbt.contains(PLAYER_SOLD_TAG)) {
			nbt.remove(PLAYER_SOLD_TAG);
			stack.setNbt(nbt);
		}
	}
}
