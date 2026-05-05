package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadObjectKeyValidatorTest {

    private final UploadObjectKeyValidator uploadObjectKeyValidator = new UploadObjectKeyValidator();

    @Test
    void profile_image_object_key가_사용자_경로와_일치하면_유효하다() {
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.PROFILE_IMAGE,
                "profile-images/1/profile.png"
        )).isTrue();
    }

    @Test
    void profile_image_object_key가_다른_사용자_경로이면_유효하지_않다() {
        assertThat(uploadObjectKeyValidator.isOwnedBy(
                1L,
                UploadPurpose.PROFILE_IMAGE,
                "profile-images/2/profile.png"
        )).isFalse();
    }

    @Test
    void object_key가_비어있으면_유효하지_않다() {
        assertThat(uploadObjectKeyValidator.isOwnedBy(1L, UploadPurpose.PROFILE_IMAGE, " ")).isFalse();
    }
}
