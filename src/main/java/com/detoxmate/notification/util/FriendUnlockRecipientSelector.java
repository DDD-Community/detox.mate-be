package com.detoxmate.notification.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class FriendUnlockRecipientSelector {

    private static final int MAX_RECIPIENTS = 3;

    public List<Long> select(List<Long> candidateUserIds){
        //중복을 제거하고 섞을 수 있는 수정 가능한 목록으로 복사
        List<Long> shuffledUserIds = new ArrayList<>(new LinkedHashSet<>(candidateUserIds));

        Collections.shuffle(shuffledUserIds);

        return shuffledUserIds.stream()
                .limit(MAX_RECIPIENTS)
                .toList();
    }
}
