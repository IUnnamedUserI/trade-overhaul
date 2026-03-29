package com.unnameduser.tradeoverhaul.common.numismatic;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

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
	
	/**
	 * Форматирует сумму в читаемый вид.
	 * @param amount количество монет в медном эквиваленте
	 * @return строка вида "1 золотая, 5 серебряных, 3 медных"
	 */
	public static String formatMoney(int amount) {
		if (amount <= 0) {
			return "0 монет";
		}
		
		StringBuilder sb = new StringBuilder();
		
		// Numismatic Overhaul: 1 золотая = 100 серебряных = 10000 медных
		// 1 серебряная = 100 медных
		int gold = amount / 10000;
		int remaining = amount % 10000;
		int silver = remaining / 100;
		int copper = remaining % 100;
		
		if (gold > 0) {
			sb.append(gold).append(" золот");
			if (gold % 10 == 1 && gold % 100 != 11) {
				sb.append("ая");
			} else if (gold % 10 >= 2 && gold % 10 <= 4 && (gold % 100 < 10 || gold % 100 >= 20)) {
				sb.append("ые");
			} else {
				sb.append("ых");
			}
		}
		
		if (silver > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(silver).append(" серебрян");
			if (silver % 10 == 1 && silver % 100 != 11) {
				sb.append("ая");
			} else if (silver % 10 >= 2 && silver % 10 <= 4 && (silver % 100 < 10 || silver % 100 >= 20)) {
				sb.append("ые");
			} else {
				sb.append("ых");
			}
		}
		
		if (copper > 0) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(copper).append(" медн");
			if (copper % 10 == 1 && copper % 100 != 11) {
				sb.append("ая");
			} else if (copper % 10 >= 2 && copper % 10 <= 4 && (copper % 100 < 10 || copper % 100 >= 20)) {
				sb.append("ые");
			} else {
				sb.append("ых");
			}
		}
		
		return sb.toString();
	}
}
