package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
}
