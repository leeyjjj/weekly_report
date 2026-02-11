package com.weekly.repository;

import com.weekly.entity.DailyLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    @EntityGraph(attributePaths = {"user", "user.team"})
    List<DailyLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(Long userId, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"user"})
    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);
}
