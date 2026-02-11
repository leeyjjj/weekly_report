package com.weekly.controller;

import com.weekly.entity.Room;
import com.weekly.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        return "rooms/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("room", new Room());
        return "rooms/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("room", roomService.findById(id));
        return "rooms/form";
    }

    @PostMapping
    public String create(@ModelAttribute Room room, RedirectAttributes ra) {
        roomService.save(room);
        ra.addFlashAttribute("message", "회의실이 등록되었습니다.");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Room room, RedirectAttributes ra) {
        Room existing = roomService.findById(id);
        existing.setName(room.getName());
        existing.setLocation(room.getLocation());
        existing.setCapacity(room.getCapacity());
        existing.setDescription(room.getDescription());
        roomService.save(existing);
        ra.addFlashAttribute("message", "회의실 정보가 수정되었습니다.");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        roomService.deactivate(id);
        ra.addFlashAttribute("message", "회의실이 비활성화되었습니다.");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        roomService.activate(id);
        ra.addFlashAttribute("message", "회의실이 활성화되었습니다.");
        return "redirect:/rooms";
    }
}
