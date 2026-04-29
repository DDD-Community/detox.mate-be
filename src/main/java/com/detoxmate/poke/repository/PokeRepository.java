package com.detoxmate.poke.repository;

import com.detoxmate.poke.domain.Poke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PokeRepository extends JpaRepository<Poke, Long> {

    @Query("""
        select count(p) > 0
        from Poke p
        where p.groupChallengeId = :groupChallengeId
          and p.activityRecordId = :activityRecordId
          and p.senderUserId = :senderUserId
          and p.receiverUserId = :receiverUserId
          and p.pokeDate = :pokeDate
        """)
    boolean existsPoke(Long groupChallengeId, Long activityRecordId, Long senderUserId, Long receiverUserId, LocalDate pokeDate);
}
