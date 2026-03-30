package com.unnameduser.tradeoverhaul.common.util;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/**
 * Утилиты для локализации и форматирования текста.
 */
public class LocalizationUtil {
	
	private LocalizationUtil() {}
	
	/**
	 * Форматирует количество монет в читаемый вид с учётом падежей.
	 * @param amount количество монет
	 * @param translationKey базовый ключ перевода (например, "currency.gold")
	 * @return отформатированная строка
	 */
	public static String formatCurrency(int amount, String translationKey) {
		if (amount <= 0) {
			return "0 " + getCurrencyName(translationKey, "gen");
		}
		
		String name = getCurrencyName(translationKey, getPluralForm(amount));
		return amount + " " + name;
	}
	
	/**
	 * Определяет форму множественного числа для русского языка.
	 */
	private static String getPluralForm(int amount) {
		int lastDigit = amount % 10;
		int lastTwoDigits = amount % 100;
		
		if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
			return "gen"; // множественное число (11-19)
		}
		
		if (lastDigit == 1) {
			return "nom"; // единственное число (1)
		}
		
		if (lastDigit >= 2 && lastDigit <= 4) {
			return "nom"; // множественное число (2-4)
		}
		
		return "gen"; // множественное число (5-9, 0)
	}
	
	/**
	 * Получает название валюты из перевода.
	 */
	private static String getCurrencyName(String baseKey, String caseType) {
		String key = baseKey + "." + caseType;
		// Возвращаем ключ для последующего перевода через Text.translatable
		return key;
	}
	
	/**
	 * Создаёт текстовый компонент для отображения денег.
	 */
	public static Text createMoneyText(int gold, int silver, int copper) {
		StringBuilder sb = new StringBuilder();
		
		if (gold > 0) {
			sb.append("§6").append(formatCurrency(gold, "currency.gold")).append(" ");
		}
		if (silver > 0) {
			sb.append("§f").append(formatCurrency(silver, "currency.silver")).append(" ");
		}
		if (copper > 0 || (gold == 0 && silver == 0)) {
			sb.append("§e").append(formatCurrency(copper, "currency.copper"));
		}
		
		return Text.literal(sb.toString().trim());
	}
}
