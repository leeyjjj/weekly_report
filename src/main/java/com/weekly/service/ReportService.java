package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.entity.PromptType;
import com.weekly.entity.Report;
import com.weekly.entity.User;
import com.weekly.repository.ReportRepository;
import com.weekly.repository.ReportSpecification;
import com.weekly.util.MarkdownRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final LlmService llmService;
    private final TeamsWebhookService teamsService;
    private final ReportRepository reportRepository;
    private final MarkdownRenderer markdownRenderer;
    private final PromptTemplateService promptTemplateService;
    private final Path outputDirectory;

    public ReportService(LlmService llmService, TeamsWebhookService teamsService,
                         ReportRepository reportRepository, MarkdownRenderer markdownRenderer,
                         PromptTemplateService promptTemplateService, AppConfig appConfig) {
        this.llmService = llmService;
        this.teamsService = teamsService;
        this.reportRepository = reportRepository;
        this.markdownRenderer = markdownRenderer;
        this.promptTemplateService = promptTemplateService;
        this.outputDirectory = Path.of(appConfig.output().directory());
    }

    public MarkdownRenderer getMarkdownRenderer() {
        return markdownRenderer;
    }

    public String getLlmProviderName() {
        return llmService.getProviderName();
    }

    @Transactional
    public Report generateAndSave(User user, String rawText, LocalDate periodStart, LocalDate periodEnd) {
        return generateAndSave(user, rawText, periodStart, periodEnd, null);
    }

    @Transactional
    public Report generateAndSave(User user, String rawText, LocalDate periodStart, LocalDate periodEnd,
                                  Long generalTemplateId) {
        log.info("Generating report for user '{}' from input ({} chars)", user.getName(), rawText.length());

        String generalReportMd = generateReport(rawText, generalTemplateId);

        boolean teamsSent = false;
        String teamsError = null;
        if (teamsService.isEnabled()) {
            try {
                teamsService.sendReport(generalReportMd);
                teamsSent = true;
            } catch (Exception e) {
                log.error("Failed to send to Teams", e);
                teamsError = e.getMessage();
            }
        }

        String savedFilePath = null;
        try {
            savedFilePath = saveToFile(user.getName(), generalReportMd);
        } catch (IOException e) {
            log.error("Failed to save report to file", e);
        }

        Report report = new Report();
        report.setUser(user);
        report.setRawInput(rawText);
        report.setGeneralReportMd(generalReportMd);
        report.setTeamsSent(teamsSent);
        report.setTeamsError(teamsError);
        report.setSavedFilePath(savedFilePath);
        report.setLlmProvider(llmService.getProviderName());
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        reportRepository.save(report);

        try {
            String summaryPrompt = "다음 주간업무보고를 간단히 한 줄로 요약해주세요. 요약만 출력하세요.";
            String summary = llmService.generate(summaryPrompt, generalReportMd);
            if (summary != null && !summary.isBlank()) {
                report.setSummary(summary.trim().length() > 300 ? summary.trim().substring(0, 300) : summary.trim());
                reportRepository.save(report);
            }
        } catch (Exception e) {
            log.warn("Failed to generate summary", e);
        }

        return report;
    }

    @Transactional
    public Report save(Report report) {
        return reportRepository.save(report);
    }

    public Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다: " + id));
    }

    public Page<Report> findByUser(Long userId, Pageable pageable) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<Report> findAll(Pageable pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Report> findFiltered(Long userId, Long teamId, LocalDateTime start, LocalDateTime end,
                                      String keyword, Pageable pageable) {
        return reportRepository.findAll(
                ReportSpecification.withFilters(userId, teamId, start, end, keyword), pageable);
    }

    public java.util.List<Report> findAllFiltered(Long userId, Long teamId, LocalDateTime start, LocalDateTime end,
                                                   String keyword) {
        return reportRepository.findAll(
                ReportSpecification.withFilters(userId, teamId, start, end, keyword));
    }

    @Transactional
    public Report regenerate(Long reportId, String newRawText) {
        Report report = findById(reportId);
        String generalReportMd = generateReport(newRawText, null);
        report.setRawInput(newRawText);
        report.setGeneralReportMd(generalReportMd);
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateManually(Long reportId, String generalMd) {
        Report report = findById(reportId);
        report.setGeneralReportMd(generalMd);
        return reportRepository.save(report);
    }

    @Transactional
    public Report resendToTeams(Long reportId) {
        Report report = findById(reportId);
        try {
            teamsService.sendReport(report.getGeneralReportMd());
            report.setTeamsSent(true);
            report.setTeamsError(null);
        } catch (Exception e) {
            report.setTeamsError(e.getMessage());
        }
        return reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Long reportId) {
        reportRepository.deleteById(reportId);
    }

    private String generateReport(String rawText, Long generalTemplateId) {
        String prompt = promptTemplateService.getPrompt(generalTemplateId, PromptType.GENERAL);

        log.info("Sending prompt to LLM");
        String response = llmService.generate(prompt, rawText);
        validateReportResponse(response);

        return response.trim();
    }

    private void validateReportResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new InvalidReportException("AI가 빈 응답을 반환했습니다. 업무 내용을 더 상세하게 입력해주세요.");
        }
        // 유효한 보고서에는 마크다운 헤더(###)가 포함되어야 함
        if (!response.contains("###")) {
            log.warn("AI response does not look like a valid report: {}", response.substring(0, Math.min(200, response.length())));
            throw new InvalidReportException("입력하신 내용으로 보고서를 생성할 수 없습니다. 이번 주 수행한 업무, 다음 주 계획, 이슈사항 등을 구체적으로 입력해주세요.");
        }
    }

    public static class InvalidReportException extends RuntimeException {
        public InvalidReportException(String message) {
            super(message);
        }
    }

    private String saveToFile(String userName, String generalReport) throws IOException {
        Files.createDirectories(outputDirectory);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path filePath = outputDirectory.resolve("weekly_report_" + userName + "_" + timestamp + ".md");

        String content = """
                # 주간업무보고 - %s (%s)

                ---

                %s
                """.formatted(userName, timestamp, generalReport);

        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        log.info("Report saved to file: {}", filePath.toAbsolutePath());
        return filePath.toAbsolutePath().toString();
    }
}
