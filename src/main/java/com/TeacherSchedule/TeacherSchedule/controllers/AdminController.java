package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/teachers")
public class AdminController {

    @Autowired
    private TeacherRepository repo;

    // Show teacher list, but only if admin is logged in
    @GetMapping({ "", "/" })
    public String showTeacherList(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Teacher> teachers = repo.findAll();
        model.addAttribute("teachers", teachers);
        return "admin/index";
    }

    // Show add form
    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        model.addAttribute("teacher", new Teacher());
        return "admin/add";
    }

    // Handle form submission
    @PostMapping("/add")
    public String addTeacher(@ModelAttribute Teacher teacher, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        teacher.setCreatedAt(LocalDate.now());
        repo.save(teacher);
        return "redirect:/teachers";
    }

    @GetMapping("/edit")
    public String showEditForm(@RequestParam("id") int id, Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher == null) {
            return "redirect:/teachers";
        }

        model.addAttribute("teacher", teacher);
        return "admin/edit";
    }

    @PostMapping("/edit")
    public String updateTeacher(@ModelAttribute Teacher teacher, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        repo.save(teacher); // Automatically updates by ID
        return "redirect:/teachers";
    }

    @GetMapping("/delete")
    public String deleteTeacher(@RequestParam("id") int id, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        repo.deleteById(id);
        return "redirect:/teachers";
    }

    private final ScheduleService scheduleService;

    public AdminController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedule")
    public String showSchedule(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<String> schedule = scheduleService.getCurrentSchedule();

        // Validate schedule format
        for (int i = 0; i < schedule.size(); i++) {
            String[] parts = schedule.get(i).split(" - ");
            if (parts.length < 4) {
                schedule.set(i, schedule.get(i) + " - Unknown Section"); // Add default section if missing
            }
        }

        model.addAttribute("schedule", schedule);
        return "admin/schedule";
    }

    @PostMapping("/generateSchedule")
    public String generateSchedule(@RequestParam("section") String section, Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        try {
            model.addAttribute("schedule", scheduleService.generateSchedule(section));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("selectedSection", section); // Pass the selected section to the model
        return "admin/schedule";
    }

    @PostMapping("/saveSchedule")
    public String saveSchedule(@RequestParam("section") String section, HttpSession session, Model model) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        scheduleService.saveSchedule(section);
        model.addAttribute("selectedSection", section); // Pass the selected section to the model
        return "redirect:/teachers/schedule"; // Corrected redirect path
    }

    @GetMapping("/allSchedules")
    public String showAllSchedules(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Schedule> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        return "admin/allSchedules";
    }

    @GetMapping("/filterSchedule")
    public String filterSchedule(@RequestParam("section") String section, Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Schedule> schedules = scheduleService.getSchedulesBySection(section);
        model.addAttribute("schedules", schedules);
        return "admin/allSchedules";
    }
}
