package com.weekly.controller;

import com.weekly.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userService.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getName(),
                        u.getTeam() != null ? u.getTeam().getName() : null))
                .toList();
    }

    record UserDto(Long id, String name, String teamName) {}
}
