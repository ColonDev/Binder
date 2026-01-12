package com.binder.demo.attachments;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @Column(name = "attachment_id", nullable = false)
    private UUID attachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false)
    private AttachmentType attachmentType;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "user_owner", nullable = false)
    private UUID userOwner;

    @PrePersist
    void prePersist() {
        if (attachmentId == null) attachmentId = UUID.randomUUID();
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }

    public AttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(AttachmentType attachmentType) { this.attachmentType = attachmentType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public UUID getUserOwner() { return userOwner; }
    public void setUserOwner(UUID userOwner) { this.userOwner = userOwner; }
}
