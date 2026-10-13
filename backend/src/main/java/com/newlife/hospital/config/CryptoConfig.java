package com.newlife.hospital.config;

import com.newlife.hospital.security.BlindIndexer;
import com.newlife.hospital.security.EncryptionUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CryptoConfig {

    /** BCrypt, cost 12, for login passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public EncryptionUtil encryptionUtil(AppProperties props) {
        return EncryptionUtil.fromBase64Key(props.crypto().key());
    }

    @Bean
    public BlindIndexer blindIndexer(AppProperties props) {
        return new BlindIndexer(props.crypto().hmacKey());
    }
}
