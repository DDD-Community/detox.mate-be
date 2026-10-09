package com.detoxmate.user.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class UserCodeCollisionClassifierTest {

    @Test
    @DisplayName("MySQL의 개인 코드 유니크 키 충돌만 재시도 대상으로 분류한다")
    void isUserCodeCollision_recognizesNamedMysqlUniqueKey() {
        assertThat(classify("Duplicate entry 'ABCDE' for key 'users.uk_users_user_code'", "23000", 1062))
                .isTrue();
        assertThat(classify("Duplicate entry 'ABCDE' for key 'uk_users_user_code'", "23000", 1062))
                .isTrue();
    }

    @Test
    @DisplayName("이메일·소셜 연결·다른 무결성 위반은 코드 충돌로 분류하지 않는다")
    void isUserCodeCollision_rejectsUnrelatedIntegrityFailures() {
        assertThat(classify("Duplicate entry 'uk_users_user_code' for key 'users.uk_users_email'", "23000", 1062))
                .isFalse();
        assertThat(classify("Duplicate entry 'for key 'uk_users_user_code' injected' for key 'users.uk_users_email'", "23000", 1062))
                .isFalse();
        assertThat(classify("Duplicate entry '1-KAKAO' for key 'uk_social_login_users_user_provider'", "23000", 1062))
                .isFalse();
        assertThat(classify("Duplicate entry 'ABCDE' for key 'uk_users_user_code_extra'", "23000", 1062))
                .isFalse();
        assertThat(classify("Column 'user_code' cannot be null", "23000", 1048)).isFalse();
        assertThat(classify("Data too long for column 'user_code'", "22001", 1406)).isFalse();
    }

    @Test
    @DisplayName("H2 테스트 DB에서도 해당 코드 인덱스의 중복 오류만 분류한다")
    void isUserCodeCollision_recognizesOnlyNamedH2UniqueIndex() {
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_USER_CODE_INDEX_4 ON PUBLIC.USERS(USER_CODE) VALUES ('ABCDE')\"", "23505", 23505))
                .isTrue();
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_USER_CODE INDEX PUBLIC.UK_USERS_USER_CODE_INDEX_4 ON PUBLIC.USERS(USER_CODE NULLS FIRST) VALUES ( /* 11 */ 'ABCDE' )\"; SQL statement:", "23505", 23505))
                .isTrue();
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES ('uk_users_user_code')\"", "23505", 23505))
                .isFalse();
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_EMAIL INDEX PUBLIC.UK_USERS_EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES ('uk_users_user_code')\"", "23505", 23505))
                .isFalse();
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_EMAIL INDEX PUBLIC.UK_USERS_USER_CODE_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES ('uk_users_user_code')\"", "23505", 23505))
                .isFalse();
        assertThat(classify("Unique index or primary key violation: \"PUBLIC.UK_USERS_USER_CODE INDEX PUBLIC.UK_USERS_EMAIL_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES ('uk_users_user_code')\"", "23505", 23505))
                .isFalse();
    }

    private boolean classify(String message, String sqlState, int code) {
        return UserCodeCollisionClassifier.isUserCodeCollision(new DataIntegrityViolationException(
                "insert rejected", new SQLIntegrityConstraintViolationException(message, sqlState, code)
        ));
    }
}
