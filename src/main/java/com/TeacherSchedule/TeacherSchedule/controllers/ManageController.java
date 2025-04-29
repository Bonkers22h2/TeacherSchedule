package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Room;
import com.TeacherSchedule.TeacherSchedule.models.Section;
import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    @GetMapping
    public String showManagePage() {
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
}
