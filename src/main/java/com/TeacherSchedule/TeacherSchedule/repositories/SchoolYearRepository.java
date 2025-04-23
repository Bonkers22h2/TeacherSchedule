package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {
}
