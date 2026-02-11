package com.weekly.controller;

import com.weekly.entity.ChatMessage;
import com.weekly.entity.Report;
import com.weekly.entity.User;
import com.weekly.repository.ChatMessageRepository;
import com.weekly.service.ReportService;
import com.weekly.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final ReportService reportService;

    public ChatController(ChatMessageRepository chatMessageRepository,
                          UserService userService,
                          ReportService reportService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.reportService = reportService;
    }

    @GetMapping
    public String chatPage(Model model, HttpSession session) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("teams", userService.findAllTeams());
        model.addAttribute("currentUserId", session.getAttribute("currentUserId"));

        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId != null) {
            List<ChatMessage> messages = chatMessageRepository.findByUserIdAndUsedFalseOrderByCreatedAtAsc(userId);
            model.addAttribute("messages", messages);
        }
        return "chat";
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> send(@RequestBody Map<String, String> body,
                                                     HttpSession session) {
        Long userId = resolveUserId(body, session);
        String content = body.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "내용을 입력해주세요."));
        }

        User user = userService.findById(userId);
        ChatMessage msg = new ChatMessage();
        msg.setUser(user);
        msg.setContent(content.trim());
        msg.setUsed(false);
        chatMessageRepository.save(msg);

        session.setAttribute("currentUserId", userId);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return ResponseEntity.ok(Map.of(
                "id", msg.getId(),
                "content", msg.getContent(),
                "time", msg.getCreatedAt().format(fmt)
        ));
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        chatMessageRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/clear")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> clear(HttpSession session) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId != null) {
            chatMessageRepository.deleteByUserIdAndUsedFalse(userId);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/generate")
    @Transactional
    public String generate(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
                           HttpSession session,
                           RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            ra.addFlashAttribute("error", "사용자를 선택해주세요.");
            return "redirect:/chat";
        }

        List<ChatMessage> messages = chatMessageRepository.findByUserIdAndUsedFalseOrderByCreatedAtAsc(userId);
        if (messages.isEmpty()) {
            ra.addFlashAttribute("error", "입력된 메시지가 없습니다.");
            return "redirect:/chat";
        }

        String rawText = messages.stream()
                .map(ChatMessage::getContent)
                .collect(Collectors.joining("\n"));

        User user = userService.findById(userId);
        userService.updateLastActive(user);

        if (periodStart == null) {
            periodStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }
        if (periodEnd == null) {
            periodEnd = periodStart.plusDays(4);
        }

        try {
            Report report = reportService.generateAndSave(user, rawText, periodStart, periodEnd);

            for (ChatMessage msg : messages) {
                msg.setUsed(true);
                chatMessageRepository.save(msg);
            }

            return "redirect:/reports/" + report.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/chat";
        }
    }

    private Long resolveUserId(Map<String, String> body, HttpSession session) {
        String userIdStr = body.get("userId");
        if (userIdStr != null && !userIdStr.isBlank()) {
            return Long.parseLong(userIdStr);
        }
        Long sessionUserId = (Long) session.getAttribute("currentUserId");
        if (sessionUserId != null) {
            return sessionUserId;
        }
        throw new IllegalArgumentException("사용자를 선택해주세요.");
    }
}
