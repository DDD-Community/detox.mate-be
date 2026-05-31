package com.detoxmate.user.service;

import com.detoxmate.auth.dto.AuthLoginResponse;
import com.detoxmate.user.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevAuthService {

    private static final int DYNAMIC_TEST_USER_MIN_NUMBER = 1;
    private static final int DYNAMIC_TEST_USER_MAX_NUMBER = 100;
    private static final String DYNAMIC_TEST_USER_PREFIX = "test";
    private static final Map<String, TestUser> LEGACY_TEST_USERS = Map.of(
            "front-a", new TestUser("프론트 테스트 A", null),
            "front-b", new TestUser("프론트 테스트 B", null),
            "front-c", new TestUser("프론트 테스트 C", null),
            "server-a", new TestUser("서버 테스트 A", null),
            "server-b", new TestUser("서버 테스트 B", null),
            "server-c", new TestUser("서버 테스트 C", null)
    );

    private final AuthService authService;

    @Transactional
    public AuthLoginResponse testLogin(String testUserKey) {
        TestUser testUser = resolveTestUser(testUserKey);
        if (testUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported test user key");
        }

        return authService.loginWithSocialUser(
                SocialProvider.TEST,
                testUserKey,
                testUser.displayName(),
                testUser.profileImageObjectKey()
        );
    }

    private TestUser resolveTestUser(String testUserKey) {
        TestUser legacyTestUser = LEGACY_TEST_USERS.get(testUserKey);
        if (legacyTestUser != null) {
            return legacyTestUser;
        }

        Integer dynamicTestUserNumber = parseDynamicTestUserNumber(testUserKey);
        if (dynamicTestUserNumber == null) {
            return null;
        }

        return new TestUser("테스트 유저 " + dynamicTestUserNumber, null);
    }

    private Integer parseDynamicTestUserNumber(String testUserKey) {
        if (testUserKey == null || !testUserKey.startsWith(DYNAMIC_TEST_USER_PREFIX)) {
            return null;
        }

        String numberText = testUserKey.substring(DYNAMIC_TEST_USER_PREFIX.length());
        if (numberText.isBlank() || numberText.startsWith("0")) {
            return null;
        }

        try {
            int number = Integer.parseInt(numberText);
            if (!numberText.equals(String.valueOf(number))
                    || number < DYNAMIC_TEST_USER_MIN_NUMBER
                    || number > DYNAMIC_TEST_USER_MAX_NUMBER) {
                return null;
            }
            return number;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record TestUser(String displayName, String profileImageObjectKey) {
    }
}
