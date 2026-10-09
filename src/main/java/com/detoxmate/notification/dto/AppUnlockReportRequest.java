package com.detoxmate.notification.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AppUnlockReportRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 0) @DecimalMax("2147483647")
        BigDecimal unlockMinutes,
        @NotNull Boolean limitExceeded
) {
}
