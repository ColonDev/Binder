package com.binder.demo.classroom;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;


/**
 * A container for a classroom.
 * Each classroom has a unique ID, name, description, and creation timestamp.
 */
@Entity
@Table(name = "classrooms")
public class Classroom {

    @Id
    @UuidGenerator
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
}
