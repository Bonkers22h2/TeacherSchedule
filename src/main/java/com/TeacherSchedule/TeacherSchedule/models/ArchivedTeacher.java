package com.TeacherSchedule.TeacherSchedule.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "archived_teachers")
public class ArchivedTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String firstName;
    private String lastName;
    private String middleName;
    private String gender;
    private LocalDate birthDate;

    private String contactNumber;
    private String email;
    private String address;

    private String subjects;
    private String gradeLevels;
    private int yearsOfExperience;

    private LocalDate createdAt;

    public ArchivedTeacher() {}

    public ArchivedTeacher(Teacher teacher) {
        this.firstName = teacher.getFirstName();
        this.lastName = teacher.getLastName();
        this.middleName = teacher.getMiddleName();
        this.gender = teacher.getGender();
        this.birthDate = teacher.getBirthDate();
        this.contactNumber = teacher.getContactNumber();
        this.email = teacher.getEmail();
        this.address = teacher.getAddress();
        this.subjects = teacher.getSubjects();
        this.gradeLevels = teacher.getGradeLevels();
        this.yearsOfExperience = teacher.getYearsOfExperience();
        this.createdAt = teacher.getCreatedAt();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getGradeLevels() {
        return gradeLevels;
    }

    public void setGradeLevels(String gradeLevels) {
        this.gradeLevels = gradeLevels;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
