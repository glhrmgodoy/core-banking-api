package com.godoy.corebanking.util;

import java.util.Locale;
import java.util.Set;

public final class NameUtils {

    private static final Set<String> CONNECTORS = Set.of("da", "das", "de", "do", "dos", "e");

    private NameUtils() {
    }

    public static String mask(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String[] parts = fullName.strip().split("\\s+");
        StringBuilder masked = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (CONNECTORS.contains(parts[i].toLowerCase(Locale.ROOT))) {
                continue;
            }
            masked.append(' ')
                    .append(Character.toUpperCase(parts[i].charAt(0)))
                    .append('.');
        }
        return masked.toString();
    }
}
