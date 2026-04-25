package com.detoxmate.activityrecord.repository;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

    List<ActivityRecord> findAllByGroupChallengeParticipantIdOrderByCreatedAtDesc(Long groupChallengeParticipantId);

    List<ActivityRecord> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
