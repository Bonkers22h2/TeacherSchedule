package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Room;
import com.TeacherSchedule.TeacherSchedule.models.Section;
import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedRoom;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSection;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSchoolYear;
import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSchedule;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedRoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedSectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedSchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedScheduleRepository;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teachers/manage")
public class ManageController {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SchoolYearRepository schoolYearRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ArchivedSectionRepository archivedSectionRepository;

    @Autowired
    private ArchivedRoomRepository archivedRoomRepository;

    @Autowired
    private ArchivedSchoolYearRepository archivedSchoolYearRepository;

    @Autowired
    private ArchivedScheduleRepository archivedScheduleRepository;

    @Autowired
    private ScheduleService scheduleService; // Inject ScheduleService

    private String getCurrentSchoolYear(HttpSession session) {
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        if (sessionSchoolYear == null || sessionSchoolYear.isEmpty()) {
            String currentSchoolYear = scheduleService.getCurrentSchoolYear();
            if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
                throw new IllegalStateException("No current school year found. Please ensure a school year is set.");
            }
            session.setAttribute("currentSchoolYear", currentSchoolYear);
            return currentSchoolYear;
        }
        return sessionSchoolYear;
    }

    @GetMapping
    public String showManagePage(Model model, HttpSession session) {
        String currentSchoolYear = getCurrentSchoolYear(session);

        // Fetch the latest school year
        String latestSchoolYear = schoolYearRepository.findAll().stream()
            .max((sy1, sy2) -> sy1.getYear().compareTo(sy2.getYear()))
            .map(SchoolYear::getYear)
            .orElse("No School Year Available");

        // Filter sections and rooms by the current school year
        List<Section> sections = sectionRepository.findAll().stream()
            .filter(section -> currentSchoolYear.equals(section.getSchoolYear()))
            .collect(Collectors.toList());

        List<Room> rooms = roomRepository.findAll().stream()
            .filter(room -> currentSchoolYear.equals(room.getSchoolYear()))
            .collect(Collectors.toList());

        model.addAttribute("sections", sections);
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", rooms);
        model.addAttribute("currentSchoolYear", currentSchoolYear);
        model.addAttribute("latestSchoolYear", latestSchoolYear);

        return "admin/manage"; // Return the view for the manage page
    }

    @PostMapping("/saveSection")
    public String saveSection(@RequestParam String section, HttpSession session, RedirectAttributes redirectAttributes) {
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        if (sectionRepository.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(section) && s.getSchoolYear().equals(effectiveCurrentSchoolYear))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Section already exists for the current school year.");
            return "redirect:/teachers/manage";
        }

        Section newSection = new Section();
        newSection.setName(section);
        newSection.setSchoolYear(effectiveCurrentSchoolYear); // Set the current school year
        sectionRepository.save(newSection); // Save the section to the database
        redirectAttributes.addFlashAttribute("successMessage", "Section added successfully.");
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveSchoolYear")
    public String saveSchoolYear(@RequestParam String schoolYear, RedirectAttributes redirectAttributes) {
        if (schoolYearRepository.findAll().stream().anyMatch(sy -> sy.getYear().equalsIgnoreCase(schoolYear))) {
            redirectAttributes.addFlashAttribute("errorMessage", "School Year already exists.");
            return "redirect:/teachers/manage";
        }
        SchoolYear newSchoolYear = new SchoolYear();
        newSchoolYear.setYear(schoolYear);
        schoolYearRepository.save(newSchoolYear); // Save the school year to the database
        redirectAttributes.addFlashAttribute("successMessage", "School Year added successfully.");
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveRoom")
    public String saveRoom(@RequestParam String room, @RequestParam(required = false) String labType, HttpSession session, RedirectAttributes redirectAttributes) {
        String sessionSchoolYear = (String) session.getAttribute("currentSchoolYear");
        String currentSchoolYear = sessionSchoolYear;
        if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
            currentSchoolYear = scheduleService.getCurrentSchoolYear();
            session.setAttribute("currentSchoolYear", currentSchoolYear);
        }
        final String effectiveCurrentSchoolYear = currentSchoolYear; // Make it effectively final

        if (roomRepository.findAll().stream().anyMatch(r -> r.getName().equalsIgnoreCase(room) && r.getSchoolYear().equals(effectiveCurrentSchoolYear))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room already exists for the current school year.");
            return "redirect:/teachers/manage";
        }

        Room newRoom = new Room();
        newRoom.setName(room);
        newRoom.setLabType(labType); // Save the lab type if provided
        newRoom.setSchoolYear(effectiveCurrentSchoolYear); // Set the current school year
        roomRepository.save(newRoom); // Save the room to the database
        redirectAttributes.addFlashAttribute("successMessage", "Room added successfully.");
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editRoom")
    public String editRoom(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room not found.");
            return "redirect:/teachers/manage";
        }

        // Check if the room is referenced in schedules
        boolean isReferenced = scheduleService.getAllSchedules().stream()
                .anyMatch(schedule -> room.getName().equals(schedule.getRoom()) || room.getName().equals(schedule.getLabRoom()));
        if (isReferenced) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. Room is currently in use in schedules.");
            return "redirect:/teachers/manage";
        }

        model.addAttribute("room", room);
        return "admin/editRoom";
    }

    @PostMapping("/editRoom")
    public String updateRoom(@ModelAttribute Room room, RedirectAttributes redirectAttributes) {
        // Check for duplicate room name
        boolean isDuplicate = roomRepository.findAll().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(room.getName()) && !r.getId().equals(room.getId()));
        if (isDuplicate) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. A room with the same name already exists.");
            return "redirect:/teachers/manage";
        }

        roomRepository.save(room);
        redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully.");
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveRoom")
    public String archiveRoom(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null) {
            // Check if the room or lab room is referenced in the schedules table
            boolean isReferenced = scheduleService.getAllSchedules().stream()
                    .anyMatch(schedule -> room.getName().equals(schedule.getRoom()) || room.getName().equals(schedule.getLabRoom()));
            if (isReferenced) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot archive room. It is currently in use in the schedules.");
                return "redirect:/teachers/manage";
            }

            archivedRoomRepository.save(new ArchivedRoom(room));
            roomRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room archived successfully.");
        }
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editSchoolYear")
    public String editSchoolYear(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        SchoolYear schoolYear = schoolYearRepository.findById(id).orElse(null);
        if (schoolYear == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "School Year not found.");
            return "redirect:/teachers/manage";
        }

        // Check if the school year is referenced in schedules
        boolean isReferenced = scheduleService.getAllSchedules().stream()
                .anyMatch(schedule -> schoolYear.getYear().equals(schedule.getSchoolYear()));
        if (isReferenced) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. School Year is currently in use in schedules.");
            return "redirect:/teachers/manage";
        }

        model.addAttribute("schoolYear", schoolYear);
        return "admin/editSchoolYear";
    }

    @PostMapping("/editSchoolYear")
    public String updateSchoolYear(@ModelAttribute SchoolYear schoolYear, RedirectAttributes redirectAttributes) {
        // Check for duplicate school year
        boolean isDuplicate = schoolYearRepository.findAll().stream()
                .anyMatch(sy -> sy.getYear().equalsIgnoreCase(schoolYear.getYear()) && !sy.getId().equals(schoolYear.getId()));
        if (isDuplicate) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. A school year with the same name already exists.");
            return "redirect:/teachers/manage";
        }

        schoolYearRepository.save(schoolYear);
        redirectAttributes.addFlashAttribute("successMessage", "School Year updated successfully.");
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveSchoolYear")
    public String archiveSchoolYear(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        SchoolYear schoolYear = schoolYearRepository.findById(id).orElse(null);
        if (schoolYear != null) {
            // Check if the school year is referenced in the schedules table
            boolean isReferenced = scheduleService.getAllSchedules().stream()
                    .anyMatch(schedule -> schoolYear.getYear().equals(schedule.getSchoolYear()));
            if (isReferenced) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot archive school year. It is currently in use in the schedules.");
                return "redirect:/teachers/manage";
            }

            archivedSchoolYearRepository.save(new ArchivedSchoolYear(schoolYear));
            schoolYearRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "School year archived successfully.");
        }
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editSection")
    public String editSection(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Section section = sectionRepository.findById(id).orElse(null);
        if (section == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Section not found.");
            return "redirect:/teachers/manage";
        }

        // Check if the section is referenced in schedules
        boolean isReferenced = scheduleService.getAllSchedules().stream()
                .anyMatch(schedule -> section.getName().equals(schedule.getSection()));
        if (isReferenced) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. Section is currently in use in schedules.");
            return "redirect:/teachers/manage";
        }

        model.addAttribute("section", section);
        return "admin/editSection";
    }

    @PostMapping("/editSection")
    public String updateSection(@ModelAttribute Section section, RedirectAttributes redirectAttributes) {
        // Check for duplicate section name
        boolean isDuplicate = sectionRepository.findAll().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(section.getName()) && !s.getId().equals(section.getId()));
        if (isDuplicate) {
            redirectAttributes.addFlashAttribute("errorMessage", "Edit unavailable. A section with the same name already exists.");
            return "redirect:/teachers/manage";
        }

        sectionRepository.save(section);
        redirectAttributes.addFlashAttribute("successMessage", "Section updated successfully.");
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveSection")
    public String archiveSection(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        Section section = sectionRepository.findById(id).orElse(null);
        if (section != null) {
            // Check if the section is referenced in the schedules table
            boolean isReferenced = scheduleService.getAllSchedules().stream()
                    .anyMatch(schedule -> section.getName().equals(schedule.getSection()));
            if (isReferenced) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot archive section. It is currently in use in the schedules.");
                return "redirect:/teachers/manage";
            }

            archivedSectionRepository.save(new ArchivedSection(section));
            sectionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Section archived successfully.");
        }
        return "redirect:/teachers/manage";
    }

    @PostMapping("/nextSchoolYear")
    public String goToNextSchoolYear(@RequestParam(value = "removeArchivedSchedules", required = false) boolean removeArchivedSchedules,
                                     HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Fetch the current school year
            String currentSchoolYear = (String) session.getAttribute("currentSchoolYear");
            if (currentSchoolYear == null || currentSchoolYear.isEmpty()) {
                currentSchoolYear = scheduleService.getCurrentSchoolYear();
                session.setAttribute("currentSchoolYear", currentSchoolYear);
            }

            // Check if there are any schedules for the current school year
            List<Schedule> currentYearSchedules = scheduleService.getSchedulesBySchoolYear(currentSchoolYear);
            if (currentYearSchedules.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot proceed to the next school year. No schedules exist for the current school year.");
                return "redirect:/teachers/manage";
            }

            if (!removeArchivedSchedules) {
                // Archive all schedules before proceeding to the next school year
                for (Schedule schedule : currentYearSchedules) {
                    ArchivedSchedule archivedSchedule = new ArchivedSchedule(schedule);
                    archivedScheduleRepository.save(archivedSchedule);
                    scheduleService.deleteSchedule(schedule.getId());
                }
            }

            // Parse the current school year and calculate the next one
            String[] years = currentSchoolYear.split("-");
            int startYear = Integer.parseInt(years[0]) + 1;
            int endYear = Integer.parseInt(years[1]) + 1;
            String nextSchoolYear = startYear + "-" + endYear;

            // Save the next school year
            SchoolYear newSchoolYear = new SchoolYear(nextSchoolYear);
            schoolYearRepository.save(newSchoolYear);

            redirectAttributes.addFlashAttribute("successMessage", "Successfully moved to the next school year: " + nextSchoolYear);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to proceed to the next school year: " + e.getMessage());
        }
        return "redirect:/signin";
    }
}
