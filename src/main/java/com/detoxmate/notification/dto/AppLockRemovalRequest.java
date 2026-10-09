package com.detoxmate.notification.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AppLockRemovalRequest(
        @NotNull @Size(max = 3) List<@NotNull @Positive Long> recipientUserIds
) {
}
