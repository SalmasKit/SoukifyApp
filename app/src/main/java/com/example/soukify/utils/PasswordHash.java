package com.example.soukify.utils;
import at.favre.lib.crypto.bcrypt.BCrypt;
public class PasswordHash {
    private static final int COST = 12;

    public static String hashPassword(String plainPassword) {
        return BCrypt.withDefaults().hashToString(COST, plainPassword.toCharArray());
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
