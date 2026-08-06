package com.dequeue.common.util;

import java.security.SecureRandom;
import java.util.Locale;

public final class SlugUtils {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    
    private SlugUtils() {}
    
    public static String generateVendorCode(String shopName) {
        String slug = shopName.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        if (slug.length() > 20) {
            slug = slug.substring(0, 20);
        }
        
        return slug + "-" + generateRandomSuffix(4);
    }
    
    private static String generateRandomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
