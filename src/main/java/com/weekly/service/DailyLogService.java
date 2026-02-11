package com.weekly.service;

import com.weekly.entity.DailyLog;
import com.weekly.entity.User;
import com.weekly.repository.DailyLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class DailyLogService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");

    private final DailyLogRepository repository;

    public DailyLogService(DailyLogRepository repository) {
        this.repository = repository;
    }

    public List<DailyLog> getWeekLogs(Long userId, LocalDate weekMonday) {
        LocalDate friday = weekMonday.plusDays(4);
        return repository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, weekMonday, friday);
    }

    /**
     * Returns a map of dayOfWeek(1~5) -> DailyLog for the given week.
     * Missing days have null values.
     */
    public LinkedHashMap<LocalDate, DailyLog> getWeekLogMap(Long userId, LocalDate weekMonday) {
        List<DailyLog> logs = getWeekLogs(userId, weekMonday);
        Map<LocalDate, DailyLog> byDate = new HashMap<>();
        for (DailyLog log : logs) {
            byDate.put(log.getLogDate(), log);
        }

        LinkedHashMap<LocalDate, DailyLog> result = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            LocalDate date = weekMonday.plusDays(i);
            result.put(date, byDate.get(date));
        }
        return result;
    }

    @Transactional
    public DailyLog save(User user, LocalDate logDate, String content) {
        DailyLog log = repository.findByUserIdAndLogDate(user.getId(), logDate)
                .orElseGet(() -> {
                    DailyLog newLog = new DailyLog();
                    newLog.setUser(user);
                    newLog.setLogDate(logDate);
                    return newLog;
                });
        log.setContent(content);
        return repository.save(log);
    }

    /**
     * Combines all daily logs for the week into a single text for report generation.
     */
    public String combineWeekLogs(Long userId, LocalDate weekMonday) {
        List<DailyLog> logs = getWeekLogs(userId, weekMonday);
        if (logs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (DailyLog log : logs) {
            if (log.getContent() == null || log.getContent().isBlank()) continue;
            String dayName = log.getLogDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            sb.append("[").append(dayName).append(" ")
              .append(log.getLogDate().format(DATE_FMT)).append("] ")
              .append(log.getContent().trim()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
