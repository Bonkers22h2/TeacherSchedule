package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "schedules") // Specifies the table name
public class Schedule {

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
    @JoinColumn(name = "teacher_id") // Maps to the teacher table
    private Teacher teacher; // New field for the assigned teacher

    // Default constructor
    public Schedule() {}

    // Constructor with all fields except teacher
    public Schedule(String timeSlot, String subject, String section, String schoolYear, String room, String gradeLevel, String subSubject) {
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.section = section;
        this.schoolYear = schoolYear;
        this.room = room;
        this.gradeLevel = gradeLevel;
        this.subSubject = subSubject;
    }

    // Getters and setters
    public Long getId() {
        return id;
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
