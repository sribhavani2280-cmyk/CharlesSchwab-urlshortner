package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/** Generates collision-resistant base62 short codes and validates custom aliases. */
@Component
public class CodeGenerator {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    public String validateAlias(String alias) {
        if (alias == null || !ALIAS_PATTERN.matcher(alias).matches()) {
            throw ApiException.badRequest(
                    "Alias must be 3-64 characters of letters, digits, hyphen or underscore");
        }
        return alias;
    }
}
