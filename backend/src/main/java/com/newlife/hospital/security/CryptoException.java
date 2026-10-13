package com.newlife.hospital.security;

/** Unchecked wrapper for cryptographic failures; never carries plaintext in its message. */
public class CryptoException extends RuntimeException {
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
