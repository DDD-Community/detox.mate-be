package com.detoxmate.user.dto;

import java.util.List;

public record MyProfileResponse(
        Long id,
        String displayName,
        List<String> providers
) {
}
