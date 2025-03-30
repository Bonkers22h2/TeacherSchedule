package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "schedules") // Specifies the table name
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String timeSlot;
    private String className;
    private String section; // Column for the section

    public Schedule() {}

    public Schedule(String timeSlot, String className) {
        this.timeSlot = timeSlot;
        this.className = className;
    }

    public Schedule(String timeSlot, String className, String section) {
        this.timeSlot = timeSlot;
        this.className = className;
        this.section = section;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
