package com.detoxmate.poke.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.poke.service.PokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-challenges")
@Slf4j
public class PokeController {

    private final PokeService pokeService;

    @PostMapping("/{groupChallengeId}/activity-records/{activityRecordId}/members/{targetUserId}/poke")
    public ResponseEntity<Void> pokeUser(@PathVariable Long groupChallengeId,
                                         @PathVariable Long activityRecordId,
                                         @PathVariable Long targetUserId,
                                         CurrentUser currentUser) {

        log.info(
                "[Poke][create-poke] senderUserId={} poked receiverUserId={} in groupChallengeId={}, activityRecordId={}",
                currentUser.id(), targetUserId, groupChallengeId, activityRecordId
        );
        pokeService.poke(groupChallengeId, activityRecordId, targetUserId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
