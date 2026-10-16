package com.godoy.corebanking.util;

public final class AccountNumberUtils {

    private static final int MIN_DIGITS = 6;

    private AccountNumberUtils() {
    }

    public static String format(long sequence) {
        if (sequence < 1) {
            throw new IllegalArgumentException("O número da conta deve ser positivo: " + sequence);
        }
        String base = String.format("%0" + MIN_DIGITS + "d", sequence);
        return base + "-" + checkDigit(base);
    }

    static int checkDigit(String digits) {
        int sum = 0;
        int weight = 2;
        for (int i = digits.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(digits.charAt(i)) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }
}
