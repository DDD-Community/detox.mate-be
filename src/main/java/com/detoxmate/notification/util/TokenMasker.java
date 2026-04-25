package com.detoxmate.notification.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class TokenMasker {

    private TokenMasker(){}

    public static String mask(String token){
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
