package com.weekly.service;

import com.weekly.entity.Team;
import com.weekly.entity.User;
import com.weekly.repository.ReportRepository;
import com.weekly.repository.TeamRepository;
import com.weekly.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ReportRepository reportRepository;

    public UserService(UserRepository userRepository, TeamRepository teamRepository,
                       ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.reportRepository = reportRepository;
    }

    public List<User> findAll() {
        return userRepository.findAllByOrderByNameAsc();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public User findOrCreate(String name, Long teamId) {
        return userRepository.findByName(name.trim())
                .orElseGet(() -> {
                    User user = new User();
                    user.setName(name.trim());
                    if (teamId != null) {
                        teamRepository.findById(teamId).ifPresent(user::setTeam);
                    }
                    return userRepository.save(user);
                });
    }

    @Transactional
    public void updateLastActive(User user) {
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, String name, String email, Long teamId) {
        User user = findById(id);
        user.setName(name.trim());
        user.setEmail(email);
        if (teamId != null) {
            teamRepository.findById(teamId).ifPresent(user::setTeam);
        } else {
            user.setTeam(null);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        long reportCount = reportRepository.countByUserId(id);
        if (reportCount > 0) {
            throw new IllegalStateException("작성한 보고서가 " + reportCount + "건 있어 삭제할 수 없습니다. 먼저 보고서를 삭제해주세요.");
        }
        userRepository.deleteById(id);
    }

    public long countByTeam(Long teamId) {
        return userRepository.countByTeamId(teamId);
    }

    public List<Team> findAllTeams() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    public Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Team saveTeam(Team team) {
        return teamRepository.save(team);
    }

    @Transactional
    public Team updateTeam(Long id, String name) {
        Team team = findTeamById(id);
        team.setName(name.trim());
        return teamRepository.save(team);
    }

    @Transactional
    public void deleteTeam(Long id) {
        long count = countByTeam(id);
        if (count > 0) {
            throw new IllegalStateException("소속 사용자가 " + count + "명 있어 삭제할 수 없습니다. 먼저 사용자의 팀을 변경해주세요.");
        }
        teamRepository.deleteById(id);
    }

    @Transactional
    public Team findOrCreateTeam(String teamName) {
        return teamRepository.findByName(teamName.trim())
                .orElseGet(() -> teamRepository.save(new Team(teamName.trim())));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaults() {
        if (userRepository.count() > 0) return;
        log.info("Seeding default team and user...");
        Team admin = teamRepository.findByName("admin")
                .orElseGet(() -> teamRepository.save(new Team("admin")));
        User user = new User();
        user.setName("admin");
        user.setTeam(admin);
        userRepository.save(user);
    }
}
