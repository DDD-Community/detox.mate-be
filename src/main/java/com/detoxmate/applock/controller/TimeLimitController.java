package com.detoxmate.applock.controller;

import com.detoxmate.applock.dto.TimeLimitRequest;
import com.detoxmate.applock.dto.TimeLimitResponse;
import com.detoxmate.applock.service.TimeLimitService;
import com.detoxmate.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TimeLimitController {

    private final TimeLimitService timeLimitService;

    @PutMapping("/me/time-limit")
    public TimeLimitResponse set(CurrentUser currentUser, @Valid @RequestBody TimeLimitRequest request) {
        return new TimeLimitResponse(timeLimitService.set(currentUser.id(), request.totalLockMinutes()));
    }

    @GetMapping("/me/time-limit")
    public TimeLimitResponse get(CurrentUser currentUser) {
        return new TimeLimitResponse(timeLimitService.get(currentUser.id()));
    }

    @DeleteMapping("/me/time-limit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser currentUser) {
        timeLimitService.delete(currentUser.id());
    }
}
