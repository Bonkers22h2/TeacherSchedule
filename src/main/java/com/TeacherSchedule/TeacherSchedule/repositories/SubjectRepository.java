package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByGradeLevel(String gradeLevel); // Fetch subjects by grade level

    Subject findByNameAndGradeLevel(String name, String gradeLevel); // Fetch subject by name and grade level
}
