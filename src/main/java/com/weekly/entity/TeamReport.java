package com.weekly.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_reports", indexes = {
        @Index(name = "idx_team_report_team", columnList = "team_id"),
        @Index(name = "idx_team_report_week", columnList = "weekStart")
})
@Getter
@Setter
@NoArgsConstructor
public class TeamReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private int reportCount;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String aggregatedMd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
