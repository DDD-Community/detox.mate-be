package com.detoxmate.user.dto;

import com.detoxmate.common.validation.NullOrNotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Setter;

public final class UpdateMyProfileRequest {

    @Pattern(regexp = "(?U)\\S{1,5}", message = "프로필 이름은 공백 없이 1~5자여야 합니다.")
    @Setter
    private String displayName;

    @Size(max = 1024)
    @NullOrNotBlank
    private String profileImageObjectKey;

    private boolean profileImageObjectKeyPresent;

    public String displayName() {
        return displayName;
    }

    public String profileImageObjectKey() {
        return profileImageObjectKey;
    }

    public boolean hasProfileImageObjectKey() {
        return profileImageObjectKeyPresent;
    }

    public void setProfileImageObjectKey(String profileImageObjectKey) {
        this.profileImageObjectKey = profileImageObjectKey;
        this.profileImageObjectKeyPresent = true;
    }
}
