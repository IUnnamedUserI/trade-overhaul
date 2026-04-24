package com.unnameduser.tradeoverhaul.common.numismatic;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;

/**
 * Утилиты для работы с валютой Numismatic Overhaul.
 * 
 * Система монет:
 * - Бронзовая монета (bronze_coin) = 1 единица
 * - Серебряная монета (silver_coin) = 10 бронзовых
 * - Золотая монета (gold_coin) = 100 бронзовых (10 серебряных)
 */
public class NumismaticHelper {
	
	private NumismaticHelper() {}
	
	/**
	 * Инициализирует компонент валюты у сущности если он ещё не создан.
	 * @param entity сущность
	 */
	public static void ensureCurrencyComponent(@Nullable Entity entity) {
		if (entity == null || !(entity instanceof LivingEntity living)) {
			return;
		}
		try {
			// Пытаемся получить компонент - это создаст его если он ещё не существует
			ModComponents.CURRENCY.get(living);
		} catch (Exception e) {
			// Компонент не удалось создать
		}
	}
	
	/**
	 * Проверяет, есть ли у сущности достаточно монет.
	 * @param entity сущность (игрок или житель)
	 * @param amount количество монет в бронзовом эквиваленте
	 * @return true если достаточно монет
	 */
	public static boolean hasEnoughMoney(@Nullable Entity entity, int amount) {
		if (entity == null || !(entity instanceof LivingEntity living)) {
			return false;
		}

		try {
			CurrencyComponent component = ModComponents.CURRENCY.get(living);
			if (component == null) {
				return amount <= 0;
			}
			return component.getValue() >= amount;
		} catch (Exception e) {
			// Компонент валюты не найден
			return amount <= 0;
		}
	}

	/**
	 * Снимает монеты у сущности.
	 * @param entity сущность (игрок или житель)
	 * @param amount количество монет в бронзовом эквиваленте
	 * @return true если успешно снято
	 */
	public static boolean removeMoney(@Nullable Entity entity, int amount) {
		if (entity == null || !(entity instanceof LivingEntity living)) {
			return false;
		}

		try {
			CurrencyComponent component = ModComponents.CURRENCY.get(living);
			if (component == null) {
				return false;
			}

			if (component.getValue() < amount) {
				return false;
			}

			component.silentModify(-amount);
			return true;
		} catch (Exception e) {
			// Компонент валюты не найден
			return false;
		}
	}

	/**
	 * Добавляет монеты сущности.
	 * @param entity сущность (игрок или житель)
	 * @param amount количество монет в бронзовом эквиваленте
	 */
	public static void addMoney(@Nullable Entity entity, int amount) {
		if (entity == null || !(entity instanceof LivingEntity living)) {
			return;
		}

		try {
			CurrencyComponent component = ModComponents.CURRENCY.get(living);
			if (component == null) {
				return;
			}

			component.silentModify(amount);
		} catch (Exception e) {
			// Компонент валюты не найден
		}
	}
	
	/**
	 * Получает общее количество монет у сущности в бронзовом эквиваленте.
	 * @param entity сущность
	 * @return количество монет в бронзовом эквиваленте
	 */
	public static int getTotalMoney(@Nullable Entity entity) {
		if (entity == null || !(entity instanceof LivingEntity living)) {
			return 0;
		}

		try {
			CurrencyComponent component = ModComponents.CURRENCY.get(living);
			if (component == null) {
				return 0;
			}
			return (int) component.getValue();
		} catch (Exception e) {
			// Компонент валюты не найден (например, у жителей при первом создании)
			return 0;
		}
	}

	public static String formatMoney(int totalCopper) {
		int gold = totalCopper / 10000;
		int silver = (totalCopper % 10000) / 100;
		int copper = totalCopper % 100;

		StringBuilder sb = new StringBuilder();
		if (gold > 0) {
			sb.append(gold).append(" ").append(Text.translatable("currency.gold").getString()).append(" ");
		}
		if (silver > 0) {
			sb.append(silver).append(" ").append(Text.translatable("currency.silver").getString()).append(" ");
		}
		if (copper > 0 || (gold == 0 && silver == 0)) {
			sb.append(copper).append(" ").append(Text.translatable("currency.copper").getString());
		}
		return sb.toString().trim();
	}
}
