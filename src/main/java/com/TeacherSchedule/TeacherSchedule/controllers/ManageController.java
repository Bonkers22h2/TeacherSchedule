package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Room;
import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import com.TeacherSchedule.TeacherSchedule.models.Section;
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SchoolYearRepository;
import com.TeacherSchedule.TeacherSchedule.repositories.SectionRepository;
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
        return "admin/manage";
    }

    @PostMapping("/saveSection")
    public String saveSection(@RequestParam String section) {
        sectionRepository.save(new Section(section));
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveSchoolYear")
    public String saveSchoolYear(@RequestParam String schoolYear) {
        schoolYearRepository.save(new SchoolYear(schoolYear));
        return "redirect:/teachers/manage";
    }

    @PostMapping("/saveRoom")
    public String saveRoom(@RequestParam String room) {
        roomRepository.save(new Room(room));
        return "redirect:/teachers/manage";
    }
}
