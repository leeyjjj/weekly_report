package com.weekly.controller;

import com.weekly.entity.Team;
import com.weekly.entity.User;
import com.weekly.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // ─── 사용자 관리 ───

    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("teams", userService.findAllTeams());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String userNewForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("teams", userService.findAllTeams());
        return "admin/user-form";
    }

    @GetMapping("/users/{id}/edit")
    public String userEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("teams", userService.findAllTeams());
        return "admin/user-form";
    }

    @PostMapping("/users")
    public String userCreate(@RequestParam String name,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) Long teamId,
                             RedirectAttributes ra) {
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email);
        if (teamId != null) {
            user.setTeam(userService.findTeamById(teamId));
        }
        userService.save(user);
        ra.addFlashAttribute("message", "사용자가 등록되었습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}")
    public String userUpdate(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) Long teamId,
                             RedirectAttributes ra) {
        userService.updateUser(id, name, email, teamId);
        ra.addFlashAttribute("message", "사용자 정보가 수정되었습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String userDelete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("message", "사용자가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "삭제 실패: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ─── 팀 관리 ───

    @GetMapping("/teams")
    public String teamList(Model model) {
        var teams = userService.findAllTeams();
        model.addAttribute("teams", teams);
        var countMap = new java.util.LinkedHashMap<Long, Long>();
        for (Team t : teams) {
            countMap.put(t.getId(), userService.countByTeam(t.getId()));
        }
        model.addAttribute("memberCounts", countMap);
        return "admin/teams";
    }

    @GetMapping("/teams/new")
    public String teamNewForm(Model model) {
        model.addAttribute("team", new Team());
        return "admin/team-form";
    }

    @GetMapping("/teams/{id}/edit")
    public String teamEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("team", userService.findTeamById(id));
        return "admin/team-form";
    }

    @PostMapping("/teams")
    public String teamCreate(@RequestParam String name, RedirectAttributes ra) {
        Team team = new Team(name.trim());
        userService.saveTeam(team);
        ra.addFlashAttribute("message", "팀이 등록되었습니다.");
        return "redirect:/admin/teams";
    }

    @PostMapping("/teams/{id}")
    public String teamUpdate(@PathVariable Long id, @RequestParam String name, RedirectAttributes ra) {
        userService.updateTeam(id, name);
        ra.addFlashAttribute("message", "팀 정보가 수정되었습니다.");
        return "redirect:/admin/teams";
    }

    @PostMapping("/teams/{id}/delete")
    public String teamDelete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteTeam(id);
            ra.addFlashAttribute("message", "팀이 삭제되었습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teams";
    }
}
