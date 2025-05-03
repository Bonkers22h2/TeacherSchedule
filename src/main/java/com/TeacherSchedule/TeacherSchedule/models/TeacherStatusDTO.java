package com.TeacherSchedule.TeacherSchedule.models;

public class TeacherStatusDTO {
    private Teacher teacher;
    private String status;

    public TeacherStatusDTO(Teacher teacher, String status) {
        this.teacher = teacher;
        this.status = status;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
