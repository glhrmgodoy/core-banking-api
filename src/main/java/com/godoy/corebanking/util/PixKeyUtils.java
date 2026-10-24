package com.godoy.corebanking.util;

import com.godoy.corebanking.domain.enums.PixKeyType;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PixKeyUtils {

    private static final Pattern MASKED_CPF = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern UUID_FORMAT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private PixKeyUtils() {
    }

    public static String normalize(PixKeyType type, String value) {
        if (type == PixKeyType.RANDOM || value == null) {
            return null;
        }
        String stripped = value.strip();
        return switch (type) {
            case CPF -> stripped.replaceAll("\\D", "");
            case EMAIL -> stripped.toLowerCase(Locale.ROOT);
            case PHONE -> stripped;
            case RANDOM -> null;
        };
    }

    public static String normalizeForLookup(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        if (MASKED_CPF.matcher(stripped).matches()) {
            return stripped.replaceAll("\\D", "");
        }
        if (stripped.contains("@") || UUID_FORMAT.matcher(stripped).matches()) {
            return stripped.toLowerCase(Locale.ROOT);
        }
        return stripped;
    }
}
