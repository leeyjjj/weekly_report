package com.weekly.controller;

import com.weekly.entity.Report;
import com.weekly.repository.ReportRepository;
import com.weekly.service.ExcelExportService;
import com.weekly.service.ReportService;
import com.weekly.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.weekly.model.WeekGroup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Controller
public class DashboardController {

    private final ReportService reportService;
    private final UserService userService;
    private final ReportRepository reportRepository;
    private final ExcelExportService excelExportService;

    public DashboardController(ReportService reportService, UserService userService,
                               ReportRepository reportRepository, ExcelExportService excelExportService) {
        this.reportService = reportService;
        this.userService = userService;
        this.reportRepository = reportRepository;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long userId,
                            @RequestParam(required = false) Long teamId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<Report> reports = reportService.findFiltered(userId, teamId, start, end, keyword, PageRequest.of(page, 20));

        // Stats
        long totalReports = reportRepository.count();
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        long thisWeekReports = reportRepository.countByCreatedAtAfter(weekStart);
        long activeUsers = reportRepository.countDistinctUsers();
        long teamsSentCount = reportRepository.countByTeamsSentTrue();

        model.addAttribute("teams", userService.findAllTeams());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("reports", reports);
        model.addAttribute("selectedTeamId", teamId);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("keyword", keyword);

        // Week grouping
        WeekFields wf = WeekFields.ISO;
        Map<String, List<Report>> grouped = new LinkedHashMap<>();
        for (Report r : reports.getContent()) {
            LocalDate d = r.getCreatedAt().toLocalDate();
            int year = d.get(wf.weekBasedYear());
            int week = d.get(wf.weekOfWeekBasedYear());
            grouped.computeIfAbsent(year + "-" + week, k -> new ArrayList<>()).add(r);
        }
        List<WeekGroup> weekGroups = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Report first = entry.getValue().getFirst();
            LocalDate d = first.getCreatedAt().toLocalDate();
            int year = d.get(wf.weekBasedYear());
            int week = d.get(wf.weekOfWeekBasedYear());
            LocalDate ws = d.with(DayOfWeek.MONDAY);
            LocalDate we = d.with(DayOfWeek.SUNDAY);
            weekGroups.add(new WeekGroup(year, week, ws, we, entry.getValue()));
        }
        model.addAttribute("weekGroups", weekGroups);

        model.addAttribute("totalReports", totalReports);
        model.addAttribute("thisWeekReports", thisWeekReports);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("teamsSentCount", teamsSentCount);
        return "dashboard";
    }

    @GetMapping("/dashboard/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) throws java.io.IOException {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<Report> reports = reportService.findAllFiltered(userId, teamId, start, end, keyword);
        byte[] excel = excelExportService.exportReports(reports);
        String filename = "weekly_reports_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
