package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;
import com.TeacherSchedule.TeacherSchedule.models.Attendance;
import com.TeacherSchedule.TeacherSchedule.repositories.AttendanceRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    // TEMP: Hardcoded credentials for admin
    private final String adminEmail = "admin";
    private final String adminPassword = "adminadmin";

    @Autowired
    private TeacherRepository teacherRepository; // Inject TeacherRepository

    @Autowired
    private AttendanceRepository attendanceRepository; // Inject AttendanceRepository

    @Autowired
    private SchoolYearRepository schoolYearRepository; // Inject SchoolYearRepository

    @GetMapping("/signin")
    public String showSignInPage(Model model) {
        model.addAttribute("schoolYears", schoolYearRepository.findAll()); // Fetch school years
        return "auth/signin"; // HTML in templates/auth/
    }

    @PostMapping("/signin")
    public String adminSignIn(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam("schoolYear") String schoolYear,
                               HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate admin credentials
        if ("admin".equals(email) && "adminadmin".equals(password)) { // Match hardcoded credentials
            session.setAttribute("role", "admin");
            session.setAttribute("currentSchoolYear", schoolYear); // Set the selected school year
            return "redirect:/teachers";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password.");
            return "redirect:/signin";
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

                // Record attendance
                Attendance attendance = new Attendance(teacher.getId(), LocalDate.now(), LocalTime.now());
                attendanceRepository.save(attendance);

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