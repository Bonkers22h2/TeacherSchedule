package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Room;
import com.TeacherSchedule.TeacherSchedule.models.Section;
import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedRoom;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSection;
import com.TeacherSchedule.TeacherSchedule.models.ArchivedSchoolYear;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedRoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedSectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.ArchivedSchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.services.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private ScheduleService scheduleService; // Inject ScheduleService

    @GetMapping
    public String showManagePage(Model model) {
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "admin/manage"; // Return the view for the manage page
    }

    @PostMapping("/saveSection")
    public String saveSection(@RequestParam String section, RedirectAttributes redirectAttributes) {
        if (sectionRepository.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(section))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Section already exists.");
            return "redirect:/teachers/manage";
        }
        Section newSection = new Section();
        newSection.setName(section);
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
    public String saveRoom(@RequestParam String room, @RequestParam(required = false) String labType, RedirectAttributes redirectAttributes) {
        if (roomRepository.findAll().stream().anyMatch(r -> r.getName().equalsIgnoreCase(room))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Room already exists.");
            return "redirect:/teachers/manage";
        }
        Room newRoom = new Room();
        newRoom.setName(room);
        newRoom.setLabType(labType); // Save the lab type if provided
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
}
