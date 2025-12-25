package com.example.soukify.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class for managing currency preferences and conversions
 */
public class CurrencyHelper {
    
    private static final String PREFS_NAME = "com.example.soukify.currency_prefs";
    private static final String CURRENCY_KEY = "selected_currency";
    private static final String DEFAULT_CURRENCY = "MAD";
    
    // Static exchange rates (simplified for demonstration)
    // Base is MAD
    private static final Map<String, Double> EXCHANGE_RATES = new HashMap<>();
    
    static {
        EXCHANGE_RATES.put("MAD", 1.0);
        EXCHANGE_RATES.put("USD", 0.10);  // 1 MAD = 0.10 USD
        EXCHANGE_RATES.put("EUR", 0.092); // 1 MAD = 0.092 EUR
    }

    /**
     * Get the saved currency preference
     */
    public static String getCurrency(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CURRENCY_KEY, DEFAULT_CURRENCY);
    }
    
    /**
     * Save currency preference locally
     */
    public static void setCurrency(Context context, String currencyCode) {
        // Handle input like "USD - US Dollar ($)"
        String code = extractCurrencyCode(currencyCode);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(CURRENCY_KEY, code).apply();
    }
    
    /**
     * Extract currency code from display string
     */
    public static String extractCurrencyCode(String currencyDisplayString) {
        if (currencyDisplayString == null || currencyDisplayString.isEmpty()) {
            return DEFAULT_CURRENCY;
        }
        if (currencyDisplayString.contains("-")) {
            return currencyDisplayString.split("-")[0].trim();
        }
        return currencyDisplayString.trim();
    }

    /**
     * Convert price between currencies
     */
    public static double convert(double price, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return price;
        }

        Double fromRate = EXCHANGE_RATES.get(fromCurrency.toUpperCase());
        Double toRate = EXCHANGE_RATES.get(toCurrency.toUpperCase());

        if (fromRate == null || toRate == null) {
            return price; // Fallback to original price if rate not found
        }

        // Convert to base (MAD) then to target
        double priceInBase = price / fromRate;
        return priceInBase * toRate;
    }

    /**
     * Format price with currency symbol
     */
    public static String formatPrice(double price, String currencyCode) {
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance();
            format.setCurrency(Currency.getInstance(currencyCode.toUpperCase()));
            return format.format(price);
        } catch (Exception e) {
            // Fallback if currency code is not recognized by Java
            return String.format("%.2f %s", price, currencyCode);
        }
    }
    
    /**
     * Full process: Get preferred currency, convert, and format
     */
    public static String formatLocalizedPrice(Context context, double originalPrice, String originalCurrency) {
        String preferredCurrency = getCurrency(context);
        double convertedPrice = convert(originalPrice, originalCurrency, preferredCurrency);
        return formatPrice(convertedPrice, preferredCurrency);
    }
}
