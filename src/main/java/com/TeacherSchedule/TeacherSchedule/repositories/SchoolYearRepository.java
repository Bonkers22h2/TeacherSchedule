package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {
    List<SchoolYear> findAllByOrderByYearAsc();
}
