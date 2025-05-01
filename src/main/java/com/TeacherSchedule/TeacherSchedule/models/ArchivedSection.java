package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "archived_sections")
public class ArchivedSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public ArchivedSection() {}

    public ArchivedSection(Section section) {
        this.name = section.getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
