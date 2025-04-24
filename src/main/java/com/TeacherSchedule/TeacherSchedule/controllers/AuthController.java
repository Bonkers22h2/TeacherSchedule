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
    public String processTeacherLogin(@RequestParam int teacherId,
                                       HttpSession session,
                                       Model model) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null); // Fetch teacher by ID
        if (teacher != null) {
            session.setAttribute("role", "teacher");
            session.setAttribute("teacherId", teacher.getId()); // Store teacher ID in session
            session.setAttribute("teacherName", teacher.getFirstName() + " " + teacher.getLastName()); // Store teacher name
            return "redirect:/teachers"; // Redirect to teacher-specific page
        } else {
            model.addAttribute("error", "Invalid Teacher ID");
            return "auth/signin";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/signin";
    }
}