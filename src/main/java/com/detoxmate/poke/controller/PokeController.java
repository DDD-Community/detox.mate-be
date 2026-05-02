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
@RequestMapping("/challenge-records")
@Slf4j
public class PokeController {

    private final PokeService pokeService;

    @PostMapping("/{challengeRecordId}/pokes/{receiverUserId}")
    public ResponseEntity<Void> pokeUser(@PathVariable Long challengeRecordId,
                                         @PathVariable Long receiverUserId,
                                         CurrentUser currentUser) {
        log.info(
                "[Poke][create-poke] senderUserId={} poked receiverUserId={} in challengeRecordId={}",
                currentUser.id(), receiverUserId, challengeRecordId);

        pokeService.poke(challengeRecordId, receiverUserId, currentUser.id());

        return ResponseEntity.noContent().build();
    }
}
