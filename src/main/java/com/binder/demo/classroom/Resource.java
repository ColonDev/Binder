package com.binder.demo.classroom;

import com.binder.demo.attachments.Attachment;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "resources")
public class Resource extends ClassroomPost {

    @Id
    @UuidGenerator
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @ManyToMany
    @JoinTable(
            name = "resource_attachments",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    private Set<Attachment> attachments = new HashSet<>();

    public Resource() {
        setPostType(PostType.RESOURCE);
    }

    @PostLoad
    void postLoad() {
        setPostType(PostType.RESOURCE);
    }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    @Override
    public UUID getPostId() { return resourceId; }

    public Set<Attachment> getAttachments() { return attachments; }
    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = (attachments == null) ? new HashSet<>() : attachments;
    }
}
