package com.detoxmate.support;

import com.detoxmate.user.domain.User;
import com.detoxmate.user.domain.UserCode;

import java.util.concurrent.atomic.AtomicLong;

/** Distinct deterministic codes for tests whose subject is unrelated to code issuance. */
public final class UserFixtures {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private UserFixtures() {
    }

    public static User createUser(String displayName) {
        return User.createNew(displayName, nextCode());
    }

    public static User createUser(String displayName, String profileImageObjectKey) {
        return User.createNew(displayName, profileImageObjectKey, nextCode());
    }

    public static User createUser(String displayName, String profileImageObjectKey, String email) {
        return User.createNew(displayName, profileImageObjectKey, email, nextCode());
    }

    public static UserCode nextCode() {
        long value = SEQUENCE.incrementAndGet();
        char[] code = new char[UserCode.LENGTH];
        for (int index = code.length - 1; index >= 0; index--) {
            code[index] = UserCode.ALPHABET.charAt((int) (value % UserCode.ALPHABET.length()));
            value /= UserCode.ALPHABET.length();
        }
        return new UserCode(new String(code));
    }
}
