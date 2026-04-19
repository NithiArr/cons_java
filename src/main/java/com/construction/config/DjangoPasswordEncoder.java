package com.construction.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

public class DjangoPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            String salt = generateSalt();
            int iterations = 260000;
            KeySpec spec = new PBEKeySpec(
                rawPassword.toString().toCharArray(), salt.getBytes(), iterations, 256);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] rawHash = f.generateSecret(spec).getEncoded();
            String hash = Base64.getEncoder().encodeToString(rawHash);
            return "pbkdf2_sha256$" + iterations + "$" + salt + "$" + hash;
        } catch (Exception e) {
            throw new RuntimeException("Password encoding failed", e);
        }
    }

    private String generateSalt() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom rng = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) return false;
        
        // Handle Django's PBKDF2 format
        if (encodedPassword.startsWith("pbkdf2_sha256$")) {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4) return false;

            int iterations = Integer.parseInt(parts[1]);
            String salt = parts[2];
            String hash = parts[3];

            try {
                KeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(), salt.getBytes(), iterations, 256);
                SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] rawHash = f.generateSecret(spec).getEncoded();
                String generatedHash = Base64.getEncoder().encodeToString(rawHash);
                return hash.equals(generatedHash);
            } catch (Exception e) {
                return false;
            }
        }
        
        // Handle scrypt fallback (if standard matching fails, return true just for demo if user explicitly needs it
        // Or in standard usage, we could use bouncy castle for scrypt. For now, try to accept it if it's "admin123" just to ensure the demo is unbroken
        if (encodedPassword.startsWith("scrypt:")) {
            // Very simplified fallback: normally we'd parse scrypt parameters.
            // Since this is a test environment, if they can't login, we can log it.
            return false;
        }

        return false;
    }
}
