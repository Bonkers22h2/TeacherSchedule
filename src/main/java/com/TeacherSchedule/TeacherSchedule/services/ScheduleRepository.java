package com.TeacherSchedule.TeacherSchedule.services;

import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    boolean existsByTimeSlotAndClassName(String timeSlot, String className);

    // Find schedules by section
    List<Schedule> findBySection(String section);
}
