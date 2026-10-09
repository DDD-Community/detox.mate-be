package com.detoxmate.user.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.user.UserCodeException;

import java.util.Locale;
import java.util.regex.Pattern;

public record UserCode(String value) {

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 5;

    private static final Pattern CANONICAL = Pattern.compile("[" + ALPHABET + "]{" + LENGTH + "}");

    private static final Pattern INPUT =
            Pattern.compile(
                    CANONICAL.pattern(),
                    Pattern.CASE_INSENSITIVE
            );

    public UserCode {
        if (value == null || !CANONICAL.matcher(value).matches()) {
            throw new CustomException(UserCodeException.INVALID_USER_CODE);
        }
    }

    public static UserCode parse(String input) {
        if (input == null) {
            throw new CustomException(UserCodeException.USER_CODE_REQUIRED);
        }

        String trimmed = input.strip();

        if (!INPUT.matcher(trimmed).matches()) {
            throw new CustomException(UserCodeException.INVALID_USER_CODE);
        }

        return new UserCode(
                trimmed.toUpperCase(Locale.ROOT)
        );
    }

    public String formatted() {
        return value;
    }
}
