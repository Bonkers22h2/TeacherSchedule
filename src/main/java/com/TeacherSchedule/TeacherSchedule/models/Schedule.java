package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "schedules") // Specifies the table name
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String timeSlot;
    private String subject; // Renamed from className to subject
    private String section; // Column for the section
    private String schoolYear; // Added schoolYear field
    private String room; // Added room field
    private String gradeLevel; // Added gradeLevel field
    private String subSubject; // Added field for subsubject

    public String getSubSubject() {
        return subSubject;
    }

    public void setSubSubject(String subSubject) {
        this.subSubject = subSubject;
    }

    // Update constructor to include subSubject
    public Schedule(String timeSlot, String subject, String section, String schoolYear, String room, String gradeLevel, String subSubject) {
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.section = section;
        this.schoolYear = schoolYear;
        this.room = room;
        this.gradeLevel = gradeLevel;
        this.subSubject = subSubject;
    }

    @ManyToOne
    @JoinColumn(name = "teacher_id") // Ensure this maps to the correct column in the database
    private Teacher teacher;

    public Schedule() {}

    public Schedule(String timeSlot, String subject) { // Updated parameter name
        this.timeSlot = timeSlot;
        this.subject = subject;
    }

    public Schedule(String timeSlot, String subject, String section) { // Updated parameter name
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.section = section;
    }

    public Schedule(String timeSlot, String subject, String section, String schoolYear) {
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.section = section;
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

    // Update constructor to include room
    public Schedule(String timeSlot, String subject, String section, String schoolYear, String room, String gradeLevel) {
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.section = section;
        this.schoolYear = schoolYear;
        this.room = room;
        this.gradeLevel = gradeLevel;
    }

    public Long getId() {
        return id;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getSubject() { // Updated getter name
        return subject;
    }

    public void setSubject(String subject) { // Updated setter name
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

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}
