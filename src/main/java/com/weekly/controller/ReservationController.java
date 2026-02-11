package com.weekly.controller;

import com.weekly.entity.Reservation;
import com.weekly.entity.Room;
import com.weekly.service.ReservationService;
import com.weekly.service.RoomService;
import com.weekly.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final RoomService roomService;
    private final UserService userService;

    public ReservationController(ReservationService reservationService,
                                  RoomService roomService,
                                  UserService userService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping
    public String calendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                           Model model) {
        LocalDate weekStart = (week != null) ? week.with(DayOfWeek.MONDAY) : LocalDate.now().with(DayOfWeek.MONDAY);

        List<Room> rooms = roomService.findAllActive();
        Map<Long, Map<LocalDate, List<Reservation>>> calendar = reservationService.buildWeeklyCalendar(weekStart);

        List<LocalDate> weekDays = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            weekDays.add(weekStart.plusDays(i));
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("calendar", calendar);
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));

        return "reservations/calendar";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long roomId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam(required = false) Integer hour,
                          Model model) {
        model.addAttribute("rooms", roomService.findAllActive());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedHour", hour);
        return "reservations/form";
    }

    @PostMapping
    public String create(@RequestParam Long roomId,
                         @RequestParam Long userId,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                         @RequestParam(required = false) String attendees,
                         @RequestParam(required = false, defaultValue = "false") boolean recurring,
                         @RequestParam(required = false) String recurType,
                         @RequestParam(required = false, defaultValue = "1") int recurWeeks,
                         RedirectAttributes ra) {
        try {
            if (recurring && recurType != null) {
                List<Reservation> created = reservationService.createRecurring(
                        roomId, userId, title, description, startTime, endTime, attendees, recurType, recurWeeks);
                ra.addFlashAttribute("message", created.size() + "건의 반복 예약이 생성되었습니다.");
            } else {
                reservationService.create(roomId, userId, title, description, startTime, endTime, attendees);
                ra.addFlashAttribute("message", "예약이 생성되었습니다.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations/new?roomId=" + roomId;
        }

        LocalDate weekDate = startTime.toLocalDate().with(DayOfWeek.MONDAY);
        return "redirect:/reservations?week=" + weekDate;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Reservation reservation = reservationService.findById(id);
        model.addAttribute("reservation", reservation);
        model.addAttribute("rooms", roomService.findAllActive());
        model.addAttribute("users", userService.findAll());
        return "reservations/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                         @RequestParam(required = false) String attendees,
                         RedirectAttributes ra) {
        try {
            reservationService.update(id, title, description, startTime, endTime, attendees);
            ra.addFlashAttribute("message", "예약이 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations/" + id + "/edit";
        }
        LocalDate weekDate = startTime.toLocalDate().with(DayOfWeek.MONDAY);
        return "redirect:/reservations?week=" + weekDate;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                         RedirectAttributes ra) {
        reservationService.cancel(id);
        ra.addFlashAttribute("message", "예약이 취소되었습니다.");
        LocalDate weekStart = (week != null) ? week : LocalDate.now().with(DayOfWeek.MONDAY);
        return "redirect:/reservations?week=" + weekStart;
    }

    @PostMapping("/cancel-recurring")
    public String cancelRecurring(@RequestParam String groupId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                                   RedirectAttributes ra) {
        reservationService.cancelFutureRecurring(groupId);
        ra.addFlashAttribute("message", "향후 반복 예약이 모두 취소되었습니다.");
        LocalDate weekStart = (week != null) ? week : LocalDate.now().with(DayOfWeek.MONDAY);
        return "redirect:/reservations?week=" + weekStart;
    }

    @GetMapping("/api/room-schedule")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> roomSchedule(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Reservation> reservations = reservationService.findActiveInRange(start, end)
                .stream()
                .filter(r -> r.getRoom().getId().equals(roomId))
                .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Reservation r : reservations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("startTime", r.getStartTime().format(fmt));
            item.put("endTime", r.getEndTime().format(fmt));
            item.put("userName", r.getUser().getName());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
}
