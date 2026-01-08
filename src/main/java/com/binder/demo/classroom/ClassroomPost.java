package com.binder.demo.classroom;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class ClassroomPost {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "creator_teacher_id", nullable = false)
    private UUID creatorTeacherId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private PostType postType;

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getCreatorTeacherId() { return creatorTeacherId; }
    public void setCreatorTeacherId(UUID creatorTeacherId) { this.creatorTeacherId = creatorTeacherId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public PostType getPostType() { return postType; }
    protected void setPostType(PostType postType) { this.postType = postType; }
}
