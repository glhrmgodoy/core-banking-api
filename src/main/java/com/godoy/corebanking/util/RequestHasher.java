package com.godoy.corebanking.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestHasher {

    private RequestHasher() {
    }

    public static String sha256(Object... fields) {
        StringBuilder canonical = new StringBuilder();
        for (Object field : fields) {
            if (field == null) {
                canonical.append('-');
                continue;
            }
            String value = field instanceof BigDecimal amount
                    ? amount.stripTrailingZeros().toPlainString()
                    : field.toString();
            canonical.append(value.length()).append(':').append(value);
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
