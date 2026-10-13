package com.newlife.hospital.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed HMAC-SHA256 "blind index". Lets the database enforce uniqueness and answer exact-match lookups on
 * encrypted columns (such as government ID numbers) without storing or querying the plaintext.
 */
public final class BlindIndexer {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public BlindIndexer(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("HMAC key is not configured");
        }
        byte[] bytes = Base64.getDecoder().decode(base64Key.trim());
        if (bytes.length < 32) {
            throw new IllegalArgumentException("HMAC key must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, ALGORITHM);
    }

    public String index(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Blind index computation failed", e);
        }
    }
}
