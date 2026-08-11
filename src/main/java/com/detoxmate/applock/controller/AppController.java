package com.detoxmate.applock.controller;

import com.detoxmate.applock.dto.AppCreateRequest;
import com.detoxmate.applock.dto.AppListResponse;
import com.detoxmate.applock.dto.AppResponse;
import com.detoxmate.applock.dto.AppUpdateRequest;
import com.detoxmate.applock.service.AppService;
import com.detoxmate.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping("/me/apps")
    @ResponseStatus(HttpStatus.CREATED)
    public AppResponse create(CurrentUser currentUser, @Valid @RequestBody AppCreateRequest request) {
        return appService.create(currentUser.id(), request);
    }

    @GetMapping("/me/apps")
    public AppListResponse getAll(CurrentUser currentUser) {
        return appService.getAll(currentUser.id());
    }

    @GetMapping("/me/apps/{appId}")
    public AppResponse get(CurrentUser currentUser, @PathVariable Long appId) {
        return appService.get(currentUser.id(), appId);
    }

    @PutMapping("/me/apps/{appId}")
    public AppResponse update(
            CurrentUser currentUser,
            @PathVariable Long appId,
            @Valid @RequestBody AppUpdateRequest request
    ) {
        return appService.update(currentUser.id(), appId, request);
    }

    @DeleteMapping("/me/apps/{appId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser currentUser, @PathVariable Long appId) {
        appService.delete(currentUser.id(), appId);
    }
}
