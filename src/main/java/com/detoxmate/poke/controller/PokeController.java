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

    @PostMapping("/{groupChallengeId}/members/{targetUserId}/poke")
    public ResponseEntity<Void> pokeUser(@PathVariable Long groupChallengeId,
                                         @PathVariable Long targetUserId,
                                         CurrentUser currentUser) {

        log.info("[Poke][create-poke] senderUserId={} poked receiverUserId={} in groupChallengeId={}", currentUser.id(), targetUserId, groupChallengeId);
        pokeService.poke(groupChallengeId, targetUserId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
