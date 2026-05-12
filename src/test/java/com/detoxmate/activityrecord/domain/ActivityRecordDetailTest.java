package com.detoxmate.activityrecord.domain;

import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityRecordDetailTest {

    @Test
    void 사용시간을_수정하면_목표시간_기준으로_달성여부를_다시_계산한다() {
        ActivityRecord activityRecord = activityRecord();
        UserUsageGoalTime goalTime = userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 120);
        ActivityRecordDetail detail = ActivityRecordDetail.create(activityRecord, goalTime, 180, false);

        detail.correctUseMinutes(100);

        assertThat(detail.getUseMinutes()).isEqualTo(100);
        assertThat(detail.isAchieved()).isTrue();
    }

    @Test
    void 수정한_사용시간이_목표시간을_넘으면_미달성으로_계산한다() {
        ActivityRecord activityRecord = activityRecord();
        UserUsageGoalTime goalTime = userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 120);
        ActivityRecordDetail detail = ActivityRecordDetail.create(activityRecord, goalTime, 100, true);

        detail.correctUseMinutes(121);

        assertThat(detail.getUseMinutes()).isEqualTo(121);
        assertThat(detail.isAchieved()).isFalse();
    }

    @Test
    void 수정한_사용시간은_0_이상이어야_한다() {
        ActivityRecord activityRecord = activityRecord();
        UserUsageGoalTime goalTime = userUsageGoalTime(UsageGoalTypeCode.TOTAL_USAGE, 120);
        ActivityRecordDetail detail = ActivityRecordDetail.create(activityRecord, goalTime, 100, true);

        assertThatThrownBy(() -> detail.correctUseMinutes(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ActivityRecord activityRecord() {
        User user = User.createNew("tester");
        return ActivityRecord.create(user, nullSafeParticipant(), "activity-records/sample.png", null);
    }

    private com.detoxmate.group.domain.GroupChallengeParticipant nullSafeParticipant() {
        return com.detoxmate.group.domain.GroupChallengeParticipant.join(1L, 2L);
    }

    private UserUsageGoalTime userUsageGoalTime(UsageGoalTypeCode usageGoalTypeCode, int goalMinutes) {
        UsageGoalType usageGoalType = UsageGoalType.create(usageGoalTypeCode.ordinal() + 1L, usageGoalTypeCode);
        return UserUsageGoalTime.create(User.createNew("tester"), usageGoalType, goalMinutes);
    }
}
