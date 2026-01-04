package com.binder.demo.classroom;

import java.util.UUID;

/**
 * Represents a classroom within the application.
 *
 * A Classroom is created by a teacher and
 * contains basic info used to identify and describe the class.
 */
public class Classroom {
    /** Unique identifier for the classroom */
    private UUID classId;
    /** Human-readable name of the classroom */
    private String name;
    /** Optional description of the classroom */
    private String description;
    /** Unique identifier of the user who created the classroom */
    private UUID creatorId;

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID creatorId) { this.creatorId = creatorId; }
}
