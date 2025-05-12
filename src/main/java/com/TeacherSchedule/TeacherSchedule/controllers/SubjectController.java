package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Subject;
import com.TeacherSchedule.TeacherSchedule.repositories.SubjectRepository;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService; // Import ScheduleService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ScheduleService scheduleService; // Inject ScheduleService

    @GetMapping
    public String showSubjects(Model model) {
        String currentSchoolYear = scheduleService.getCurrentSchoolYear(); // Get the current school year
        model.addAttribute("currentSchoolYear", currentSchoolYear); // Pass it to the template
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

    @PostMapping("/edit")
    @ResponseBody
    public String editSubject(@RequestBody Map<String, String> payload) {
        Long id = Long.parseLong(payload.get("id"));
        String subSubject = payload.get("subSubject");
        String gradeLevel = payload.get("gradeLevel");

        Subject subject = subjectRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid subject ID"));
        subject.setSubSubject(subSubject);
        subject.setGradeLevel(gradeLevel);
        subjectRepository.save(subject);

        return "Success";
    }
}
