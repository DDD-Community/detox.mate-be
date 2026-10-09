package com.detoxmate.user.infrastructure.persistence;

import java.sql.SQLException;
import java.util.regex.Pattern;

public final class UserCodeCollisionClassifier {

    private static final Pattern MYSQL_USER_CODE_KEY = Pattern.compile(
            "for key ['`](?:[a-z0-9_]+\\.)?uk_users_user_code['`]$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern H2_USER_CODE_INDEX = Pattern.compile(
            "^Unique index or primary key violation: \"(?:[a-z0-9_]+\\.)?uk_users_user_code"
                    + "(?:_INDEX_[a-z0-9]+| INDEX (?:[a-z0-9_]+\\.)?uk_users_user_code_INDEX_[a-z0-9]+)? ON ",
            Pattern.CASE_INSENSITIVE
    );

    private UserCodeCollisionClassifier() {
    }

    public static boolean isUserCodeCollision(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getMessage() != null) {
                if (sql.getErrorCode() == 1062 && "23000".equals(sql.getSQLState())
                        && MYSQL_USER_CODE_KEY.matcher(sql.getMessage()).find()) {
                    return true;
                }
                if (sql.getErrorCode() == 23505 && "23505".equals(sql.getSQLState())
                        && H2_USER_CODE_INDEX.matcher(sql.getMessage()).find()) {
                    return true;
                }
            }
        }
        return false;
    }
}
