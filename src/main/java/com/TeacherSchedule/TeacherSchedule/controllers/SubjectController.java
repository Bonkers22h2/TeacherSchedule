package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Subject;
import com.TeacherSchedule.TeacherSchedule.repositories.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping
    public String showSubjects(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll());
        return "admin/subjects";
    }

    @PostMapping("/add")
    public String addSubject(@RequestParam String name, 
                             @RequestParam(required = false) String subSubject, 
                             @RequestParam String gradeLevel, 
                             RedirectAttributes redirectAttributes) {
        Subject subject = new Subject(name, subSubject, gradeLevel);
        subjectRepository.save(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Subject added successfully.");
        return "redirect:/subjects";
    }

    @PostMapping("/delete")
    public String deleteSubject(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        subjectRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        return "redirect:/subjects";
    }
}
