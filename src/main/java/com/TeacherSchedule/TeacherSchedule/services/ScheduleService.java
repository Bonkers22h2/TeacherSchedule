package com.TeacherSchedule.TeacherSchedule.services;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    public ScheduleService() {
        this.classes = new ArrayList<>();
        this.timeSlots = new ArrayList<>();
        this.random = new Random();

        // Sample classes for demo
        classes.add(new Class("Math", 1, 1));
        classes.add(new Class("Science", 2, 1));
        classes.add(new Class("English", 3, 1));
        classes.add(new Class("Filipino", 4, 1));
        classes.add(new Class("Homeroom", 5, 1));
        classes.add(new Class("ESP", 6, 1));
        classes.add(new Class("TLE", 7, 1));
        classes.add(new Class("AP", 8, 1));
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
            return true;
        }

        Class currentClass = classes.get(classIndex);

        for (int i = 0; i < timeSlots.size(); i++) {
            if (timeSlots.get(i) == -1) {
                timeSlots.set(i, classIndex);

                if (!breakScheduled && i == BREAK_SLOT) {
                    timeSlots.set(i, -2);
                    if (backtrack(classIndex + 1, true))
                        return true;
                    timeSlots.set(i, -1);
                }

                if (backtrack(classIndex + 1, breakScheduled))
                    return true;

                timeSlots.set(i, -1);
            }
        }

        return false;
    }

    public List<String> generateSchedule() {
        List<String> scheduleOutput = new ArrayList<>();
        Set<String> scheduledClasses = new HashSet<>(); // Track scheduled classes

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
                        Class scheduledClass = classes.get(timeSlots.get(timeSlotIndex));
                        String classEntry = scheduledClass.getName() + " (Section " + scheduledClass.getSection() + ")";

                        // Only add non-duplicates
                        if (!scheduledClasses.contains(classEntry)) {
                            scheduleOutput.add(i + ":00 - " + endHour + ":00 - " + classEntry);
                            scheduledClasses.add(classEntry);
                        }
                    } else if (timeSlots.get(timeSlotIndex) == -2) {
                        scheduleOutput.add(i + ":00 - " + endHour + ":00 - Break");
                    }
                }
            }
        } else {
            scheduleOutput.add("It is not possible to schedule all classes within the given time frame.");
        }

        return scheduleOutput;
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
