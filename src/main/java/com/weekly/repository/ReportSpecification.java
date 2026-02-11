package com.weekly.repository;

import com.weekly.entity.Report;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportSpecification {

    public static Specification<Report> withFilters(Long userId, Long teamId,
                                                     LocalDateTime start, LocalDateTime end,
                                                     String keyword) {
        return (root, query, cb) -> {
            // Eager fetch user and team (only for SELECT, not COUNT)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", JoinType.LEFT).fetch("team", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (teamId != null) {
                predicates.add(cb.equal(root.get("user").get("team").get("id"), teamId));
            }
            if (start != null && end != null) {
                predicates.add(cb.between(root.get("createdAt"), start, end));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("generalReportMd")), pattern),
                        cb.like(cb.lower(root.get("rawInput")), pattern)
                ));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
