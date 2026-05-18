package com.detoxmate.notification.util;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.user.UserErrorCode;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationUserReader {

    private final UserRepository userRepository;

    public String findDisplayName(Long userId){
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    public Map<Long, String> findDisplayNames(Set<Long> userIds) {
        Map<Long, String> displayNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));

        if (displayNames.size() != userIds.size()) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        return displayNames;
    }

    public boolean isPushNotificationEnabled(Long userId) {
        return userRepository.findById(userId)
                .map(User::isPushNotificationEnabled)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
