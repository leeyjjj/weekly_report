package com.weekly.repository;

import com.weekly.entity.TeamReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamReportRepository extends JpaRepository<TeamReport, Long> {

    @EntityGraph(attributePaths = {"team"})
    Optional<TeamReport> findById(Long id);

    @EntityGraph(attributePaths = {"team"})
    List<TeamReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"team"})
    List<TeamReport> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
