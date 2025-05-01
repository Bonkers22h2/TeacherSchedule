package com.TeacherSchedule.TeacherSchedule.services;

import com.TeacherSchedule.TeacherSchedule.models.Schedule;
import com.TeacherSchedule.TeacherSchedule.models.Teacher;
import com.TeacherSchedule.TeacherSchedule.models.Room; // Import Room class
import com.TeacherSchedule.TeacherSchedule.repositories.RoomRepository; // Import RoomRepository
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

    private List<Class> classes;
    private List<Integer> timeSlots;
    private Random random;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository; // Add TeacherRepository to fetch teacher data

    @Autowired
    private RoomRepository roomRepository; // Inject RoomRepository

    private List<String> currentSchedule = new ArrayList<>(); // Store the generated schedule

    public ScheduleService() {
        this.classes = new ArrayList<>();
        this.timeSlots = new ArrayList<>();
        this.random = new Random();

        // Updated list of subjects for demo
        classes.add(new Class("Math", 1, 1));
        classes.add(new Class("Filipino", 2, 1));
        classes.add(new Class("AP", 3, 1));
        classes.add(new Class("Science", 4, 1));
        classes.add(new Class("TLE", 5, 1));
        classes.add(new Class("English", 6, 1));
        classes.add(new Class("M.A.P.E.H.", 7, 1));
        classes.add(new Class("Homeroom", 8, 1)); // Added Homeroom subject
    }

    private void shuffleClasses() {
        Collections.shuffle(classes, random);
    }

    private void createTimeSlots() {
        int totalSlots = (END_HOUR - START_HOUR) * 60 / CLASS_DURATION_MINUTES;
        for (int i = 0; i < totalSlots; i++) {
            timeSlots.add(-1);
        }
    }

    private boolean backtrack(int classIndex, boolean breakScheduled) {
        if (classIndex == classes.size()) {
            return true; // All classes have been scheduled
        }

        for (int i = 0; i < timeSlots.size(); i++) {
            if (timeSlots.get(i) == -1) {
                timeSlots.set(i, classIndex);

                if (!breakScheduled && i == BREAK_SLOT) {
                    timeSlots.set(i, -2); // Schedule a break
                    if (backtrack(classIndex, true)) // Continue scheduling the same class after the break
                        return true;
                    timeSlots.set(i, -1);
                }

                if (backtrack(classIndex + 1, breakScheduled)) // Move to the next class
                    return true;

                timeSlots.set(i, -1); // Backtrack
            }
        }

        return false; // No valid schedule found
    }

    public List<String> generateSchedule(String section, String schoolYear, String gradeLevel) {
        // Check if a schedule already exists for the given section and school year
        if (scheduleRepository.findBySectionAndSchoolYear(section, schoolYear).size() > 0) {
            throw new IllegalStateException("A schedule already exists for section '" + section + "' in the school year '" + schoolYear + "'.");
        }

        List<String> scheduleOutput = new ArrayList<>();
        Set<String> scheduledSubjects = new HashSet<>();
        shuffleClasses();
        createTimeSlots();

        if (backtrack(0, false)) {
            int slotIndex = 0;
            for (int i = START_HOUR; i < END_HOUR; i++) {
                for (int j = 0; j < (60 / CLASS_DURATION_MINUTES); j++) {
                    int timeSlotIndex = slotIndex++;
                    if (timeSlotIndex >= timeSlots.size()) {
                        break;
                    }

                    int startMinute = j * CLASS_DURATION_MINUTES;
                    int endHour = i + (startMinute + CLASS_DURATION_MINUTES) / 60;
                    int endMinute = (startMinute + CLASS_DURATION_MINUTES) % 60;

                    if (timeSlots.get(timeSlotIndex) != -1 && timeSlots.get(timeSlotIndex) != -2) {
                        int classIndex = timeSlots.get(timeSlotIndex);
                        Class scheduledClass = classes.get(classIndex);

                        if (scheduledSubjects.contains(scheduledClass.getName())) {
                            continue;
                        }

                        // Add subsubject logic for TLE
                        String subjectName = scheduledClass.getName();
                        if ("TLE".equals(subjectName)) {
                            switch (gradeLevel) {
                                case "Grade 1":
                                case "Grade 2":
                                case "Grade 3":
                                case "Grade 4":
                                    subjectName += " - Agriculture";
                                    break;
                                case "Grade 5":
                                case "Grade 6":
                                    subjectName += " - Industrial Arts";
                                    break;
                                case "Grade 7":
                                case "Grade 8":
                                    subjectName += " - Home Economics";
                                    break;
                                case "Grade 9":
                                case "Grade 10":
                                    subjectName += " - ICT";
                                    break;
                            }
                        }

                        String classEntry = String.format("%02d:%02d - %02d:%02d - %s - Section %d",
                                i, startMinute, endHour, endMinute, subjectName, scheduledClass.getSection());
                        scheduleOutput.add(classEntry);
                        scheduledSubjects.add(scheduledClass.getName());
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

    public void saveScheduleWithSubSubjects(String selectedSection, String selectedSchoolYear, String selectedRoom, String selectedGradeLevel) {
        if (currentSchedule.isEmpty()) {
            throw new IllegalStateException("No schedule has been generated to save.");
        }

        for (String entry : currentSchedule) {
            String[] parts = entry.split(" - ");
            if (parts.length >= 3) {
                String subject = parts[2];
                String subSubject = null;

                // Add subsubject logic for TLE
                if ("TLE".equals(subject)) {
                    switch (selectedGradeLevel) {
                        case "Grade 1":
                        case "Grade 2":
                        case "Grade 3":
                        case "Grade 4":
                            subSubject = "Agriculture";
                            break;
                        case "Grade 5":
                        case "Grade 6":
                            subSubject = "Industrial Arts";
                            break;
                        case "Grade 7":
                        case "Grade 8":
                            subSubject = "Home Economics";
                            break;
                        case "Grade 9":
                        case "Grade 10":
                            subSubject = "ICT";
                            break;
                    }
                    subject += " - " + subSubject; // Append subsubject to the subject
                }

                // Assign laboratory room based on subject
                String labRoom = null;
                if (subSubject != null) {
                    switch (subSubject) {
                        case "ICT":
                            labRoom = roomRepository.findFirstByLabType("ICT").map(Room::getName).orElse("Default ICT Lab");
                            break;
                        case "Home Economics":
                            labRoom = roomRepository.findFirstByLabType("Home Economics").map(Room::getName).orElse("Default Home Economics Lab");
                            break;
                        case "Agriculture":
                            labRoom = roomRepository.findFirstByLabType("Agriculture").map(Room::getName).orElse("Default Agriculture Lab");
                            break;
                        case "Science":
                            labRoom = roomRepository.findFirstByLabType("Science").map(Room::getName).orElse("Default Science Lab");
                            break;
                    }
                }

                // Save the schedule with the lab room
                scheduleRepository.save(new Schedule(parts[0] + " - " + parts[1], subject, selectedSection, selectedSchoolYear, labRoom, selectedGradeLevel, subSubject));
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

    public List<Schedule> getSchedulesBySectionSchoolYearAndGradeLevel(String section, String schoolYear, String gradeLevel) {
        return scheduleRepository.findBySectionAndSchoolYearAndGradeLevel(section, schoolYear, gradeLevel);
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

    public void autoAssignTeachers(String section, String schoolYear, String gradeLevel) {
        List<Schedule> schedules = getFilteredSchedules(section, schoolYear, gradeLevel);
        if (schedules.isEmpty()) {
            throw new IllegalStateException("No schedules found for the selected filters.");
        }

        for (Schedule schedule : schedules) {
            // Skip schedules that already have an assigned teacher
            if (schedule.getTeacher() != null) {
                continue;
            }

            List<Teacher> teachers = teacherRepository.findBySubjectsContaining(schedule.getSubject());
            boolean teacherAssigned = false;

            for (Teacher teacher : teachers) {
                // Ensure the teacher is assigned to the correct grade level
                if (!teacher.getGradeLevels().contains(gradeLevel)) {
                    continue;
                }

                // Check if the teacher is already assigned to a schedule at the same time slot
                boolean conflict = scheduleRepository.existsByTimeSlotAndTeacher(schedule.getTimeSlot(), teacher);
                if (!conflict) {
                    schedule.setTeacher(teacher); // Assign the teacher if no conflict
                    teacherAssigned = true;
                    break;
                }
            }

            if (!teacherAssigned) {
                schedule.setTeacher(null); // Leave the teacher field blank if no suitable teacher is available
            }

            scheduleRepository.save(schedule);
        }
    }

    public void autoAssignLabRooms(String section, String schoolYear, String gradeLevel) {
        List<Schedule> schedules = getFilteredSchedules(section, schoolYear, gradeLevel);
        List<Room> availableRooms = roomRepository.findAll();

        // Filter rooms with lab types
        List<Room> labRooms = availableRooms.stream()
                .filter(room -> room.getLabType() != null && !room.getLabType().isEmpty())
                .collect(Collectors.toList());

        if (schedules.isEmpty() || labRooms.isEmpty()) {
            throw new IllegalStateException("No schedules or lab rooms available for assignment.");
        }

        for (Schedule schedule : schedules) {
            for (Room room : labRooms) {
                if (isLabRoomAvailable(schedule, room)) {
                    schedule.setLabRoom(room.getName());
                    scheduleRepository.save(schedule); // Save the updated schedule
                    break; // Assign only one lab room per schedule
                }
            }
        }
    }

    private boolean isLabRoomAvailable(Schedule schedule, Room room) {
        // Check if the room's lab type matches the subject's lab requirement
        if (!isLabTypeCompatible(schedule.getSubject(), room.getLabType())) {
            return false;
        }

        // Check for time slot conflicts
        List<Schedule> conflictingSchedules = getAllSchedules().stream()
                .filter(s -> room.getName().equals(s.getLabRoom()) &&
                        overlaps(s.getTimeSlot(), schedule.getTimeSlot()))
                .collect(Collectors.toList());

        return conflictingSchedules.isEmpty();
    }

    private boolean isLabTypeCompatible(String subject, String labType) {
        if (subject == null || labType == null) {
            return false;
        }

        // Handle TLE subsubjects
        if (subject.startsWith("TLE - ")) {
            String subSubject = subject.substring(6); // Extract subsubject after "TLE - "
            switch (subSubject) {
                case "ICT":
                    return labType.equalsIgnoreCase("ICT");
                case "Home Economics":
                    return labType.equalsIgnoreCase("Home Economics");
                case "Agriculture":
                    return labType.equalsIgnoreCase("Agriculture");
                default:
                    return false;
            }
        }

        // Handle other subjects
        switch (subject) {
            case "ICT":
                return labType.equalsIgnoreCase("ICT");
            case "Home Economics":
                return labType.equalsIgnoreCase("Home Economics");
            case "Agriculture":
                return labType.equalsIgnoreCase("Agriculture");
            case "Science":
                return labType.equalsIgnoreCase("Science");
            default:
                return false;
        }
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
}

class Class {
    private String name;
    private int section;
    private int id;

    public Class(String name, int id, int section) {
        this.name = name;
        this.id = id;
        this.section = section;
    }

    public String getName() {
        return name;
    }

    public int getSection() {
        return section;
    }

    public int getId() {
        return id;
    }
}
