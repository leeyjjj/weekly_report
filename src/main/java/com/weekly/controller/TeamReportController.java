package com.weekly.controller;

import com.weekly.entity.PromptType;
import com.weekly.entity.Report;
import com.weekly.entity.Team;
import com.weekly.entity.TeamReport;
import com.weekly.repository.TeamReportRepository;
import com.weekly.service.LlmService;
import com.weekly.service.PdfExportService;
import com.weekly.service.PromptTemplateService;
import com.weekly.service.ReportService;
import com.weekly.service.UserService;
import com.weekly.util.MarkdownRenderer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/team-report")
public class TeamReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final LlmService llmService;
    private final PromptTemplateService promptTemplateService;
    private final MarkdownRenderer markdownRenderer;
    private final TeamReportRepository teamReportRepository;
    private final PdfExportService pdfExportService;

    public TeamReportController(ReportService reportService, UserService userService,
                                 LlmService llmService, PromptTemplateService promptTemplateService,
                                 MarkdownRenderer markdownRenderer, TeamReportRepository teamReportRepository,
                                 PdfExportService pdfExportService) {
        this.reportService = reportService;
        this.userService = userService;
        this.llmService = llmService;
        this.promptTemplateService = promptTemplateService;
        this.markdownRenderer = markdownRenderer;
        this.teamReportRepository = teamReportRepository;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("teams", userService.findAllTeams());
        model.addAttribute("teamReports", teamReportRepository.findAllByOrderByCreatedAtDesc());
        return "team-report";
    }

    @PostMapping("/generate")
    public String generateTeamReport(@RequestParam Long teamId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                      RedirectAttributes ra) {

        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atTime(LocalTime.MAX);

        List<Report> reports = reportService.findAllFiltered(null, teamId, start, end, null);

        if (reports.isEmpty()) {
            ra.addFlashAttribute("error", "해당 기간에 팀 보고서가 없습니다.");
            return "redirect:/team-report";
        }

        StringBuilder combined = new StringBuilder();
        for (Report r : reports) {
            combined.append("## ").append(r.getUser().getName()).append("의 업무\n");
            combined.append(r.getRawInput()).append("\n\n");
        }

        String teamPrompt = promptTemplateService.getPrompt(PromptType.TEAM_AGGREGATE);
        String aggregatedMd = llmService.generate(teamPrompt, combined.toString());

        Team team = reports.getFirst().getUser().getTeam();
        if (team == null) {
            ra.addFlashAttribute("error", "보고서 작성자에게 팀이 지정되어 있지 않습니다.");
            return "redirect:/team-report";
        }

        TeamReport teamReport = new TeamReport();
        teamReport.setTeam(team);
        teamReport.setWeekStart(weekStart);
        teamReport.setReportCount(reports.size());
        teamReport.setAggregatedMd(aggregatedMd);
        teamReportRepository.save(teamReport);

        return "redirect:/team-report/" + teamReport.getId();
    }

    @GetMapping("/{id}")
    public String viewTeamReport(@PathVariable Long id, Model model) {
        TeamReport teamReport = teamReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀 통합보고를 찾을 수 없습니다: " + id));

        model.addAttribute("teamReport", teamReport);
        model.addAttribute("teamName", teamReport.getTeam().getName());
        model.addAttribute("weekStart", teamReport.getWeekStart());
        model.addAttribute("reportCount", teamReport.getReportCount());
        model.addAttribute("aggregatedMd", teamReport.getAggregatedMd());
        model.addAttribute("aggregatedHtml", markdownRenderer.toHtml(teamReport.getAggregatedMd()));
        return "team-report-result";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws java.io.IOException {
        TeamReport teamReport = teamReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀 통합보고를 찾을 수 없습니다: " + id));
        byte[] pdf = pdfExportService.exportTeamReport(teamReport);
        String filename = "team_report_" + teamReport.getTeam().getName() + "_" + teamReport.getId() + ".pdf";
        String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/delete")
    public String deleteTeamReport(@PathVariable Long id, RedirectAttributes ra) {
        teamReportRepository.deleteById(id);
        ra.addFlashAttribute("message", "팀 통합보고가 삭제되었습니다.");
        return "redirect:/team-report";
    }
}
