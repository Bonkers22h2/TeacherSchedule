package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
public class SchoolYear {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String year;

    public SchoolYear() {
        // Default constructor
    }

    public SchoolYear(String year) {
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}