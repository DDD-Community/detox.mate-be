package com.detoxmate.applock.service;

import com.detoxmate.applock.domain.TimeLimit;
import com.detoxmate.applock.repository.TimeLimitRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TimeLimitService {

    private final TimeLimitRepository timeLimitRepository;
    private final UserRepository userRepository;

    @Transactional
    public int set(Long userId, Integer totalLockMinutes) {
        User user = lockActiveUser(userId);
        TimeLimit timeLimit = timeLimitRepository.findByUser_Id(userId).orElse(null);
        if (timeLimit == null) {
            timeLimit = timeLimitRepository.save(TimeLimit.create(user, totalLockMinutes));
        } else {
            timeLimit.changeTotalLockMinutes(totalLockMinutes);
        }
        return timeLimit.getTotalLockMinutes();
    }

    @Transactional(readOnly = true)
    public int get(Long userId) {
        return timeLimitRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잠금 시간 설정을 찾을 수 없습니다."))
                .getTotalLockMinutes();
    }

    @Transactional
    public void delete(Long userId) {
        lockActiveUser(userId);
        timeLimitRepository.findByUser_Id(userId).ifPresent(timeLimitRepository::delete);
    }

    private User lockActiveUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "탈퇴한 사용자입니다.");
        }
        return user;
    }
}
