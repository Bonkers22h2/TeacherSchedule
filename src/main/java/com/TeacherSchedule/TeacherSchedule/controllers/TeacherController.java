package com.TeacherSchedule.TeacherSchedule.controllers;

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
public class TeacherController {

    @Autowired
    private TeacherRepository repo;

    // Show teacher list, but only if admin is logged in
    @GetMapping({ "", "/" })
    public String showTeacherList(Model model, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        List<Teacher> teachers = repo.findAll();
        model.addAttribute("teachers", teachers);
        return "teachers/index";
    }

    // Show add form
    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        model.addAttribute("teacher", new Teacher());
        return "teachers/add";
    }

    // Handle form submission
    @PostMapping("/add")
    public String addTeacher(@ModelAttribute Teacher teacher, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        teacher.setCreatedAt(LocalDate.now());
        repo.save(teacher);
        return "redirect:/teachers";
    }

    @GetMapping("/edit")
    public String showEditForm(@RequestParam("id") int id, Model model, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher == null) {
            return "redirect:/teachers";
        }

        model.addAttribute("teacher", teacher);
        return "teachers/edit";
    }

    @PostMapping("/edit")
    public String updateTeacher(@ModelAttribute Teacher teacher, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        repo.save(teacher); // Automatically updates by ID
        return "redirect:/teachers";
    }

    @GetMapping("/delete")
    public String deleteTeacher(@RequestParam("id") int id, HttpSession session) {
        if (session.getAttribute("adminLoggedIn") == null) {
            return "redirect:/signin";
        }

        repo.deleteById(id);
        return "redirect:/teachers";
    }

    private final ScheduleService scheduleService;

    public TeacherController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedule")
    public String showSchedule(Model model) {
        model.addAttribute("schedule", scheduleService.generateSchedule());
        return "teachers/schedule"; // This must match the HTML filename inside `templates/teachers/`
    }

    @PostMapping("/generateSchedule")
    public String generateSchedule(Model model) {
        model.addAttribute("schedule", scheduleService.generateSchedule());
        return "teachers/schedule";
    }

}
