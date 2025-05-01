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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public String showManagePage(Model model) {
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("schoolYears", schoolYearRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "admin/manage"; // Return the view for the manage page
    }

    @PostMapping("/saveSection")
    public String saveSection(@RequestParam String section) {
        Section newSection = new Section();
        newSection.setName(section);
        sectionRepository.save(newSection); // Save the section to the database
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveSchoolYear")
    public String saveSchoolYear(@RequestParam String schoolYear) {
        SchoolYear newSchoolYear = new SchoolYear();
        newSchoolYear.setYear(schoolYear);
        schoolYearRepository.save(newSchoolYear); // Save the school year to the database
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveRoom")
    public String saveRoom(@RequestParam String room, @RequestParam(required = false) String labType) {
        Room newRoom = new Room();
        newRoom.setName(room);
        newRoom.setLabType(labType); // Save the lab type if provided
        roomRepository.save(newRoom); // Save the room to the database
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editRoom")
    public String editRoom(@RequestParam("id") Long id, Model model) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) {
            model.addAttribute("errorMessage", "Room not found.");
            return "redirect:/teachers/manage";
        }
        model.addAttribute("room", room);
        return "admin/editRoom"; // Ensure this view exists
    }

    @PostMapping("/editRoom")
    public String updateRoom(@ModelAttribute Room room) {
        roomRepository.save(room);
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveRoom")
    public String archiveRoom(@RequestParam("id") Long id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null) {
            archivedRoomRepository.save(new ArchivedRoom(room));
            roomRepository.deleteById(id);
        }
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editSchoolYear")
    public String editSchoolYear(@RequestParam("id") Long id, Model model) {
        SchoolYear schoolYear = schoolYearRepository.findById(id).orElse(null);
        if (schoolYear == null) {
            model.addAttribute("errorMessage", "School Year not found.");
            return "redirect:/teachers/manage";
        }
        model.addAttribute("schoolYear", schoolYear);
        return "admin/editSchoolYear"; // Ensure this view exists
    }

    @PostMapping("/editSchoolYear")
    public String updateSchoolYear(@ModelAttribute SchoolYear schoolYear) {
        schoolYearRepository.save(schoolYear);
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveSchoolYear")
    public String archiveSchoolYear(@RequestParam("id") Long id) {
        SchoolYear schoolYear = schoolYearRepository.findById(id).orElse(null);
        if (schoolYear != null) {
            archivedSchoolYearRepository.save(new ArchivedSchoolYear(schoolYear));
            schoolYearRepository.deleteById(id);
        }
        return "redirect:/teachers/manage";
    }

    @GetMapping("/editSection")
    public String editSection(@RequestParam("id") Long id, Model model) {
        Section section = sectionRepository.findById(id).orElse(null);
        if (section == null) {
            model.addAttribute("errorMessage", "Section not found.");
            return "redirect:/teachers/manage";
        }
        model.addAttribute("section", section);
        return "admin/editSection"; // Ensure this view exists
    }

    @PostMapping("/editSection")
    public String updateSection(@ModelAttribute Section section) {
        sectionRepository.save(section);
        return "redirect:/teachers/manage";
    }

    @GetMapping("/archiveSection")
    public String archiveSection(@RequestParam("id") Long id) {
        Section section = sectionRepository.findById(id).orElse(null);
        if (section != null) {
            archivedSectionRepository.save(new ArchivedSection(section));
            sectionRepository.deleteById(id);
        }
        return "redirect:/teachers/manage";
    }
}
