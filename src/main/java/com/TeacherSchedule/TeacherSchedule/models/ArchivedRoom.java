package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;

@Entity
@Table(name = "archived_rooms")
public class ArchivedRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "lab_type")
    private String labType;

    public ArchivedRoom() {}

    public ArchivedRoom(Room room) {
        this.name = room.getName();
        this.labType = room.getLabType();
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

    public String getLabType() {
        return labType;
    }

    public void setLabType(String labType) {
        this.labType = labType;
    }
}
