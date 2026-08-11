package com.detoxmate.applock.repository;

import com.detoxmate.applock.domain.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppRepository extends JpaRepository<App, Long> {

    List<App> findAllByUser_Id(Long userId);

    Optional<App> findByIdAndUser_Id(Long id, Long userId);
}
