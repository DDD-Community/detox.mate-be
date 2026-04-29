package com.detoxmate.activityrecordchallengestatus.service;


import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import com.detoxmate.activityrecordchallengestatus.repository.ActivityRecordChallengeStatusRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityRecordChallengeStatusService {

    private final ActivityRecordChallengeStatusRepository statusRepository;

    @Transactional
    public ActivityRecordChallengeStatus create(Long groupChallengeId, Long activityRecordId) {
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(groupChallengeId, activityRecordId);

        return statusRepository.save(status);
    }

    @Transactional
    public void increaseCommentCount(Long groupChallengeId, Long activityRecordId) {
        ActivityRecordChallengeStatus status = getStatus(groupChallengeId, activityRecordId);
        status.increaseCommentCount();
    }

    @Transactional
    public void increaseReactionCount(Long groupChallengeId, Long activityRecordId) {
        ActivityRecordChallengeStatus status = getStatus(groupChallengeId, activityRecordId);

        status.increaseReactionCount();
    }

    @Transactional
    public void increasePokeCount(Long groupChallengeId, Long activityRecordId) {
        ActivityRecordChallengeStatus status = getStatus(groupChallengeId, activityRecordId);

        status.increasePokeCount();
    }

    private ActivityRecordChallengeStatus getStatus(Long groupChallengeId, Long activityRecordId) {
        return statusRepository.findByChallengeRecord(groupChallengeId, activityRecordId)
                .orElseThrow(() -> new CustomException(
                        FeedErrorCode.ACTIVITY_RECORD_CHALLENGE_STATUS_NOT_FOUND));
    }

}
