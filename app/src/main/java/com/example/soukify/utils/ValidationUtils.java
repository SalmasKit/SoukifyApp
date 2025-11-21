package com.example.soukify.utils;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Regex pour numéro marocain : commence par 06 ou 07, suivi de 8 chiffres
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[67]\\d{8}$");

    // Vérifier si le téléphone est valide
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    // Vérifier si le mot de passe est valide (optionnel)
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6; // exemple : min 6 caractères
    }
}
