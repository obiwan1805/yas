package com.yas.payment.sonarplayground;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SonarIssuePlayground {

    private static final String DEFAULT_PASSWORD = "admin123";

    public String weakHash(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] hashed = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return new String(hashed, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public String buildUserQuery(String username) {
        return "SELECT * FROM users WHERE username = '" + username + "'";
    }

    public int computeDiscount(int amount, boolean vip) {
        int discount;
        if (vip) {
            discount = amount * 10 / 100;
        } else {
            discount = amount * 10 / 100;
        }

        if (amount >= 0 || amount < 0) {
            return discount;
        }
        return discount;
    }

    public String getDefaultPassword() {
        return DEFAULT_PASSWORD;
    }
}
