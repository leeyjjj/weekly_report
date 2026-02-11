package com.weekly.controller;

import com.weekly.entity.PromptType;
import com.weekly.entity.Report;
import com.weekly.entity.User;
import com.weekly.model.ReportRequest;
import com.weekly.service.PdfExportService;
import com.weekly.service.PromptTemplateService;
import com.weekly.service.ReportService;
import com.weekly.service.UserService;
import com.weekly.util.MarkdownRenderer;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final PromptTemplateService promptTemplateService;
    private final MarkdownRenderer markdownRenderer;
    private final PdfExportService pdfExportService;

    public ReportController(ReportService reportService, UserService userService,
                            PromptTemplateService promptTemplateService,
                            MarkdownRenderer markdownRenderer, PdfExportService pdfExportService) {
        this.reportService = reportService;
        this.userService = userService;
        this.promptTemplateService = promptTemplateService;
        this.markdownRenderer = markdownRenderer;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("reportRequest", new ReportRequest());
        populateFormModel(model);
        return "index";
    }

    private void populateFormModel(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("teams", userService.findAllTeams());
        model.addAttribute("generalTemplates", promptTemplateService.findByType(PromptType.GENERAL));
    }

    @PostMapping("/generate")
    public String generateReport(@Valid @ModelAttribute ReportRequest reportRequest,
                                 BindingResult bindingResult,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model);
            return "index";
        }

        User user;
        try {
            user = resolveUser(reportRequest);
        } catch (IllegalArgumentException e) {
            populateFormModel(model);
            model.addAttribute("formError", e.getMessage());
            return "index";
        }

        try {
            userService.updateLastActive(user);
            session.setAttribute("currentUserId", user.getId());
            Report report = reportService.generateAndSave(user, reportRequest.getRawText(),
                    reportRequest.getPeriodStart(), reportRequest.getPeriodEnd(),
                    reportRequest.getGeneralTemplateId());
            redirectAttrs.addFlashAttribute("justCreated", true);
            return "redirect:/reports/" + report.getId();
        } catch (ReportService.InvalidReportException e) {
            populateFormModel(model);
            model.addAttribute("formError", e.getMessage());
            return "index";
        }
    }

    @GetMapping("/reports/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        Report report = reportService.findById(id);
        model.addAttribute("report", report);
        model.addAttribute("generalHtml", markdownRenderer.toHtml(report.getGeneralReportMd()));
        model.addAttribute("generalMd", report.getGeneralReportMd());
        return "report-detail";
    }

    @GetMapping("/reports/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws java.io.IOException {
        Report report = reportService.findById(id);
        byte[] pdf = pdfExportService.exportReport(report);
        String filename = "report_" + report.getUser().getName() + "_" + report.getId() + ".pdf";
        String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/reports/{id}/edit")
    public String editReportForm(@PathVariable Long id, Model model) {
        Report report = reportService.findById(id);
        model.addAttribute("report", report);
        return "report-edit";
    }

    @PostMapping("/reports/{id}/edit")
    public String updateReport(@PathVariable Long id,
                               @RequestParam String editMode,
                               @RequestParam(required = false) String rawInput,
                               @RequestParam(required = false) String generalReportMd) {
        if ("regenerate".equals(editMode)) {
            reportService.regenerate(id, rawInput);
        } else {
            reportService.updateManually(id, generalReportMd);
        }
        return "redirect:/reports/" + id;
    }

    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id, @RequestParam Long userId) {
        reportService.deleteReport(id);
        return "redirect:/my-reports?userId=" + userId;
    }

    @PostMapping("/reports/{id}/resend-teams")
    public String resendTeams(@PathVariable Long id) {
        reportService.resendToTeams(id);
        return "redirect:/reports/" + id;
    }

    @GetMapping("/my-reports")
    public String myReports(@RequestParam Long userId,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        User user = userService.findById(userId);
        Page<Report> reports = reportService.findByUser(userId, PageRequest.of(page, 20));
        model.addAttribute("user", user);
        model.addAttribute("reports", reports);
        return "my-reports";
    }

    @GetMapping("/reports/{id}/copy")
    public String copyReport(@PathVariable Long id, Model model) {
        Report source = reportService.findById(id);
        ReportRequest req = new ReportRequest();
        req.setUserId(source.getUser().getId());
        req.setRawText(source.getRawInput());
        model.addAttribute("reportRequest", req);
        populateFormModel(model);
        model.addAttribute("copyFrom", true);
        return "index";
    }

    private User resolveUser(ReportRequest req) {
        if (req.getUserId() != null && req.getUserId() > 0) {
            return userService.findById(req.getUserId());
        }
        if (req.getUserName() != null && !req.getUserName().isBlank()) {
            Long teamId = resolveTeamId(req);
            return userService.findOrCreate(req.getUserName(), teamId);
        }
        throw new IllegalArgumentException("사용자를 선택하거나 이름을 입력해주세요");
    }

    private Long resolveTeamId(ReportRequest req) {
        if (req.getNewTeamName() != null && !req.getNewTeamName().isBlank()) {
            return userService.findOrCreateTeam(req.getNewTeamName()).getId();
        }
        return req.getTeamId();
    }
}
