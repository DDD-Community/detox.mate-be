package com.detoxmate.transferminute.repository;

import com.detoxmate.transferminute.domain.TransferPhrase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TransferPhraseRepository extends JpaRepository<TransferPhrase, Long> {

    @Query(value = "SELECT id, transfer_phrase FROM transfer_phrase ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<TransferPhrase> findRandom();
}
