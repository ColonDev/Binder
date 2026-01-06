package com.binder.demo.classroom;

import com.binder.demo.attachments.Attachment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generic container class representing a post in a classroom.
 */
public abstract class ClassroomPost {

    private UUID id;
    private UUID classId;
    private String title;
    private String description;
    private Instant createdTime;
    private UUID creatorTeacherId;

    /* Used by the UI to decide how to render the post */
    private PostType postType;

    /* Populated by the service when fetching posts */
    private List<Attachment> attachments = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }

    public UUID getCreatorTeacherId() { return creatorTeacherId; }
    public void setCreatorTeacherId(UUID creatorTeacherId) { this.creatorTeacherId = creatorTeacherId; }

    public PostType getPostType() { return postType; }
    protected void setPostType(PostType postType) { this.postType = postType; }

    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = (attachments == null) ? new ArrayList<>() : attachments;
    }
}
