package com.weekly.controller;

import com.weekly.entity.DailyLog;
import com.weekly.entity.PromptType;
import com.weekly.entity.User;
import com.weekly.service.DailyLogService;
import com.weekly.service.PromptTemplateService;
import com.weekly.service.ReportService;
import com.weekly.service.UserService;
import com.weekly.entity.Report;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/daily")
public class DailyLogController {

    private final DailyLogService dailyLogService;
    private final UserService userService;
    private final ReportService reportService;
    private final PromptTemplateService promptTemplateService;

    public DailyLogController(DailyLogService dailyLogService, UserService userService,
                              ReportService reportService,
                              PromptTemplateService promptTemplateService) {
        this.dailyLogService = dailyLogService;
        this.userService = userService;
        this.reportService = reportService;
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public String dailyPage(@RequestParam(required = false) Long userId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                            HttpSession session, Model model) {
        // Resolve user
        if (userId == null) {
            Long sessionUserId = (Long) session.getAttribute("currentUserId");
            if (sessionUserId != null) userId = sessionUserId;
        }

        // Resolve week monday
        LocalDate weekMonday;
        if (week != null) {
            weekMonday = week.with(DayOfWeek.MONDAY);
        } else {
            weekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        }

        LinkedHashMap<LocalDate, DailyLog> weekLogs = null;
        if (userId != null) {
            weekLogs = dailyLogService.getWeekLogMap(userId, weekMonday);
        }

        model.addAttribute("users", userService.findAll());
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("weekMonday", weekMonday);
        model.addAttribute("weekFriday", weekMonday.plusDays(4));
        model.addAttribute("prevWeek", weekMonday.minusWeeks(1));
        model.addAttribute("nextWeek", weekMonday.plusWeeks(1));
        model.addAttribute("weekLogs", weekLogs);
        model.addAttribute("generalTemplates", promptTemplateService.findByType(PromptType.GENERAL));
        return "daily-log";
    }

    @PostMapping("/save")
    public String saveDayLog(@RequestParam Long userId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                             @RequestParam String content,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        User user = userService.findById(userId);
        session.setAttribute("currentUserId", userId);
        dailyLogService.save(user, logDate, content);
        redirectAttrs.addFlashAttribute("message", logDate.getMonthValue() + "/" + logDate.getDayOfMonth() + " 저장 완료");
        return "redirect:/daily?userId=" + userId + "&week=" + week;
    }

    @PostMapping("/generate")
    public String generateFromDaily(@RequestParam Long userId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                                    @RequestParam(required = false) Long generalTemplateId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttrs) {
        User user = userService.findById(userId);
        session.setAttribute("currentUserId", userId);

        LocalDate weekMonday = week.with(DayOfWeek.MONDAY);
        String combined = dailyLogService.combineWeekLogs(userId, weekMonday);

        if (combined.isBlank()) {
            redirectAttrs.addFlashAttribute("error", "작성된 일별 기록이 없습니다.");
            return "redirect:/daily?userId=" + userId + "&week=" + week;
        }

        try {
            Report report = reportService.generateAndSave(user, combined, weekMonday, weekMonday.plusDays(4),
                    generalTemplateId);
            redirectAttrs.addFlashAttribute("justCreated", true);
            return "redirect:/reports/" + report.getId();
        } catch (ReportService.InvalidReportException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/daily?userId=" + userId + "&week=" + week;
        }
    }
}
