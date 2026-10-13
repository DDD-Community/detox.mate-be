package com.detoxmate.transferminute.service;

import com.detoxmate.transferminute.domain.TransferPhrase;
import com.detoxmate.transferminute.repository.TransferPhraseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TransferPhraseService {

    private final TransferPhraseRepository transferPhraseRepository;

    @Transactional(readOnly = true)
    public String getTransferPhrase() {
        return transferPhraseRepository.findRandom()
                .map(TransferPhrase::getTransferPhrase)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
