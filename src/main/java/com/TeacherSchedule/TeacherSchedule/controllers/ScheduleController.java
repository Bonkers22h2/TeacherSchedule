package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/teachers/schedule/generate")
    public String generateSchedule(@RequestParam("section") String section,
                                   @RequestParam("schoolYear") String schoolYear,
                                   @RequestParam("gradeLevel") String gradeLevel, // Added gradeLevel parameter
                                   RedirectAttributes redirectAttributes) {
        try {
            scheduleService.generateSchedule(section, schoolYear, gradeLevel); // Pass gradeLevel to the service
            redirectAttributes.addFlashAttribute("successMessage", "Schedule successfully generated for section " + section + " in the school year " + schoolYear + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teachers/schedule";
    }
}