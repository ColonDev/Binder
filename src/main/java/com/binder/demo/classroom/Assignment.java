package com.binder.demo.classroom;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic container class representing an Assignment in a classroom.
 */
public class Assignment extends ClassroomPost {

    private String timeToComplete;
    private Instant dueDate;
    private Integer maxMarks;

    public Assignment() {
        setPostType("ASSIGNMENT");
    }

    /* Convenience constructor if you want one */
    public Assignment(UUID assignmentId, UUID classId, String title, String description,
                      Instant createdTime, UUID creatorTeacherId,
                      String timeToComplete, Instant dueDate, Integer maxMarks) {
        setPostType("ASSIGNMENT");
        setId(assignmentId);
        setClassId(classId);
        setTitle(title);
        setDescription(description);
        setCreatedTime(createdTime);
        setCreatorTeacherId(creatorTeacherId);
        this.timeToComplete = timeToComplete;
        this.dueDate = dueDate;
        this.maxMarks = maxMarks;
    }

    public String getTimeToComplete() { return timeToComplete; }
    public void setTimeToComplete(String timeToComplete) { this.timeToComplete = timeToComplete; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
}
