package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.ArchivedTeacher;
import com.TeacherSchedule.TeacherSchedule.models.Attendance;
import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.models.TeacherStatusDTO;
import com.TeacherSchedule.TeacherSchedule.models.Room;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSchedule;
import com.TeacherSchedule.TeacherSchedule.models.Section;
import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedTeacherRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.AttendanceRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedScheduleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;

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

    @Autowired
    private ArchivedScheduleRepository archivedScheduleRepository;

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

        // Fetch the current school year
        String currentSchoolYear = schoolYearRepository.findAll().stream()
                .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                .map(sy -> sy.getYear())
                .orElse("No School Year Available");
        model.addAttribute("currentSchoolYear", currentSchoolYear);

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

        // Create a map to store the count of available rooms for each grade level
        Map<String, Long> availableRoomsByGrade = new HashMap<>();

        for (String gradeLevel : sectionsByGrade.keySet()) {
            long availableRooms = roomRepository.findAll().stream()
                .filter(room -> room.getLabType() == null || room.getLabType().isEmpty()) // General rooms only
                .filter(room -> scheduleService.getAllSchedules().stream()
                    .noneMatch(schedule -> room.getName().equals(schedule.getRoom()) && gradeLevel.equals(schedule.getGradeLevel())))
                .count();
            availableRoomsByGrade.put(gradeLevel, availableRooms);
        }

        // Add the section count and available rooms map to the model
        model.addAttribute("sectionCountByGrade", sectionCountByGrade);
        model.addAttribute("availableRoomsByGrade", availableRoomsByGrade);
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
    public String addTeacher(@ModelAttribute Teacher teacher, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        teacher.setCreatedAt(LocalDate.now());
        repo.save(teacher);
        redirectAttributes.addFlashAttribute("successMessage", "Teacher successfully added.");
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
    public String archiveTeacher(@RequestParam("id") int id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        Teacher teacher = repo.findById(id).orElse(null);
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Teacher not found.");
            return "redirect:/teachers";
        }

        // Check if the teacher is assigned to any schedules
        boolean isAssignedToSchedule = scheduleService.getSchedulesByTeacher(teacher).size() > 0;
        if (isAssignedToSchedule) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot archive teacher. This teacher is assigned to one or more classes.");
            return "redirect:/teachers";
        }

        try {
            // Remove references to the teacher in archived_schedules
            List<ArchivedSchedule> archivedSchedules = archivedScheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getTeacher() != null && schedule.getTeacher().getId() == id)
                .collect(Collectors.toList());
            for (ArchivedSchedule archivedSchedule : archivedSchedules) {
                archivedSchedule.setTeacher(null); // Remove the reference
                archivedScheduleRepository.save(archivedSchedule);
            }

            // Save teacher to archived_teachers table
            ArchivedTeacher archivedTeacher = new ArchivedTeacher(teacher);
            archivedTeacher.setArchivedAt(LocalDate.now()); // Ensure the current date is set
            archivedTeacherRepository.save(archivedTeacher);

            // Delete teacher from the main table
            repo.deleteById(id);

            redirectAttributes.addFlashAttribute("successMessage", "Teacher archived successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to archive teacher: " + e.getMessage());
        }

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

        // Fetch all sections and filter out those already in the schedule
        List<String> unavailableSections = scheduleService.getAllSchedules().stream()
                .map(Schedule::getSection)
                .filter(section -> section != null) // Ensure null values are ignored
                .collect(Collectors.toList());
        List<Section> availableSections = sectionRepository.findAll().stream()
                .filter(section -> !unavailableSections.contains(section.getName())) // Exclude sections already in the schedule
                .collect(Collectors.toList());

        // Fetch all rooms and filter out those already in the schedule or with lab_type not null/none
        List<String> unavailableRooms = scheduleService.getAllSchedules().stream()
                .map(Schedule::getRoom)
                .filter(room -> room != null) // Ensure null values are ignored
                .collect(Collectors.toList());
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(room -> (room.getLabType() == null || room.getLabType().trim().equalsIgnoreCase("")) // Include only general rooms
                        && !unavailableRooms.contains(room.getName())) // Exclude rooms already in the schedule
                .collect(Collectors.toList());

        model.addAttribute("schedule", schedule);
        model.addAttribute("sections", availableSections); // Pass only available sections
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", availableRooms); // Pass only available rooms
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
            @RequestParam("room") String room,
            @RequestParam("gradeLevel") String gradeLevel,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Validate that section and room are not empty
        if (section == null || section.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Section is required to save the schedule.");
            return "redirect:/teachers/schedule";
        }
        if (room == null || room.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room is required to save the schedule.");
            return "redirect:/teachers/schedule";
        }

        try {
            // Fetch the current school year
            String currentSchoolYear = schoolYearRepository.findAll().stream()
                    .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                    .map(sy -> sy.getYear())
                    .orElseThrow(() -> new IllegalStateException("No current school year found."));

            // Ensure the current school year exists in the database
            if (!schoolYearRepository.findAll().stream().anyMatch(sy -> sy.getYear().equals(currentSchoolYear))) {
                SchoolYear newSchoolYear = new SchoolYear(currentSchoolYear);
                schoolYearRepository.save(newSchoolYear);
            }

            // Save the schedule
            scheduleService.saveScheduleWithSubSubjects(section, currentSchoolYear, room, gradeLevel);

            redirectAttributes.addFlashAttribute("successMessage", "Schedule saved successfully for the current school year.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
        }

        return "redirect:/teachers/schedule";
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

    // Load all teachers
    List<Teacher> allTeachers = teacherRepo.findAll();
    if (allTeachers == null || allTeachers.isEmpty()) {
        model.addAttribute("error", "No teachers found.");
        model.addAttribute("teacherCount", 0);
        return "admin/attendance";
    } else {
        model.addAttribute("teacherCount", allTeachers.size());
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

    Set<Integer> allTeacherIds = allTeachers.stream()
        .map(Teacher::getId)
        .collect(Collectors.toSet());

    long teachersAbsent = allTeacherIds.stream()
        .filter(id -> !presentTeacherIds.contains(id))
        .count();

    // Make teacherStatuses (Present, Late, Absent)
    List<TeacherStatusDTO> teacherStatuses = new ArrayList<>();
    for (Teacher teacher : allTeachers) {
        Optional<Attendance> attendance = todaysAttendance.stream()
            .filter(a -> a.getTeacherId() == teacher.getId())
            .findFirst();

        if (attendance.isPresent()) {
            if (attendance.get().getTime().isAfter(cutoffTime)) {
                teacherStatuses.add(new TeacherStatusDTO(teacher, "Late"));
            } else {
                teacherStatuses.add(new TeacherStatusDTO(teacher, "Present"));
            }
        } else {
            teacherStatuses.add(new TeacherStatusDTO(teacher, "Absent"));
        }
    }

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

    // Models
    Map<String, Integer> statusOrder = Map.of("Present", 1, "Late", 2, "Absent", 3);
    teacherStatuses.sort(Comparator.comparing(ts -> statusOrder.getOrDefault(ts.getStatus(), 99)));
    model.addAttribute("teacherStatuses", teacherStatuses);
    model.addAttribute("teachersPresent", teachersPresent);
    model.addAttribute("teachersLate", teachersLate);
    model.addAttribute("teachersAbsent", teachersAbsent);
    model.addAttribute("monthlyPresent", totalPresent);
    model.addAttribute("monthlyLate", totalLate);
    model.addAttribute("monthlyAbsent", totalAbsent);
    model.addAttribute("today", today);


    return "admin/attendance";
}

@GetMapping("/archived")
public String showArchivedTeachers(Model model, HttpSession session) {
    if (!"admin".equals(session.getAttribute("role"))) {
        return "redirect:/signin";
    }

    model.addAttribute("archivedTeachers", archivedTeacherRepository.findAll());
    return "admin/archivedTeachers";
}

    @PostMapping("/autoAssignTeacher")
    public String autoAssignTeachers(@RequestParam(value = "section", required = false) String section,
                                      RedirectAttributes redirectAttributes) {
        try {
            scheduleService.autoAssignTeachers(section);
            redirectAttributes.addFlashAttribute("successMessage", "Teachers successfully assigned!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
        }
        return "redirect:/teachers/allSchedules";
    }

    @PostMapping("/autoAssignLabRoom")
    public String autoAssignLabRoom(@RequestParam(value = "section", required = false) String section,
                                    RedirectAttributes redirectAttributes) {
        try {
            scheduleService.autoAssignLabRooms(section);
            redirectAttributes.addFlashAttribute("successMessage", "Lab rooms successfully assigned!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
        }
        return "redirect:/teachers/allSchedules";
    }

    @PostMapping("/archiveSchedules")
    public String archiveAllSchedules(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<Schedule> schedules = scheduleService.getAllSchedules();
        if (schedules.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No schedules available to archive.");
            return "redirect:/teachers/allSchedules";
        }

        for (Schedule schedule : schedules) {
            ArchivedSchedule archivedSchedule = new ArchivedSchedule(schedule);
            archivedScheduleRepository.save(archivedSchedule);
            scheduleService.deleteSchedule(schedule.getId());
        }

        redirectAttributes.addFlashAttribute("successMessage", "All schedules have been archived.");
        return "redirect:/teachers/allSchedules";
    }

    @GetMapping("/archivedSchedules")
    public String showArchivedSchedules(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<ArchivedSchedule> archivedSchedules = archivedScheduleRepository.findAll();
        model.addAttribute("archivedSchedules", archivedSchedules);

        // Add school years to the model for the dropdown
        model.addAttribute("schoolYears", schoolYearRepository.findAll());

        return "admin/archivedSchedules";
    }

    @GetMapping("/archivedSchedules/filter")
    public String filterArchivedSchedules(@RequestParam(value = "schoolYear", required = false) String schoolYear, 
                                           Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        List<ArchivedSchedule> filteredArchivedSchedules;

        if (schoolYear == null || schoolYear.isEmpty()) {
            // If no school year is selected, show all archived schedules
            filteredArchivedSchedules = archivedScheduleRepository.findAll();
        } else {
            // Filter archived schedules by the selected school year
            filteredArchivedSchedules = archivedScheduleRepository.findAll().stream()
                .filter(schedule -> schoolYear.equals(schedule.getSchoolYear()))
                .collect(Collectors.toList());
        }

        model.addAttribute("filteredArchivedSchedules", filteredArchivedSchedules);
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("selectedSchoolYear", schoolYear);

        return "admin/archivedSchedules";
    }

    @GetMapping("/editSchedule")
    public String showEditScheduleForm(@RequestParam("id") Long id, Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        Schedule schedule = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (schedule == null) {
            model.addAttribute("errorMessage", "Schedule not found.");
            return "redirect:/teachers/allSchedules";
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "admin/editSchedule";
    }

    @PostMapping("/editSchedule")
    public String updateSchedule(@ModelAttribute Schedule schedule, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        try {
            scheduleService.saveScheduleWithSubSubjects(schedule.getSection(), schedule.getSchoolYear(), schedule.getRoom(), schedule.getGradeLevel());
            redirectAttributes.addFlashAttribute("successMessage", "Schedule updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update schedule: " + e.getMessage());
        }

        return "redirect:/teachers/allSchedules";
    }

    @GetMapping("/archiveSchedule")
    public String archiveSchedule(@RequestParam("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        Schedule schedule = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (schedule == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Schedule not found.");
            return "redirect:/teachers/allSchedules";
        }

        ArchivedSchedule archivedSchedule = new ArchivedSchedule(schedule);
        archivedScheduleRepository.save(archivedSchedule);
        scheduleService.deleteSchedule(id);

        redirectAttributes.addFlashAttribute("successMessage", "Schedule archived successfully.");
        return "redirect:/teachers/allSchedules";
    }

}
