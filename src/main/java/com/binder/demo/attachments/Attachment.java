package com.binder.demo.attachments;

import java.time.Instant;
import java.util.UUID;

/*
    Attachment
    Represents an uploaded file, image, or link stored in the attachments table.
 */
public class Attachment {

    private UUID attachmentId;
    private String attachmentType;   // e.g. FILE / LINK / IMAGE (matches DB constraint)
    private String url;
    private Instant uploadedAt;
    private UUID userOwner;

    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public UUID getUserOwner() { return userOwner; }
    public void setUserOwner(UUID userOwner) { this.userOwner = userOwner; }
}
