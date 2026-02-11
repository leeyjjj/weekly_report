package com.weekly.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_user_id", columnList = "user_id"),
        @Index(name = "idx_report_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawInput;

    @Column(columnDefinition = "LONGTEXT")
    private String generalReportMd;

    @Column(nullable = false)
    private boolean teamsSent;

    @Column(length = 500)
    private String teamsError;

    @Column(length = 500)
    private String savedFilePath;

    @Column(length = 300)
    private String summary;

    @Column(length = 100)
    private String llmProvider;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
