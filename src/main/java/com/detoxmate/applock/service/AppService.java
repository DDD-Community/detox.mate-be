package com.detoxmate.applock.service;

import com.detoxmate.applock.domain.App;
import com.detoxmate.applock.dto.AppCreateRequest;
import com.detoxmate.applock.dto.AppListResponse;
import com.detoxmate.applock.dto.AppResponse;
import com.detoxmate.applock.dto.AppUpdateRequest;
import com.detoxmate.applock.repository.AppRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AppService {

    private static final String APP_NOT_FOUND_MESSAGE = "앱을 찾을 수 없습니다.";
    private static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    private static final String USER_MISMATCH_MESSAGE = "요청 사용자와 인증 사용자가 일치하지 않습니다.";

    private final AppRepository appRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppResponse create(Long currentUserId, AppCreateRequest request) {
        validateRequestUser(currentUserId, request.userId());
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
        App app = App.create(user, request.appDisplayName(), request.dailyLimitMinutes());

        return AppResponse.from(appRepository.save(app));
    }

    @Transactional(readOnly = true)
    public AppListResponse getAll(Long currentUserId) {
        return new AppListResponse(appRepository.findAllByUser_Id(currentUserId).stream()
                .map(AppResponse::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public AppResponse get(Long currentUserId, Long appId) {
        return AppResponse.from(findOwnedApp(currentUserId, appId));
    }

    @Transactional
    public AppResponse update(Long currentUserId, Long appId, AppUpdateRequest request) {
        validateRequestUser(currentUserId, request.userId());
        App app = findOwnedApp(currentUserId, appId);
        app.update(request.appDisplayName(), request.dailyLimitMinutes());

        return AppResponse.from(app);
    }

    @Transactional
    public void delete(Long currentUserId, Long appId) {
        appRepository.delete(findOwnedApp(currentUserId, appId));
    }

    private App findOwnedApp(Long currentUserId, Long appId) {
        return appRepository.findByIdAndUser_Id(appId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, APP_NOT_FOUND_MESSAGE));
    }

    private void validateRequestUser(Long currentUserId, Long requestUserId) {
        if (!Objects.equals(currentUserId, requestUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, USER_MISMATCH_MESSAGE);
        }
    }
}
