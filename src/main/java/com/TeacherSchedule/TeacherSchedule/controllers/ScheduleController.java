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

    @PostMapping("/teachers/autoAssignTeacher")
    public String autoAssignTeacher(@RequestParam("section") String section, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.autoAssignTeachers(section);
            redirectAttributes.addFlashAttribute("successMessage", "Teachers successfully assigned to section " + section + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teachers/allSchedules";
    }

    @PostMapping("/teachers/schedule/generate") // Changed endpoint to avoid conflict
    public String generateSchedule(@RequestParam("section") String section,
                                   @RequestParam("schoolYear") String schoolYear,
                                   RedirectAttributes redirectAttributes) {
        try {
            scheduleService.generateSchedule(section, schoolYear);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule successfully generated for section " + section + " in the school year " + schoolYear + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teachers/schedule";
    }
}