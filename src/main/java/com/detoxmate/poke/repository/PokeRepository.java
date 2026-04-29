package com.detoxmate.poke.repository;

import com.detoxmate.poke.domain.Poke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PokeRepository extends JpaRepository<Poke, Long> {
}
