package com.detoxmate.group.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomInviteCodeGenerator implements InviteCodeGenerator {

    private static final char[] CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int INVITE_CODE_LENGTH = 5;

    @Override
    public String generate() {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);

        for (int index = 0; index < INVITE_CODE_LENGTH; index++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(CHARSET.length);
            builder.append(CHARSET[randomIndex]);
        }

        return builder.toString();
    }
}
