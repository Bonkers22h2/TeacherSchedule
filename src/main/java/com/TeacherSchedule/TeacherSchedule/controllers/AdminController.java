package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.ArchivedTeacher;
import com.TeacherSchedule.TeacherSchedule.models.Attendance;
import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedTeacherRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.AttendanceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teachers")
public class AdminController {

    @Autowired
    private TeacherRepository repo;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SchoolYearRepository schoolYearRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ArchivedTeacherRepository archivedTeacherRepository; // Add repository for archived teachers

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private ScheduleService scheduleService;

    // Show teacher list, but only if admin is logged in
    @GetMapping({ "", "/" })
    public String showTeacherList(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");

        if ("teacher".equals(role)) {
            Integer teacherId = (Integer) session.getAttribute("teacherId");
            if (teacherId == null) {
                return "redirect:/signin";
            }
    
            // Fetch the teacher entity
            Teacher teacher = repo.findById(teacherId).orElse(null);
            if (teacher == null) {
                return "redirect:/signin";
            }
    
            // Get schedules for this teacher
            List<Schedule> schedules = scheduleService.getSchedulesByTeacher(teacher);
            
            // Derive teacherName from the Teacher entity
            String teacherName = teacher.getFirstName() + " " + teacher.getLastName(); // ✅ Get from entity
            
            model.addAttribute("schedules", schedules);
            model.addAttribute("teacherName", teacherName); // Pass to template
            return "teacher/index";
        }
        


        List<Teacher> teachers = repo.findAll();
        if (teachers == null || teachers.isEmpty()) {
            model.addAttribute("error", "No teachers found.");
            model.addAttribute("teacherCount", 0);
        } else {
            model.addAttribute("teachers", teachers);
            model.addAttribute("teacherCount", teachers.size());
        }

        
    // Count number of teachers present today
        LocalDate today = LocalDate.now();
        List<Attendance> todaysAttendance = attendanceRepo.findAll()
            .stream()
            .filter(a -> today.equals(a.getDate()))
            .collect(Collectors.toList());

        long teachersPresent = todaysAttendance.stream()
            .map(Attendance::getTeacherId)
            .distinct()
            .count();


        model.addAttribute("teachersPresent", teachersPresent);
        addSectionCountToModel(model);
        return "admin/index";
    }


    // Show Section count
    private void addSectionCountToModel(Model model) {
        // Fetch all schedules (to count sections per grade level)
        List<Schedule> schedules = scheduleService.getAllSchedules();

        // Create a map to store unique sections for each grade level
        Map<String, Set<String>> sectionsByGrade = new HashMap<>();

        // Populate the map with grade levels and their respective unique sections
        for (Schedule schedule : schedules) {
            String gradeLevel = schedule.getGradeLevel();
            String section = schedule.getSection();

            if (gradeLevel != null && section != null) {
                sectionsByGrade
                        .computeIfAbsent(gradeLevel, k -> new HashSet<>())
                        .add(section);
            }
        }

        // Create a map to store the count of sections for each grade level
        Map<String, Integer> sectionCountByGrade = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : sectionsByGrade.entrySet()) {
            sectionCountByGrade.put(entry.getKey(), entry.getValue().size());
        }

        // Add the section count map to the model
        model.addAttribute("sectionCountByGrade", sectionCountByGrade);
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

    @GetMapping("/archive")
    public String archiveTeacher(@RequestParam("id") int id, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher not found");
        }

        // Save teacher to archived_teachers table
        ArchivedTeacher archivedTeacher = new ArchivedTeacher(teacher);
        archivedTeacherRepository.save(archivedTeacher);

        // Delete teacher from the main table
        repo.deleteById(id);

        return "redirect:/teachers";
    }

    @GetMapping("/schedule")
    public String showSchedule(Model model, HttpSession session,
            @RequestParam(value = "selectedSection", required = false) String selectedSection,
            @RequestParam(value = "selectedSchoolYear", required = false) String selectedSchoolYear,
            @RequestParam(value = "selectedRoom", required = false) String selectedRoom) {
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
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll()); // Fetch from the "room" table
        model.addAttribute("selectedSection", selectedSection);
        model.addAttribute("selectedSchoolYear", selectedSchoolYear);
        model.addAttribute("selectedRoom", selectedRoom);
        return "admin/schedule";
    }

    @PostMapping("/generateSchedule")
    public String generateSchedule(@RequestParam("section") String section,
            @RequestParam("schoolYear") String schoolYear,
            @RequestParam("room") String room,
            @RequestParam("gradeLevel") String gradeLevel,
            Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Check if a schedule already exists
        if (!scheduleService.getFilteredSchedules(section, schoolYear, gradeLevel).isEmpty()) {
            model.addAttribute("error", "A schedule already exists for the selected section, school year, and grade level.");
            model.addAttribute("sections", sectionRepository.findAll());
            model.addAttribute("schoolYears", schoolYearRepository.findAll());
            model.addAttribute("rooms", roomRepository.findAll());
            model.addAttribute("selectedSection", section);
            model.addAttribute("selectedSchoolYear", schoolYear);
            model.addAttribute("selectedRoom", room);
            model.addAttribute("selectedGradeLevel", gradeLevel);
            return "admin/schedule";
        }

        try {
            List<String> schedule = scheduleService.generateSchedule(section, schoolYear, gradeLevel);
            model.addAttribute("schedule", schedule);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("selectedSection", section);
        model.addAttribute("selectedSchoolYear", schoolYear);
        model.addAttribute("selectedRoom", room);
        model.addAttribute("selectedGradeLevel", gradeLevel);
        return "admin/schedule";
    }

    @PostMapping("/saveSchedule")
    public String saveSchedule(@RequestParam("section") String section,
            @RequestParam("schoolYear") String schoolYear,
            @RequestParam("room") String room,
            @RequestParam("gradeLevel") String gradeLevel,
            HttpSession session, Model model) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Check if a schedule already exists
        if (!scheduleService.getFilteredSchedules(section, schoolYear, gradeLevel).isEmpty()) {
            model.addAttribute("error", "A schedule already exists for the selected section, school year, and grade level.");
            model.addAttribute("sections", sectionRepository.findAll());
            model.addAttribute("schoolYears", schoolYearRepository.findAll());
            model.addAttribute("rooms", roomRepository.findAll());
            model.addAttribute("selectedSection", section);
            model.addAttribute("selectedSchoolYear", schoolYear);
            model.addAttribute("selectedRoom", room);
            model.addAttribute("selectedGradeLevel", gradeLevel);
            return "admin/schedule";
        }

        try {
            // Use the updated method to save schedules with subsubjects
            scheduleService.saveScheduleWithSubSubjects(section, schoolYear, room, gradeLevel);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("selectedSection", section);
        model.addAttribute("selectedSchoolYear", schoolYear);
        model.addAttribute("selectedRoom", room);
        model.addAttribute("selectedGradeLevel", gradeLevel); // Pass the selected grade level to the model
        return "admin/schedule";
    }

    @GetMapping("/allSchedules")
    public String showAllSchedules(Model model, HttpSession session,
            @RequestParam(value = "selectedSection", required = false) String selectedSection,
            @RequestParam(value = "selectedSchoolYear", required = false) String selectedSchoolYear,
            @RequestParam(value = "selectedGradeLevel", required = false) String selectedGradeLevel) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Schedule> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("selectedSection", selectedSection);
        model.addAttribute("selectedSchoolYear", selectedSchoolYear);
        model.addAttribute("selectedGradeLevel", selectedGradeLevel);
        return "admin/allSchedules";
    }

    @GetMapping("/filterSchedule")
    public String filterSchedule(@RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "schoolYear", required = false) String schoolYear,
            @RequestParam(value = "gradeLevel", required = false) String gradeLevel,
            Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Schedule> schedules;

        if ((section == null || section.isEmpty()) &&
                (schoolYear == null || schoolYear.isEmpty()) &&
                (gradeLevel == null || gradeLevel.isEmpty())) {
            schedules = scheduleService.getAllSchedules(); // No filters applied
        } else {
            schedules = scheduleService.getFilteredSchedules(section, schoolYear, gradeLevel);
        }

        model.addAttribute("schedules", schedules);
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("selectedSection", section);
        model.addAttribute("selectedSchoolYear", schoolYear);
        model.addAttribute("selectedGradeLevel", gradeLevel);
        return "admin/allSchedules";
    }

    @GetMapping("/profile")
    public String showProfile(@RequestParam(value = "teacherName", required = false) String teacherName,
            Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }
        model.addAttribute("teacherName", teacherName); // Pass teacherName to the view
        return "admin/profile";
    }

    public String getMethodName(@RequestParam String param) {
        return new String();
    }

    @GetMapping("/attendance")
    public String showTeacherList(HttpSession session, Model model) {
    if (!"admin".equals(session.getAttribute("role"))) {
        return "redirect:/signin";
    }

    List<Teacher> teachers = repo.findAll();
    if (teachers == null || teachers.isEmpty()) {
        model.addAttribute("error", "No teachers found.");
        model.addAttribute("teacherCount", 0);
    } else {
        model.addAttribute("teachers", teachers);
        model.addAttribute("teacherCount", teachers.size());
    }

    LocalDate today = LocalDate.now();
    LocalTime cutoffTime = LocalTime.of(8, 0);

    // ---------- DAILY ATTENDANCE ----------
    List<Attendance> todaysAttendance = attendanceRepo.findAll()
        .stream()
        .filter(a -> today.equals(a.getDate()))
        .collect(Collectors.toList());

    Set<Integer> presentTeacherIds = todaysAttendance.stream()
        .map(Attendance::getTeacherId)
        .collect(Collectors.toSet());

    long teachersPresent = presentTeacherIds.size();

    long teachersLate = todaysAttendance.stream()
        .filter(a -> a.getTime().isAfter(cutoffTime))
        .map(Attendance::getTeacherId)
        .distinct()
        .count();

    List<Teacher> allTeachers = teacherRepo.findAll();
    Set<Integer> allTeacherIds = allTeachers.stream()
        .map(Teacher::getId)
        .collect(Collectors.toSet());

    long teachersAbsent = allTeacherIds.stream()
        .filter(id -> !presentTeacherIds.contains(id))
        .count();

    model.addAttribute("teachersPresent", teachersPresent);
    model.addAttribute("teachersLate", teachersLate);
    model.addAttribute("teachersAbsent", teachersAbsent);

    // ---------- MONTHLY ATTENDANCE ----------
    YearMonth currentMonth = YearMonth.now();
    LocalDate startOfMonth = currentMonth.atDay(1);
    LocalDate endOfMonth = currentMonth.atEndOfMonth();

    List<Attendance> monthlyAttendance = attendanceRepo.findAll()
        .stream()
        .filter(a -> !a.getDate().isBefore(startOfMonth) && !a.getDate().isAfter(endOfMonth))
        .collect(Collectors.toList());

    Map<LocalDate, List<Attendance>> attendanceByDate = monthlyAttendance.stream()
        .collect(Collectors.groupingBy(Attendance::getDate));

    long totalPresent = 0;
    long totalLate = 0;
    long totalAbsent = 0;

    for (LocalDate date : attendanceByDate.keySet()) {
        List<Attendance> dailyRecords = attendanceByDate.get(date);

        Set<Integer> presentIds = dailyRecords.stream()
            .map(Attendance::getTeacherId)
            .collect(Collectors.toSet());

        Set<Integer> lateIds = dailyRecords.stream()
            .filter(a -> a.getTime().isAfter(cutoffTime))
            .map(Attendance::getTeacherId)
            .collect(Collectors.toSet());

        totalPresent += presentIds.size();
        totalLate += lateIds.size();
        totalAbsent += allTeacherIds.stream()
            .filter(id -> !presentIds.contains(id))
            .count();
    }

    model.addAttribute("monthlyPresent", totalPresent);
    model.addAttribute("monthlyLate", totalLate);
    model.addAttribute("monthlyAbsent", totalAbsent);

    return "admin/attendance";
}

    @PostMapping("/autoAssignTeacher")
    public String autoAssignTeachers(@RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "schoolYear", required = false) String schoolYear,
            @RequestParam(value = "gradeLevel", required = false) String gradeLevel,
            HttpSession session, Model model) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        try {
            scheduleService.autoAssignTeachers(section, schoolYear, gradeLevel);
            model.addAttribute("successMessage", "Teachers successfully assigned based on the selected filters.");
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        List<Schedule> schedules = scheduleService.getFilteredSchedules(section, schoolYear, gradeLevel);
        model.addAttribute("schedules", schedules);
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("selectedSection", section);
        model.addAttribute("selectedSchoolYear", schoolYear);
        model.addAttribute("selectedGradeLevel", gradeLevel);
        return "admin/allSchedules";
    }

    @PostMapping("/autoAssignLabRoom")
    public String autoAssignLabRoom(@RequestParam(value = "section", required = false) String section,
                                    @RequestParam(value = "schoolYear", required = false) String schoolYear,
                                    @RequestParam(value = "gradeLevel", required = false) String gradeLevel,
                                    RedirectAttributes redirectAttributes) {
        try {
            scheduleService.autoAssignLabRooms(section, schoolYear, gradeLevel);
            redirectAttributes.addFlashAttribute("successMessage", "Lab rooms successfully assigned!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teachers/allSchedules";
    }

}
