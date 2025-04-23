package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {
}
