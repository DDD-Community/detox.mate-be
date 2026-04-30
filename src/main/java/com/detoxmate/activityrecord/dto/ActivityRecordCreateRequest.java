package com.detoxmate.activityrecord.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ActivityRecordCreateRequest(
        String activityImageObjectKey,
        String reflectionText,
        Long groupChallengeParticipantId,

        @NotEmpty
        List<@Valid ActivityRecordDetailRequest> details
) {
}
