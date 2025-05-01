package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "archived_school_years")
public class ArchivedSchoolYear {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String year;

    public ArchivedSchoolYear() {}

    public ArchivedSchoolYear(SchoolYear schoolYear) {
        this.year = schoolYear.getYear();
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
