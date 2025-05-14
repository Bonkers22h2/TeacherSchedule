package com.TeacherSchedule.TeacherSchedule.services;

import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.models.Room; // Import Room class
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository; // Import RoomRepository
import com.TeacherSchedule.TeacherSchedule.repositories.SubjectRepository; // Import SubjectRepository
import com.TeacherSchedule.TeacherSchedule.models.Subject; // Import Subject class
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    static final int CLASS_DURATION_MINUTES = 60;
    static final int BREAK_DURATION_MINUTES = 30;
    static final int START_HOUR = 7;
    static final int END_HOUR = 18;
    static final int MAX_CLASSES = 6;
    static final int BREAK_SLOT = 3;

    private List<Integer> timeSlots;
    private Random random;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository; // Add TeacherRepository to fetch teacher data

    @Autowired
    private RoomRepository roomRepository; // Inject RoomRepository

    @Autowired
    private SubjectRepository subjectRepository; // Inject SubjectRepository

    private List<String> currentSchedule = new ArrayList<>(); // Store the generated schedule

    public ScheduleService() {
        this.timeSlots = new ArrayList<>();
        this.random = new Random();
    }

    private <T> void shuffleList(List<T> list) {
        Collections.shuffle(list, random);
    }

    private void createTimeSlots(int numClasses) {
        timeSlots.clear();
        for (int i = 0; i < numClasses + 1; i++) { // +1 for possible break
            timeSlots.add(-1);
        }
    }

    // used for backtracking algorithm to schedule classes
    private boolean backtrack(int classIndex, boolean breakScheduled, List<Subject> subjects) {
        if (classIndex == subjects.size()) {
            return true; // All classes have been scheduled
        }
        for (int i = 0; i < timeSlots.size(); i++) {
            // Check if the time slot is available
            if (timeSlots.get(i) == -1) {
                // Check if the class can be scheduled in this time slot
                timeSlots.set(i, classIndex);

                // Check if a break has already been scheduled
                if (!breakScheduled && i == BREAK_SLOT) {
                    timeSlots.set(i, -2); // Schedule a break
                    if (backtrack(classIndex, true, subjects)) // Continue scheduling the same class after the break
                        return true;
                    timeSlots.set(i, -1);
                }

                if (backtrack(classIndex + 1, breakScheduled, subjects)) // Move to the next class
                    return true;

                    // Backtrack if scheduling fails
                timeSlots.set(i, -1); // Backtrack
            }
        }

        return false; // No valid schedule found
    }

    public List<String> generateSchedule(String section, String schoolYear, String gradeLevel) {
        // Check if a schedule already exists for the given section and school year
        if (scheduleRepository.findBySectionAndSchoolYear(section, schoolYear).size() > 0) {
            throw new IllegalStateException(
                    "A schedule already exists for section '" + section + "' in the school year '" + schoolYear + "'.");
        }

        // Fetch subjects dynamically from the database for the selected grade level
        List<Subject> subjects = subjectRepository.findByGradeLevel(gradeLevel);
        if (subjects == null || subjects.isEmpty()) {
            throw new IllegalStateException("No subjects found for grade level: " + gradeLevel);
        }
        shuffleList(subjects);
        createTimeSlots(subjects.size());

        List<String> scheduleOutput = new ArrayList<>();
        Set<String> scheduledSubjects = new HashSet<>();

        if (backtrack(0, false, subjects)) {
            int slotIndex = 0;
            int classCount = 0;
            for (int i = START_HOUR; i < END_HOUR && classCount < subjects.size(); i++) {
                for (int j = 0; j < (60 / CLASS_DURATION_MINUTES); j++) {
                    int timeSlotIndex = slotIndex++;
                    if (timeSlotIndex >= timeSlots.size()) {
                        break;
                    }

                    int startMinute = j * CLASS_DURATION_MINUTES;
                    int endHour = i + (startMinute + CLASS_DURATION_MINUTES) / 60;
                    int endMinute = (startMinute + CLASS_DURATION_MINUTES) % 60;

                    if (timeSlots.get(timeSlotIndex) != -1 && timeSlots.get(timeSlotIndex) != -2) {
                        int subjectIdx = timeSlots.get(timeSlotIndex);
                        Subject scheduledSubject = subjects.get(subjectIdx);

                        if (scheduledSubjects.contains(scheduledSubject.getName())) {
                            continue;
                        }

                        String subjectName = scheduledSubject.getName();
                        String subSubject = scheduledSubject.getSubSubject();

                        String classEntry = String.format("%02d:%02d - %02d:%02d - %s - %s - Section %s",
                                i, startMinute, endHour, endMinute, subjectName,
                                subSubject != null ? subSubject : "None", section);
                        scheduleOutput.add(classEntry);
                        scheduledSubjects.add(subjectName);
                        classCount++;
                    } else if (timeSlots.get(timeSlotIndex) == -2) {
                        scheduleOutput.add(String.format("%02d:%02d - %02d:%02d - Break - Unknown Section",
                                i, startMinute, endHour, endMinute));
                    }
                }
            }
        } else {
            scheduleOutput.add("It is not possible to schedule all classes within the given time frame.");
        }

        currentSchedule = scheduleOutput;
        return scheduleOutput;
    }

    public void saveScheduleWithSubSubjects(String selectedSection, String selectedSchoolYear, String selectedRoom,
            String selectedGradeLevel) {
        if (currentSchedule.isEmpty()) {
            throw new IllegalStateException("No schedule has been generated to save.");
        }

        for (String entry : currentSchedule) {
            String[] parts = entry.split(" - ");
            if (parts.length >= 3) {
                String subject = parts[2];
                String subSubject = null;

                // Fetch sub-subject from the subjects table
                Subject subjectEntity = subjectRepository.findByNameAndGradeLevel(subject, selectedGradeLevel);
                if (subjectEntity != null) {
                    subSubject = subjectEntity.getSubSubject();
                }

                // Save the schedule with the sub-subject
                scheduleRepository.save(new Schedule(parts[0] + " - " + parts[1], subject, selectedSection,
                        selectedSchoolYear, selectedRoom, selectedGradeLevel, subSubject));
            }
        }
    }

    public List<String> getCurrentSchedule() {
        return currentSchedule; // Provide access to the current schedule
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> getSchedulesBySection(String section) {
        return scheduleRepository.findBySection(section);
    }

    public List<Schedule> getSchedulesBySectionAndSchoolYear(String section, String schoolYear) {
        return scheduleRepository.findBySectionAndSchoolYear(section, schoolYear);
    }

    public List<Schedule> getSchedulesByGradeLevel(String gradeLevel) {
        return scheduleRepository.findByGradeLevel(gradeLevel);
    }

    public List<Schedule> getSchedulesBySectionSchoolYearAndGradeLevel(String section, String schoolYear,
            String gradeLevel) {
        return scheduleRepository.findBySectionAndSchoolYearAndGradeLevel(section, schoolYear, gradeLevel);
    }

    public List<Schedule> getSchedulesBySectionAndGradeLevel(String section, String gradeLevel) {
        return scheduleRepository.findBySectionAndGradeLevel(section, gradeLevel);
    }

    public List<Schedule> getFilteredSchedules(String section, String schoolYear, String gradeLevel) {
        if (section != null && schoolYear != null && gradeLevel != null) {
            return scheduleRepository.findBySectionAndSchoolYearAndGradeLevel(section, schoolYear, gradeLevel);
        } else if (section != null && schoolYear != null) {
            return scheduleRepository.findBySectionAndSchoolYear(section, schoolYear);
        } else if (section != null && gradeLevel != null) {
            return scheduleRepository.findBySectionAndGradeLevel(section, gradeLevel);
        } else if (schoolYear != null && gradeLevel != null) {
            return scheduleRepository.findBySchoolYearAndGradeLevel(schoolYear, gradeLevel);
        } else if (section != null) {
            return scheduleRepository.findBySection(section);
        } else if (schoolYear != null) {
            return scheduleRepository.findBySchoolYear(schoolYear);
        } else if (gradeLevel != null) {
            return scheduleRepository.findByGradeLevel(gradeLevel);
        } else {
            return scheduleRepository.findAll(); // Default to all schedules
        }
    } 

    public List<Schedule> getSchedulesByTeacher(Teacher teacher) {
        return scheduleRepository.findByTeacher(teacher);
    }

    public List<Schedule> getSchedulesBySchoolYear(String schoolYear) {
        return scheduleRepository.findBySchoolYear(schoolYear);
    }

    public void autoAssignTeachers(String section, String schoolYear) {
        List<Schedule> schedules = getSchedulesBySectionAndSchoolYear(section, schoolYear);
        if (schedules.isEmpty()) {
            throw new IllegalStateException("No schedules found for the selected section and school year.");
        }

        Set<Integer> assignedHomeroomTeachers = scheduleRepository.findAll().stream()
                .filter(schedule -> "Homeroom".equals(schedule.getSubject()) && schedule.getTeacher() != null)
                .filter(schedule -> schoolYear.equals(schedule.getSchoolYear())) // Restrict to current school year
                .map(schedule -> schedule.getTeacher().getId())
                .collect(Collectors.toSet());

        for (Schedule schedule : schedules) {
            if (schedule.getTeacher() != null) {
                continue;
            }

            String subject = schedule.getSubject();
            List<Teacher> teachers;

            if ("Homeroom".equals(subject)) {
                teachers = teacherRepository.findAll().stream()
                        .filter(teacher -> !assignedHomeroomTeachers.contains(teacher.getId()))
                        .filter(teacher -> schoolYear.equals(teacher.getSchoolYear())) // Restrict to current school year
                        .collect(Collectors.toList());
            } else {
                if (subject.startsWith("TLE")) {
                    subject = "TLE";
                }
                teachers = teacherRepository.findBySubjectsContaining(subject).stream()
                        .filter(teacher -> schoolYear.equals(teacher.getSchoolYear())) // Restrict to current school year
                        .collect(Collectors.toList());
            }

            boolean teacherAssigned = false;

            for (Teacher teacher : teachers) {
                boolean conflict = scheduleRepository.findAll().stream()
                        .filter(s -> schoolYear.equals(s.getSchoolYear())) // Restrict to current school year
                        .anyMatch(s -> s.getTeacher() != null && s.getTeacher().getId() == teacher.getId() &&
                                overlaps(s.getTimeSlot(), schedule.getTimeSlot()));
                if (!conflict) {
                    schedule.setTeacher(teacher);
                    if ("Homeroom".equals(subject)) {
                        assignedHomeroomTeachers.add(teacher.getId());
                    }
                    teacherAssigned = true;
                    break;
                }
            }

            if (!teacherAssigned) {
                schedule.setTeacher(null);
            }

            scheduleRepository.save(schedule);
        }
    }

    public void autoAssignLabRooms(String section, String schoolYear) {
        List<Schedule> schedules = getSchedulesBySectionAndSchoolYear(section, schoolYear);
        List<Room> labRooms = roomRepository.findAll().stream()
                .filter(room -> room.getLabType() != null && !room.getLabType().isEmpty())
                .collect(Collectors.toList());

        if (schedules.isEmpty() || labRooms.isEmpty()) {
            throw new IllegalStateException("No schedules or lab rooms available for assignment.");
        }

        for (Schedule schedule : schedules) {
            String subject = schedule.getSubject();
            Room assignedRoom = null;

            if ("Science".equals(subject)) {
                assignedRoom = findAvailableLabRoom(schedule, labRooms, "Science");
            } else if (schedule.getSubSubject() != null) {
                assignedRoom = findAvailableLabRoom(schedule, labRooms, schedule.getSubSubject());
            }

            if (assignedRoom != null) {
                schedule.setLabRoom(assignedRoom.getName());
                scheduleRepository.save(schedule);
            }
        }
    }

    public String getCurrentSchoolYear() {
        return scheduleRepository.findAll().stream()
                .map(Schedule::getSchoolYear)
                .filter(schoolYear -> schoolYear != null && !schoolYear.isEmpty())
                .max(String::compareTo)
                .orElse("No School Year Available");
    }

    private Room findAvailableLabRoom(Schedule schedule, List<Room> labRooms, String labType) {
        for (Room room : labRooms) {
            // Check if the room's lab type matches the provided labType
            if (labType.equalsIgnoreCase(room.getLabType())) {
                // Check for time slot conflicts
                boolean isAvailable = getAllSchedules().stream()
                        .noneMatch(s -> room.getName().equals(s.getLabRoom()) &&
                                overlaps(s.getTimeSlot(), schedule.getTimeSlot()));
                if (isAvailable) {
                    return room; // Return the first available room
                }
            }
        }
        return null; // No available room found
    }

    private boolean overlaps(String timeSlot1, String timeSlot2) {
        // Assuming time slots are in the format "HH:mm - HH:mm"
        String[] parts1 = timeSlot1.split(" - ");
        String[] parts2 = timeSlot2.split(" - ");

        if (parts1.length < 2 || parts2.length < 2) {
            return false; // Invalid time slot format
        }

        // Parse start and end times
        int start1 = parseTime(parts1[0]);
        int end1 = parseTime(parts1[1]);
        int start2 = parseTime(parts2[0]);
        int end2 = parseTime(parts2[1]);

        // Check for overlap
        return start1 < end2 && start2 < end1;
    }

    private int parseTime(String time) {
        // Convert "HH:mm" to an integer representing minutes since midnight
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }

    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }
}
