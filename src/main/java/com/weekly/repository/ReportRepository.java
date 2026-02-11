package com.weekly.repository;

import com.weekly.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    @EntityGraph(attributePaths = {"user", "user.team"})
    Optional<Report> findById(Long id);

    @EntityGraph(attributePaths = {"user", "user.team"})
    Page<Report> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<Report> findByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    Page<Report> findByDateRange(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    Page<Report> findByUserIdAndDateRange(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    @Query("SELECT r FROM Report r WHERE r.user.team.id = :teamId ORDER BY r.createdAt DESC")
    Page<Report> findByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.team"})
    @Query("SELECT r FROM Report r WHERE r.user.team.id = :teamId AND r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    Page<Report> findByTeamIdAndDateRange(@Param("teamId") Long teamId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           Pageable pageable);

    long countByUserId(Long userId);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByTeamsSentTrue();

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM Report r")
    long countDistinctUsers();
}
