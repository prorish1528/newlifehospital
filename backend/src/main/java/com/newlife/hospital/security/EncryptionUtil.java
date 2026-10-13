package com.newlife.hospital.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Symmetric encryption utility based on AES-256 in GCM mode (authenticated encryption).
 *
 * <p>Differences from the original ECB-based template, and why:
 * <ul>
 *   <li>{@code Cipher.getInstance("AES")} silently means {@code AES/ECB/PKCS5Padding}. ECB is deterministic and
 *       leaks patterns (identical plaintexts give identical ciphertexts) and has no integrity protection.
 *       GCM uses a fresh random 96-bit IV per message plus a 128-bit authentication tag, so tampered
 *       ciphertext fails to decrypt instead of yielding garbage.</li>
 *   <li>The template key {@code "NewLifeHospitalSec"} is 18 bytes, which AES rejects with
 *       {@code InvalidKeyException}; AES-256 needs exactly 32 random bytes. Keys are never hard-coded: they come
 *       from configuration ({@code APP_CRYPTO_KEY}, base64) backed by the environment or a secret manager.</li>
 *   <li>{@code String.getBytes()} uses the platform charset; UTF-8 is explicit so values round-trip everywhere.</li>
 * </ul>
 *
 * <p>Ciphertext format: {@code base64( IV[12] || ciphertext || tag[16] )}.
 *
 * <p>Developer CLI. The key is read from the {@code APP_CRYPTO_KEY} environment variable and plaintext from stdin,
 * so neither lands in shell history:
 * <pre>
 *   java -cp target/classes com.newlife.hospital.security.EncryptionUtil keygen
 *   echo 123456789012 | java -cp target/classes com.newlife.hospital.security.EncryptionUtil encrypt patients.id_number
 *   java -cp target/classes com.newlife.hospital.security.EncryptionUtil decrypt BASE64_CIPHERTEXT patients.id_number
 * </pre>
 */
public final class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec keySpec;

    public EncryptionUtil(byte[] key) {
        if (key == null || key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
        }
        this.keySpec = new SecretKeySpec(Arrays.copyOf(key, key.length), ALGORITHM);
    }

    /** Builds an instance from a base64-encoded 32-byte key, e.g. the value of {@code APP_CRYPTO_KEY}. */
    public static EncryptionUtil fromBase64Key(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("Encryption key is not configured");
        }
        return new EncryptionUtil(Base64.getDecoder().decode(base64Key.trim()));
    }

    /** Generates a new random AES-256 key, base64 encoded. */
    public static String generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
            generator.init(KEY_LENGTH_BYTES * 8, RANDOM);
            return Base64.getEncoder().encodeToString(generator.generateKey().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Key generation failed", e);
        }
    }

    public String encrypt(String value) {
        return encrypt(value, null);
    }

    /**
     * Encrypts {@code value}. When {@code associatedData} is given (for example {@code "patients.id_number"}) the
     * ciphertext is bound to that context and only decrypts with the same associated data.
     */
    public String encrypt(String value, String associatedData) {
        if (value == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedValue) {
        return decrypt(encryptedValue, null);
    }

    public String decrypt(String encryptedValue, String associatedData) {
        if (encryptedValue == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length < IV_LENGTH_BYTES + TAG_LENGTH_BITS / 8) {
                throw new CryptoException("Ciphertext is too short", null);
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec,
                    new GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH_BYTES));
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(payload, IV_LENGTH_BYTES, payload.length - IV_LENGTH_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CryptoException("Decryption failed: wrong key, wrong context or tampered data", e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: EncryptionUtil keygen | encrypt [aad] | decrypt <ciphertext> [aad]");
            System.exit(2);
        }
        if ("keygen".equals(args[0])) {
            System.out.println(generateKey());
            return;
        }
        String envKey = System.getenv("APP_CRYPTO_KEY");
        if (envKey == null || envKey.isBlank()) {
            System.err.println("Set APP_CRYPTO_KEY (base64, 32 bytes) first.");
            System.exit(2);
        }
        EncryptionUtil util = fromBase64Key(envKey);
        switch (args[0]) {
            case "encrypt" -> {
                String plain = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
                System.out.println(util.encrypt(plain, args.length > 1 ? args[1] : null));
            }
            case "decrypt" -> {
                if (args.length < 2) {
                    System.err.println("decrypt requires the base64 ciphertext argument");
                    System.exit(2);
                }
                System.out.println(util.decrypt(args[1], args.length > 2 ? args[2] : null));
            }
            default -> {
                System.err.println("Unknown command: " + args[0]);
                System.exit(2);
            }
        }
    }
}
