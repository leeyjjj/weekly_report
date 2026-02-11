package com.weekly.model;

import com.weekly.entity.Report;

import java.time.LocalDate;
import java.util.List;

public record WeekGroup(int year, int weekNumber, LocalDate weekStart, LocalDate weekEnd, List<Report> reports) {
}
