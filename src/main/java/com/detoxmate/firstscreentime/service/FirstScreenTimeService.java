package com.detoxmate.firstscreentime.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.firstscreentime.FirstScreenTimeErrorCode;
import com.detoxmate.firstscreentime.domain.FirstScreenTime;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeCreateRequest;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeResponse;
import com.detoxmate.firstscreentime.repository.FirstScreenTimeRepository;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FirstScreenTimeService {

    private final FirstScreenTimeRepository firstScreenTimeRepository;
    private final GroupChallengeParticipantRepository participantRepository;

    @Transactional
    public FirstScreenTimeResponse create(Long userId, FirstScreenTimeCreateRequest request) {
        if (!participantRepository.existsActiveByIdAndUserId(request.groupChallengeParticipantId(), userId)) {
            throw new CustomException(FirstScreenTimeErrorCode.CAN_REGISTER_PARTICIPATE_CHALLENGE);
        }

        if (firstScreenTimeRepository.existsByGroupChallengeParticipantId(request.groupChallengeParticipantId())) {
            throw new CustomException(FirstScreenTimeErrorCode.FIRST_SCREEN_TIME_ALREADY_REGISTER);
        }

        FirstScreenTime firstScreenTime = FirstScreenTime.create(
                userId,
                request.groupChallengeParticipantId(),
                request.screenTimeMinutes(),
                request.recordDate()
        );

        FirstScreenTime saved = firstScreenTimeRepository.save(firstScreenTime);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FirstScreenTimeResponse get(Long userId, Long groupChallengeParticipantId) {
        if (!participantRepository.existsActiveByIdAndUserId(groupChallengeParticipantId, userId)) {
            throw new CustomException(FirstScreenTimeErrorCode.CAN_REGISTER_PARTICIPATE_CHALLENGE);
        }

        FirstScreenTime firstScreenTime = firstScreenTimeRepository.findByGroupChallengeParticipantId(groupChallengeParticipantId)
                .orElseThrow(() -> new CustomException(FirstScreenTimeErrorCode.CAN_NOT_FIND_PARTICIPATE_INFORMATION));

        return toResponse(firstScreenTime);
    }

    private FirstScreenTimeResponse toResponse(FirstScreenTime firstScreenTime) {
        return new FirstScreenTimeResponse(
                firstScreenTime.getId(),
                firstScreenTime.getGroupChallengeParticipantId(),
                firstScreenTime.getScreenTimeMinutes(),
                firstScreenTime.getRecordDate(),
                firstScreenTime.getCreatedAt()
        );
    }
}
