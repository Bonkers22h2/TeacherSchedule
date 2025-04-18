package com.TeacherSchedule.TeacherSchedule.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    // TEMP: Hardcoded credentials for now
    private final String adminEmail = "admin";
    private final String adminPassword = "adminadmin";

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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/signin";
    }
}
