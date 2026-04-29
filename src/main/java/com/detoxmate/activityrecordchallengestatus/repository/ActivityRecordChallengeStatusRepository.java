package com.detoxmate.activityrecordchallengestatus.repository;

import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRecordChallengeStatusRepository extends JpaRepository<ActivityRecordChallengeStatus, Integer> {
}
