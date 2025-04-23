package com.TeacherSchedule.TeacherSchedule.repositories;

import com.TeacherSchedule.TeacherSchedule.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
