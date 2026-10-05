package com.detoxmate.transferminute.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.transferminute.dto.TransferPhraseResponse;
import com.detoxmate.transferminute.service.TransferPhraseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransferPhraseController {

    private final TransferPhraseService transferPhraseService;

    @GetMapping("/transfer-phrases/random")
    public ResponseEntity<TransferPhraseResponse> getRandom(CurrentUser currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TransferPhraseResponse(transferPhraseService.getTransferPhrase()));
    }
}
