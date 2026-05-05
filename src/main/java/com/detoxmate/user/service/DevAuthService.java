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

    private static final Map<String, TestUser> TEST_USERS = Map.of(
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
        TestUser testUser = TEST_USERS.get(testUserKey);
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

    private record TestUser(String displayName, String profileImageObjectKey) {
    }
}
