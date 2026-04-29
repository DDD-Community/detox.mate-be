package com.detoxmate.activityrecordchallengestatus.repository;

import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityRecordChallengeStatusRepository extends JpaRepository<ActivityRecordChallengeStatus, Integer> {

    @Query("""
            select s
            from ActivityRecordChallengeStatus s
            where s.groupChallengeId = :groupChallengeId
              and s.activityRecordId = :activityRecordId
            """)
    Optional<ActivityRecordChallengeStatus> findByChallengeRecord(Long groupChallengeId, Long activityRecordId);
}
