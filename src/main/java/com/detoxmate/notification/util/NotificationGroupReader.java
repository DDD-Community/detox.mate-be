package com.detoxmate.notification.util;

import com.detoxmate.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationGroupReader {

    private final GroupRepository groupRepository;

    public String findGroupName(Long groupId){
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."))
                .getName();
    }
}
