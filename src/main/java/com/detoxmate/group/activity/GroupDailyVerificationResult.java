package com.detoxmate.group.activity;

public enum GroupDailyVerificationResult {
    ALL,
    HALF,
    RESET;

    public boolean success() {
        return this == ALL || this == HALF;
    }
}
