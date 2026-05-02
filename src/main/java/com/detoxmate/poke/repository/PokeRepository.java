package com.detoxmate.poke.repository;

import com.detoxmate.poke.domain.Poke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PokeRepository extends JpaRepository<Poke, Long> {

    @Query("""
        select count(p) > 0
        from Poke p
        where p.challengeRecordId = :challengeRecordId
          and p.senderUserId = :senderUserId
          and p.receiverUserId = :receiverUserId
        """)
    boolean existsPoke(Long challengeRecordId, Long senderUserId, Long receiverUserId);

    @Query("""
    select p
    from Poke p
    where p.challengeRecordId = :challengeRecordId
    order by p.pokeDate desc, p.id desc
    """)
    List<Poke> findAllByChallengeRecordOrderByLatest(Long challengeRecordId);


}
