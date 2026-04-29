package com.detoxmate.activityrecord.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimeResponse;
import com.detoxmate.activityrecord.dto.CurrentUsageGoalTimesResponse;
import com.detoxmate.activityrecord.dto.UsageGoalTimeResponse;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimeRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetRequest;
import com.detoxmate.activityrecord.dto.UserUsageGoalTimesSetResponse;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserUsageGoalTimeService {

    private static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    private static final String USAGE_GOAL_TYPE_NOT_FOUND_MESSAGE = "사용 가능한 목표 타입이 아닙니다.";

    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final UsageGoalTypeRepository usageGoalTypeRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserUsageGoalTimesSetResponse setGoalTimes(Long userId, UserUsageGoalTimesSetRequest request) {
        User user = findUser(userId);
        List<UserUsageGoalTime> goalTimes = request.goals().stream()
                .map(goal -> toUserUsageGoalTime(user, goal))
                .toList();

        List<UserUsageGoalTime> savedGoalTimes = userUsageGoalTimeRepository.saveAllAndFlush(goalTimes);
        return new UserUsageGoalTimesSetResponse(savedGoalTimes.stream()
                .map(UsageGoalTimeResponse::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public CurrentUsageGoalTimesResponse getCurrentGoalTimes(Long userId) {
        LatestGoalTimes latestGoalTimes = LatestGoalTimes.from(userUsageGoalTimeRepository.findAllByUser_Id(userId));

        return new CurrentUsageGoalTimesResponse(Arrays.stream(UsageGoalTypeCode.values())
                .map(latestGoalTimes::findBy)
                .flatMap(Optional::stream)
                .map(CurrentUsageGoalTimeResponse::from)
                .toList());
    }

    private UserUsageGoalTime toUserUsageGoalTime(User user, UserUsageGoalTimeRequest goal) {
        UsageGoalType usageGoalType = usageGoalTypeRepository.findByCode(goal.usageGoalType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, USAGE_GOAL_TYPE_NOT_FOUND_MESSAGE));

        return UserUsageGoalTime.create(user, usageGoalType, goal.goalMinutes());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }
}
