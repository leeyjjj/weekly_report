package com.weekly.controller;

import com.weekly.entity.PromptTemplate;
import com.weekly.entity.PromptType;
import com.weekly.service.PromptTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateService service;

    public PromptTemplateController(PromptTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", service.findAll());
        model.addAttribute("types", PromptType.values());
        return "prompt-templates/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("template", new PromptTemplate());
        model.addAttribute("types", PromptType.values());
        return "prompt-templates/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("template", service.findById(id));
        model.addAttribute("types", PromptType.values());
        return "prompt-templates/form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam PromptType promptType,
                         @RequestParam String content,
                         @RequestParam(required = false) boolean isDefault,
                         RedirectAttributes redirectAttrs) {
        PromptTemplate template = new PromptTemplate();
        template.setName(name);
        template.setPromptType(promptType);
        template.setContent(content);
        template.setDefault(isDefault);
        service.create(template);
        redirectAttrs.addFlashAttribute("message", "템플릿이 생성되었습니다.");
        return "redirect:/prompt-templates";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam PromptType promptType,
                         @RequestParam String content,
                         @RequestParam(required = false) boolean isDefault,
                         RedirectAttributes redirectAttrs) {
        PromptTemplate updates = new PromptTemplate();
        updates.setName(name);
        updates.setPromptType(promptType);
        updates.setContent(content);
        updates.setDefault(isDefault);
        service.update(id, updates);
        redirectAttrs.addFlashAttribute("message", "템플릿이 수정되었습니다.");
        return "redirect:/prompt-templates";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        service.delete(id);
        redirectAttrs.addFlashAttribute("message", "템플릿이 삭제되었습니다.");
        return "redirect:/prompt-templates";
    }

    @PostMapping("/{id}/set-default")
    public String setDefault(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        service.setDefault(id);
        redirectAttrs.addFlashAttribute("message", "기본 템플릿으로 설정되었습니다.");
        return "redirect:/prompt-templates";
    }
}
