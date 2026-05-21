package com.detoxmate.notification.util;

import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationGoalReader {

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    @Transactional(readOnly = true)
    public boolean hasGoal(Long userId){
        return userUsageGoalTimeRepository.existsGoalByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasNoGoal(Long userId){
        return !hasGoal(userId);
    }
}
