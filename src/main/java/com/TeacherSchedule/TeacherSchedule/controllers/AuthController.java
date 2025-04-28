package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    // TEMP: Hardcoded credentials for admin
    private final String adminEmail = "admin";
    private final String adminPassword = "adminadmin";

    @Autowired
    private TeacherRepository teacherRepository; // Inject TeacherRepository

    @GetMapping("/signin")
    public String showLoginPage() {
        return "auth/signin"; // HTML in templates/auth/
    }

    @PostMapping("/signin")
    public String processLogin(@RequestParam String email,
                                @RequestParam String password,
                                HttpSession session,
                                Model model) {

        if (email.equals(adminEmail) && password.equals(adminPassword)) {
            session.setAttribute("role", "admin");
            return "redirect:/teachers";
        } else {
            model.addAttribute("error", "Invalid credentials");
            return "auth/signin";
        }
    }

    @PostMapping("/signin/teacher")
    public String processTeacherLogin(@RequestParam String teacherId,
                                    HttpSession session,
                                    Model model) {
        try {
            int id = Integer.parseInt(teacherId);
            Teacher teacher = teacherRepository.findById(id).orElse(null);
            if (teacher != null) {
                session.setAttribute("role", "teacher");
                session.setAttribute("teacherId", teacher.getId());
                session.setAttribute("teacherName", teacher.getFirstName() + " " + teacher.getLastName());
                return "redirect:/teachers";
            } else {
                model.addAttribute("error", "Invalid Teacher ID");
                model.addAttribute("activeTab", "teacher");
                return "auth/signin";
            }
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Teacher ID must be a number");
            model.addAttribute("activeTab", "teacher");
            return "auth/signin";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/signin";
    }
}