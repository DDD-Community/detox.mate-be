package com.detoxmate.user.infrastructure;

import com.detoxmate.user.domain.UserCode;
import com.detoxmate.user.service.UserCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomUserCodeGenerator implements UserCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public UserCode generate() {
        char[] characters = new char[UserCode.LENGTH];

        for(int i=0;i<UserCode.LENGTH;i++) {
            int index = random.nextInt(UserCode.ALPHABET.length());
            characters[i] = UserCode.ALPHABET.charAt(index);
        }

        return new UserCode(new String(characters));
    }
}
