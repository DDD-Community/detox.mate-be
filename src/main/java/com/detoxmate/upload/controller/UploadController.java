package com.detoxmate.upload.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/uploads/presigned-urls")
    public PresignedUrlResponse issuePresignedUrl(
            CurrentUser currentUser,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return uploadService.issuePresignedUrl(currentUser.id(), request);
    }
}
