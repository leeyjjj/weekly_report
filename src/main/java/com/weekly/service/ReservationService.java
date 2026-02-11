package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.entity.Reservation;
import com.weekly.entity.Room;
import com.weekly.entity.User;
import com.weekly.repository.ReservationRepository;
import com.weekly.repository.RoomRepository;
import com.weekly.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RestClient teamsRestClient;
    private final AppConfig.TeamsConfig teamsConfig;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository,
                              RestClient teamsRestClient,
                              AppConfig appConfig) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.teamsRestClient = teamsRestClient;
        this.teamsConfig = appConfig.teams();
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Reservation create(Long roomId, Long userId, String title, String description,
                               LocalDateTime startTime, LocalDateTime endTime, String attendees) {
        validateTime(startTime, endTime);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("회의실을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!reservationRepository.findOverlappingWithLock(roomId, startTime, endTime).isEmpty()) {
            throw new IllegalStateException("해당 시간에 이미 예약이 있습니다.");
        }

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setUser(user);
        reservation.setTitle(title);
        reservation.setDescription(description);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setAttendees(attendees);
        reservation.setRecurring(false);
        reservation.setCancelled(false);

        Reservation saved = reservationRepository.save(reservation);
        sendTeamsNotification(saved);
        return saved;
    }

    @Transactional
    public List<Reservation> createRecurring(Long roomId, Long userId, String title, String description,
                                              LocalDateTime startTime, LocalDateTime endTime,
                                              String attendees, String recurType, int weeks) {
        validateTime(startTime, endTime);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("회의실을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String groupId = UUID.randomUUID().toString();
        List<Reservation> created = new ArrayList<>();
        List<LocalDate> skipped = new ArrayList<>();

        List<LocalDate> dates = generateDates(startTime.toLocalDate(), recurType, weeks);
        LocalTime sTime = startTime.toLocalTime();
        LocalTime eTime = endTime.toLocalTime();

        for (LocalDate date : dates) {
            LocalDateTime s = date.atTime(sTime);
            LocalDateTime e = date.atTime(eTime);

            if (!reservationRepository.findOverlappingWithLock(roomId, s, e).isEmpty()) {
                skipped.add(date);
                continue;
            }

            Reservation reservation = new Reservation();
            reservation.setRoom(room);
            reservation.setUser(user);
            reservation.setTitle(title);
            reservation.setDescription(description);
            reservation.setStartTime(s);
            reservation.setEndTime(e);
            reservation.setAttendees(attendees);
            reservation.setRecurring(true);
            reservation.setRecurringGroupId(groupId);
            reservation.setCancelled(false);

            created.add(reservationRepository.save(reservation));
        }

        if (!created.isEmpty()) {
            sendTeamsNotification(created.getFirst());
        }

        if (!skipped.isEmpty()) {
            log.info("반복 예약 중 충돌로 스킵된 날짜: {}", skipped);
        }

        return created;
    }

    @Transactional
    public Reservation update(Long id, String title, String description,
                               LocalDateTime startTime, LocalDateTime endTime, String attendees) {
        validateTime(startTime, endTime);
        Reservation reservation = findById(id);

        if (reservationRepository.existsOverlapExcluding(
                reservation.getRoom().getId(), startTime, endTime, id)) {
            throw new IllegalStateException("해당 시간에 이미 예약이 있습니다.");
        }

        reservation.setTitle(title);
        reservation.setDescription(description);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setAttendees(attendees);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancel(Long id) {
        Reservation reservation = findById(id);
        reservation.setCancelled(true);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelFutureRecurring(String groupId) {
        List<Reservation> future = reservationRepository
                .findFutureByRecurringGroup(groupId, LocalDateTime.now());
        for (Reservation r : future) {
            r.setCancelled(true);
            reservationRepository.save(r);
        }
    }

    public List<Reservation> findActiveInRange(LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findAllActiveInRange(start, end);
    }

    public Map<Long, Map<LocalDate, List<Reservation>>> buildWeeklyCalendar(LocalDate weekStart) {
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(5).atStartOfDay(); // Mon~Fri

        List<Reservation> reservations = reservationRepository.findAllActiveInRange(start, end);
        List<Room> rooms = roomRepository.findByActiveTrueOrderByNameAsc();

        Map<Long, Map<LocalDate, List<Reservation>>> calendar = new LinkedHashMap<>();
        for (Room room : rooms) {
            Map<LocalDate, List<Reservation>> dayMap = new LinkedHashMap<>();
            for (int i = 0; i < 5; i++) {
                dayMap.put(weekStart.plusDays(i), new ArrayList<>());
            }
            calendar.put(room.getId(), dayMap);
        }

        for (Reservation r : reservations) {
            LocalDate day = r.getStartTime().toLocalDate();
            Map<LocalDate, List<Reservation>> dayMap = calendar.get(r.getRoom().getId());
            if (dayMap != null && dayMap.containsKey(day)) {
                dayMap.get(day).add(r);
            }
        }

        return calendar;
    }

    private List<LocalDate> generateDates(LocalDate startDate, String recurType, int weeks) {
        List<LocalDate> dates = new ArrayList<>();
        if ("daily".equals(recurType)) {
            for (int w = 0; w < weeks; w++) {
                for (int d = 0; d < 5; d++) { // Mon~Fri
                    LocalDate date = startDate.with(DayOfWeek.MONDAY).plusWeeks(w).plusDays(d);
                    if (!date.isBefore(startDate)) {
                        dates.add(date);
                    }
                }
            }
        } else { // weekly
            for (int w = 0; w < weeks; w++) {
                dates.add(startDate.plusWeeks(w));
            }
        }
        return dates;
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("시작 시간과 종료 시간을 입력해주세요.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }
        if (java.time.Duration.between(start, end).toHours() > 8) {
            throw new IllegalArgumentException("예약은 최대 8시간까지 가능합니다.");
        }
    }

    private void sendTeamsNotification(Reservation reservation) {
        if (teamsConfig == null || !teamsConfig.isEnabled()) {
            return;
        }
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");
            String time = reservation.getStartTime().format(fmt) + " ~ " + reservation.getEndTime().format(fmt);

            Map<String, Object> payload = AdaptiveCardBuilder.buildReservation(
                    reservation.getRoom().getName(),
                    reservation.getTitle(),
                    time,
                    reservation.getUser().getName(),
                    reservation.getAttendees()
            );

            teamsRestClient.post()
                    .uri(teamsConfig.webhookUrl())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("예약 Teams 알림 전송 완료: {}", reservation.getTitle());
        } catch (Exception e) {
            log.warn("예약 Teams 알림 전송 실패: {}", e.getMessage());
        }
    }
}
