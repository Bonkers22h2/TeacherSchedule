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
import com.TeacherSchedule.TeacherSchedule.repositories.SubjectRepository; // Import SubjectRepository
import com.TeacherSchedule.TeacherSchedule.models.Subject; // Import Subject class

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

    @Autowired
    private SubjectRepository subjectRepository; // Inject SubjectRepository

    // Show teacher list, but only if admin is logged in
    @GetMapping({ "", "/" })
    public String showTeacherList(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");

        // --- Fetch the current school year ---
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        // --- Check if allEntities.html should be shown ---
        String allEntitiesFlag = "allEntitiesShown_" + currentSchoolYear;
        boolean hasSchedules = !scheduleService.getSchedulesBySchoolYear(currentSchoolYear).isEmpty();
        if ("admin".equals(role) && session.getAttribute(allEntitiesFlag) == null && !hasSchedules) {
            session.setAttribute(allEntitiesFlag, true);
            return "redirect:/teachers/allEntities";
        }

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

            // Fetch the current school year from the database
            final String teacherSchoolYear = schoolYearRepository.findAll().stream()
                    .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                    .map(SchoolYear::getYear)
                    .orElse("No School Year Available");
            session.setAttribute("currentSchoolYear", teacherSchoolYear);

            // Filter schedules for the current school year
            List<Schedule> schedules = scheduleService.getSchedulesByTeacher(teacher).stream()
                    .filter(schedule -> teacherSchoolYear.equals(schedule.getSchoolYear()))
                    .collect(Collectors.toList());

            // Derive teacherName from the Teacher entity
            String teacherName = teacher.getFirstName() + " " + teacher.getLastName();

            model.addAttribute("schedules", schedules);
            model.addAttribute("teacherName", teacherName);
            model.addAttribute("currentSchoolYear", teacherSchoolYear);
            return "teacher/index";
        }

        // Fetch the current school year from the session
        String filteredSchoolYear;
        {
            String tempSchoolYear = (String) session.getAttribute("currentSchoolYear");
            if (tempSchoolYear == null || tempSchoolYear.isEmpty()) {
                tempSchoolYear = scheduleService.getCurrentSchoolYear();
                session.setAttribute("currentSchoolYear", tempSchoolYear);
            }
            filteredSchoolYear = tempSchoolYear;
        }

        // Filter teachers by current school year
        List<Teacher> teachers = repo.findAll().stream()
            .filter(t -> filteredSchoolYear.equals(t.getSchoolYear()))
            .collect(Collectors.toList());
        if (teachers == null || teachers.isEmpty()) {
            model.addAttribute("error", "No teachers found.");
            model.addAttribute("teacherCount", 0);
        } else {
            model.addAttribute("teachers", teachers);
            model.addAttribute("teacherCount", teachers.size());
        }

        
    // Count number of teachers present today
        LocalDate today = LocalDate.now();
        List<Teacher> allTeachers = teacherRepo.findAll().stream()
            .filter(teacher -> filteredSchoolYear.equals(teacher.getSchoolYear()))
            .collect(Collectors.toList());

        List<Attendance> todaysAttendance = attendanceRepo.findAll()
            .stream()
            .filter(a -> today.equals(a.getDate()))
            .collect(Collectors.toList());

        long teachersPresent = todaysAttendance.stream()
            .map(Attendance::getTeacherId)
            .distinct()
            .count();


        model.addAttribute("teachersPresent", teachersPresent);
        addSectionCountToModel(model, currentSchoolYear);

        // Pass the current school year to the dashboard
        model.addAttribute("currentSchoolYear", effectiveCurrentSchoolYear);

        // Add logic to determine if the current school year is the latest and pass it to the model
        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");

        model.addAttribute("latestSchoolYear", latestSchoolYear);

        return "admin/index";
    }


    // Show Section count
    private void addSectionCountToModel(Model model, String currentSchoolYear) {
        // Fetch all schedules for the current school year
        List<Schedule> schedules = scheduleService.getSchedulesBySchoolYear(currentSchoolYear);

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
                .filter(room -> currentSchoolYear.equals(room.getSchoolYear())) // Filter by current school year
                .filter(room -> room.getLabType() == null || room.getLabType().isEmpty()) // General rooms only
                .filter(room -> schedules.stream()
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
        model.addAttribute("subjects", subjectRepository.findAll().stream()
            .map(Subject::getName) // Extract subject names
            .distinct() // Ensure uniqueness
            .collect(Collectors.toList())); // Collect as a list of Strings
        return "admin/add";
    }

    // Handle form submission
    @PostMapping("/add")
    public String addTeacher(@ModelAttribute Teacher teacher, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }
        
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        teacher.setCreatedAt(LocalDate.now());
        teacher.setSchoolYear(effectiveCurrentSchoolYear); // Set the current school year
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
            @RequestParam(value = "selectedRoom", required = false) String selectedRoom,
            @RequestParam(value = "selectedGradeLevel", required = false) String selectedGradeLevel
    ) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Fetch the current school year
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        // Fetch the latest school year
        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");

        // Add school year attributes to the model
        model.addAttribute("currentSchoolYear", effectiveCurrentSchoolYear);
        model.addAttribute("latestSchoolYear", latestSchoolYear);

        // Fetch all sections and exclude those already in a schedule for the current school year
        List<String> scheduledSections = scheduleService.getSchedulesBySchoolYear(effectiveCurrentSchoolYear).stream()
                .map(Schedule::getSection)
                .collect(Collectors.toList());
        List<Section> availableSections = sectionRepository.findAll().stream()
                .filter(section -> effectiveCurrentSchoolYear.equals(section.getSchoolYear()))
                .filter(section -> !scheduledSections.contains(section.getName()))
                .collect(Collectors.toList());

        // Fetch all rooms and exclude those already in a schedule for the current school year
        List<String> scheduledRooms = scheduleService.getSchedulesBySchoolYear(effectiveCurrentSchoolYear).stream()
                .map(Schedule::getRoom)
                .collect(Collectors.toList());
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(room -> effectiveCurrentSchoolYear.equals(room.getSchoolYear()))
                .filter(room -> !scheduledRooms.contains(room.getName()))
                .collect(Collectors.toList());

        // Generate schedule if all required params are present
        List<String> schedule = null;
        if (selectedSection != null && !selectedSection.isEmpty()
                && selectedSchoolYear != null && !selectedSchoolYear.isEmpty()
                && selectedGradeLevel != null && !selectedGradeLevel.isEmpty()) {
            try {
                schedule = scheduleService.generateSchedule(selectedSection, selectedSchoolYear, selectedGradeLevel);
                model.addAttribute("schedule", schedule);
            } catch (IllegalStateException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("schedule", scheduleService.getCurrentSchedule());
            }
        } else {
            model.addAttribute("schedule", scheduleService.getCurrentSchedule());
        }

        model.addAttribute("sections", availableSections);
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", availableRooms);
        model.addAttribute("selectedSection", selectedSection);
        model.addAttribute("selectedSchoolYear", effectiveCurrentSchoolYear);
        model.addAttribute("selectedRoom", selectedRoom);
        model.addAttribute("selectedGradeLevel", selectedGradeLevel);
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

        // Ensure the school year is the current one
        String currentSchoolYear = schoolYearRepository.findAll().stream()
                .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                .map(SchoolYear::getYear)
                .orElseThrow(() -> new IllegalStateException("No current school year found."));

        if (!schoolYear.equals(currentSchoolYear)) {
            model.addAttribute("error", "You can only generate schedules for the current school year.");
            return "redirect:/teachers/schedule";
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
        model.addAttribute("selectedGradeLevel", gradeLevel); // <-- keep selected grade level
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

        // Fetch the current school year from the session
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        // Default to the current school year if none is selected
        if (selectedSchoolYear == null || selectedSchoolYear.isEmpty()) {
            selectedSchoolYear = effectiveCurrentSchoolYear;
        }

        List<Schedule> schedules = scheduleService.getSchedulesBySchoolYear(selectedSchoolYear);
        model.addAttribute("schedules", schedules);
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("selectedSection", selectedSection);
        model.addAttribute("selectedSchoolYear", selectedSchoolYear);
        model.addAttribute("selectedGradeLevel", selectedGradeLevel);

        // Add currentSchoolYear and latestSchoolYear to the model
        model.addAttribute("currentSchoolYear", effectiveCurrentSchoolYear);
        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");
        model.addAttribute("latestSchoolYear", latestSchoolYear);

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

        // Fetch the current school year from the session if not provided
        String currentSchoolYear = (String) session.getAttribute("currentSchoolYear");
        if (schoolYear == null || schoolYear.isEmpty()) {
            schoolYear = currentSchoolYear;
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

        // Add currentSchoolYear and latestSchoolYear to the model
        model.addAttribute("currentSchoolYear", currentSchoolYear);
        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");
        model.addAttribute("latestSchoolYear", latestSchoolYear);

        return "admin/allSchedules";
    }

    @GetMapping("/profile")
    public String showProfile(@RequestParam(value = "teacherName", required = false) String teacherName,
            Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Fetch the current school year
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        // Add school year attributes to the model
        model.addAttribute("currentSchoolYear", effectiveCurrentSchoolYear);

        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");

        model.addAttribute("latestSchoolYear", latestSchoolYear);
        model.addAttribute("teacherName", teacherName);

        // Add distinct subjects to the model
        model.addAttribute("subjects", subjectRepository.findAll().stream()
            .map(Subject::getName) // Extract subject names
            .distinct() // Ensure uniqueness
            .collect(Collectors.toList()));

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

    // Fetch the current school year
    String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
    String currentSchoolYear = sessionSchoolYear;
    if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
        currentSchoolYear = scheduleService.getCurrentSchoolYear();
        session.setAttribute("currentSchoolYear", currentSchoolYear);
    }
    final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

    // Load teachers only for the current school year
    List<Teacher> allTeachers = teacherRepo.findAll().stream()
        .filter(teacher -> effectiveCurrentSchoolYear.equals(teacher.getSchoolYear()))
        .collect(Collectors.toList());

    if (allTeachers.isEmpty()) {
        model.addAttribute("error", "No teachers found for the current school year.");
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
                                      @RequestParam(value = "schoolYear", required = false) String schoolYear,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (schoolYear == null || schoolYear.isEmpty()) {
                schoolYear = schoolYearRepository.findAll().stream()
                        .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                        .map(SchoolYear::getYear)
                        .orElseThrow(() -> new IllegalStateException("No current school year found."));
            }
            scheduleService.autoAssignTeachers(section, schoolYear); // Pass schoolYear to the service
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
                                    @RequestParam(value = "schoolYear", required = false) String schoolYear,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (schoolYear == null || schoolYear.isEmpty()) {
                schoolYear = schoolYearRepository.findAll().stream()
                        .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
                        .map(SchoolYear::getYear)
                        .orElseThrow(() -> new IllegalStateException("No current school year found."));
            }
            scheduleService.autoAssignLabRooms(section, schoolYear); // Pass schoolYear to the service
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

    @GetMapping("/allEntities")
    public String showAllEntities(Model model, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "redirect:/signin";
        }

        // Fetch the current school year from the session
        String currentSchoolYear = (String) session.getAttribute("currentSchoolYear");
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }

        // Filter entities based on the previous school year
        String previousSchoolYear = calculatePreviousSchoolYear(currentSchoolYear);

        List<Section> sections = sectionRepository.findAll().stream()
                .filter(section -> previousSchoolYear.equals(section.getSchoolYear()))
                .distinct() // Ensure no duplicates
                .collect(Collectors.toList());

        List<Room> rooms = roomRepository.findAll().stream()
                .filter(room -> previousSchoolYear.equals(room.getSchoolYear()))
                .distinct() // Ensure no duplicates
                .collect(Collectors.toList());

        List<Teacher> teachers = repo.findAll().stream()
                .filter(teacher -> previousSchoolYear.equals(teacher.getSchoolYear()))
                .distinct() // Ensure no duplicates
                .collect(Collectors.toList());

        model.addAttribute("sections", sections);
        model.addAttribute("rooms", rooms);
        model.addAttribute("teachers", teachers);
        return "admin/allEntities";
    }

    private String calculatePreviousSchoolYear(String currentSchoolYear) {
        try {
            String[] years = currentSchoolYear.split("-");
            int startYear = Integer.parseInt(years[0]) - 1;
            int endYear = Integer.parseInt(years[1]) - 1;
            return startYear + "-" + endYear;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid school year format: " + currentSchoolYear);
        }
    }

    @PostMapping("/allEntities/keep")
    public String keepEntitiesForNewSchoolYear(
            @RequestParam(value = "keepSections", required = false) List<Long> keepSectionIds,
            @RequestParam(value = "keepRooms", required = false) List<Long> keepRoomIds,
            @RequestParam(value = "keepTeachers", required = false) List<Integer> keepTeacherIds,
            HttpSession session, RedirectAttributes redirectAttributes) {

        String currentSchoolYear = (String) session.getAttribute("currentSchoolYear");
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }

        // --- Sections ---
        List<Section> allSections = sectionRepository.findAll();
        if (keepSectionIds != null) {
            for (Section section : allSections) {
                if (keepSectionIds.contains(section.getId()) && !currentSchoolYear.equals(section.getSchoolYear())) {
                    // Duplicate or update for new school year
                    Section newSection = new Section();
                    newSection.setName(section.getName());
                    newSection.setSchoolYear(currentSchoolYear);
                    sectionRepository.save(newSection);
                }
            }
        }

        // --- Rooms ---
        List<Room> allRooms = roomRepository.findAll();
        if (keepRoomIds != null) {
            for (Room room : allRooms) {
                if (keepRoomIds.contains(room.getId()) && !currentSchoolYear.equals(room.getSchoolYear())) {
                    Room newRoom = new Room();
                    newRoom.setName(room.getName());
                    newRoom.setLabType(room.getLabType());
                    newRoom.setSchoolYear(currentSchoolYear);
                    roomRepository.save(newRoom);
                }
            }
        }

        // --- Teachers ---
        List<Teacher> allTeachers = repo.findAll();
        if (keepTeacherIds != null) {
            for (Teacher teacher : allTeachers) {
                if (keepTeacherIds.contains(teacher.getId()) && !currentSchoolYear.equals(teacher.getSchoolYear())) {
                    Teacher newTeacher = new Teacher();
                    newTeacher.setFirstName(teacher.getFirstName());
                    newTeacher.setLastName(teacher.getLastName());
                    newTeacher.setMiddleName(teacher.getMiddleName());
                    newTeacher.setGender(teacher.getGender());
                    newTeacher.setBirthDate(teacher.getBirthDate());
                    newTeacher.setContactNumber(teacher.getContactNumber());
                    newTeacher.setEmail(teacher.getEmail());
                    newTeacher.setAddress(teacher.getAddress());
                    newTeacher.setSubjects(teacher.getSubjects());
                    newTeacher.setYearsOfExperience(teacher.getYearsOfExperience());
                    newTeacher.setCreatedAt(teacher.getCreatedAt());
                    newTeacher.setEducationalBackground(teacher.getEducationalBackground());
                    newTeacher.setSchoolYear(currentSchoolYear);
                    repo.save(newTeacher);
                }
            }
        }

        // Set the flag to prevent reopening allEntities.html
        String allEntitiesFlag = "allEntitiesShown_" + currentSchoolYear;
        session.setAttribute(allEntitiesFlag, true);

        redirectAttributes.addFlashAttribute("successMessage", "Entities kept for the new school year.");
        return "redirect:/teachers";
    }

}
