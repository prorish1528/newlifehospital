package com.newlife.hospital.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptionUtilTest {

    private final EncryptionUtil util = EncryptionUtil.fromBase64Key(EncryptionUtil.generateKey());

    @Test
    void roundTripsUnicode() {
        String plain = "Aadhaar 234567890123 / नई ज़िंदगी";
        assertThat(util.decrypt(util.encrypt(plain))).isEqualTo(plain);
    }

    @Test
    void sameInputProducesDifferentCiphertexts() {
        assertThat(util.encrypt("234567890123")).isNotEqualTo(util.encrypt("234567890123"));
    }

    @Test
    void associatedDataMustMatch() {
        String c = util.encrypt("K1234567", "patients.id_number");
        assertThat(util.decrypt(c, "patients.id_number")).isEqualTo("K1234567");
        assertThatThrownBy(() -> util.decrypt(c, "other.column")).isInstanceOf(CryptoException.class);
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] bytes = Base64.getDecoder().decode(util.encrypt("secret"));
        bytes[bytes.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThatThrownBy(() -> util.decrypt(tampered)).isInstanceOf(CryptoException.class);
    }

    @Test
    void wrongKeyCannotDecrypt() {
        EncryptionUtil other = EncryptionUtil.fromBase64Key(EncryptionUtil.generateKey());
        assertThatThrownBy(() -> other.decrypt(util.encrypt("secret"))).isInstanceOf(CryptoException.class);
    }

    @Test
    void rejectsKeysThatAreNot256Bits() {
        // The original template's "NewLifeHospitalSec" key is 18 bytes and would be rejected by AES itself.
        assertThatThrownBy(() -> new EncryptionUtil("NewLifeHospitalSec".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blindIndexIsDeterministicAndKeyed() {
        BlindIndexer a = new BlindIndexer(EncryptionUtil.generateKey());
        BlindIndexer b = new BlindIndexer(EncryptionUtil.generateKey());
        assertThat(a.index("AADHAAR||234567890123")).isEqualTo(a.index("AADHAAR||234567890123"));
        assertThat(a.index("AADHAAR||234567890123")).isNotEqualTo(b.index("AADHAAR||234567890123"));
    }
}
