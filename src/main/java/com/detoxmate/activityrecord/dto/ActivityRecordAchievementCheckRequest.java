package com.detoxmate.activityrecord.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ActivityRecordAchievementCheckRequest(
        @NotEmpty
        List<@Valid ActivityRecordDetailRequest> details
) {
}
