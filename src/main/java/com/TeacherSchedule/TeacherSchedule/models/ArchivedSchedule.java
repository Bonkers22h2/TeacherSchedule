package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "archived_schedules")
public class ArchivedSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String timeSlot;
    private String subject;
    private String section;
    private String schoolYear;
    private String room;
    private String gradeLevel;
    private String subSubject;
    private String labRoom;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    public ArchivedSchedule() {}

    public ArchivedSchedule(Schedule schedule) {
        this.timeSlot = schedule.getTimeSlot();
        this.subject = schedule.getSubject();
        this.section = schedule.getSection();
        this.schoolYear = schedule.getSchoolYear();
        this.room = schedule.getRoom();
        this.gradeLevel = schedule.getGradeLevel();
        this.subSubject = schedule.getSubSubject();
        this.labRoom = schedule.getLabRoom();
        this.teacher = schedule.getTeacher();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getSubSubject() {
        return subSubject;
    }

    public void setSubSubject(String subSubject) {
        this.subSubject = subSubject;
    }

    public String getLabRoom() {
        return labRoom;
    }

    public void setLabRoom(String labRoom) {
        this.labRoom = labRoom;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}