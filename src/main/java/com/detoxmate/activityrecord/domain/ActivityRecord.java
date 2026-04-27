package com.detoxmate.activityrecord.domain;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "activity_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecord {

    @Id
    @Column(name = "activity_record_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_challenge_participant_id", nullable = false)
    private GroupChallengeParticipant groupChallengeParticipant;

    @Column(name = "activity_image_object_key", length = 500)
    private String activityImageObjectKey;

    @Lob
    @Column(name = "reflection_text")
    private String reflectionText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ActivityRecordDetail> details = new ArrayList<>();

    private ActivityRecord(
            User user,
            GroupChallengeParticipant groupChallengeParticipant,
            String activityImageObjectKey,
            String reflectionText
    ) {
        validateUser(user);
        validateGroupChallengeParticipant(groupChallengeParticipant);
        this.user = user;
        this.groupChallengeParticipant = groupChallengeParticipant;
        this.activityImageObjectKey = activityImageObjectKey;
        this.reflectionText = reflectionText;
    }

    public static ActivityRecord create(
            User user,
            GroupChallengeParticipant groupChallengeParticipant,
            String activityImageObjectKey,
            String reflectionText
    ) {
        return new ActivityRecord(user, groupChallengeParticipant, activityImageObjectKey, reflectionText);
    }

    public ActivityRecordDetail addDetail(UserUsageGoalTime userUsageGoalTime, int useMinutes, boolean isAchieved) {
        ActivityRecordDetail detail = ActivityRecordDetail.create(this, userUsageGoalTime, useMinutes, isAchieved);
        details.add(detail);
        return detail;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user 는 필수입니다.");
        }
    }

    private static void validateGroupChallengeParticipant(GroupChallengeParticipant groupChallengeParticipant) {
        if (groupChallengeParticipant == null) {
            throw new IllegalArgumentException("groupChallengeParticipant 는 필수입니다.");
        }
    }
}
