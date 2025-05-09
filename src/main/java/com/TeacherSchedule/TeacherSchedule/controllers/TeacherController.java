package com.TeacherSchedule.TeacherSchedule.controllers;

import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.services.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    @GetMapping("/search")
    public Teacher searchTeacher(@RequestParam String name) {
        Optional<Teacher> teacher = teacherRepository.findAll().stream()
                .filter(t -> (t.getFirstName() + " " + t.getLastName()).equalsIgnoreCase(name))
                .findFirst();

        if (teacher.isPresent()) {
            return teacher.get();
        } else {
            throw new IllegalArgumentException("Teacher not found");
        }
    }

    @GetMapping("/suggestions")
    public List<Teacher> getTeacherSuggestions(@RequestParam String query) {
        return teacherRepository.findAll().stream()
                .filter(teacher -> (teacher.getFirstName() + " " + teacher.getLastName()).toLowerCase().contains(query.toLowerCase()))
                .limit(10) // Limit to 10 suggestions
                .toList();
    }

    @PutMapping("/{id}")
    public Teacher updateTeacher(@PathVariable int id, @RequestBody Teacher updatedTeacher) {
        return teacherRepository.findById(id).map(teacher -> {
            teacher.setFirstName(updatedTeacher.getFirstName());
            teacher.setLastName(updatedTeacher.getLastName());
            teacher.setMiddleName(updatedTeacher.getMiddleName());
            teacher.setGender(updatedTeacher.getGender());
            teacher.setBirthDate(updatedTeacher.getBirthDate());
            teacher.setContactNumber(updatedTeacher.getContactNumber());
            teacher.setEmail(updatedTeacher.getEmail());
            teacher.setAddress(updatedTeacher.getAddress());
            teacher.setSubjects(updatedTeacher.getSubjects());
            teacher.setYearsOfExperience(updatedTeacher.getYearsOfExperience());
            return teacherRepository.save(teacher);
        }).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteTeacher(@PathVariable int id) {
        if (!teacherRepository.existsById(id)) {
            throw new IllegalArgumentException("Teacher not found");
        }
        teacherRepository.deleteById(id);
    }
}
