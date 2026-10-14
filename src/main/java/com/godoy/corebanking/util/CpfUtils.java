package com.godoy.corebanking.util;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static String mask(String cpfDigits) {
        if (cpfDigits == null || !cpfDigits.matches("\\d{11}")) {
            return null;
        }
        return "***.%s.%s-**".formatted(cpfDigits.substring(3, 6), cpfDigits.substring(6, 9));
    }
}
