package com.TeacherSchedule.TeacherSchedule.services;

import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    boolean existsByTimeSlotAndSubject(String timeSlot, String subject); // Updated from className to subject

    // Find schedules by section
    List<Schedule> findBySection(String section);

    // Find schedules by section and school year
    List<Schedule> findBySectionAndSchoolYear(String section, String schoolYear);

    // Check if a teacher is already assigned to a schedule at a specific time slot
    boolean existsByTimeSlotAndTeacher(String timeSlot, Teacher teacher);
}
