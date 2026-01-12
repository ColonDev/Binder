package com.binder.demo.services;

import com.binder.demo.attachments.Attachment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AttachmentService {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(^|[\\\\/])\\.\\.([\\\\/]|$)");

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Attachment upload(Attachment attachment) {
        validateAttachment(attachment);
        if (attachment.getAttachmentId() == null) {
            attachment.setAttachmentId(UUID.randomUUID());
        }
        if (attachment.getUploadedAt() == null) {
            attachment.setUploadedAt(Instant.now());
        }
        em.persist(attachment);
        return attachment;
    }

    @Transactional(readOnly = true)
    public Optional<Attachment> get(UUID attachmentId) {
        if (attachmentId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Attachment.class, attachmentId));
    }

    @Transactional
    public Attachment update(Attachment attachment) {
        validateAttachment(attachment);
        if (attachment.getAttachmentId() == null) {
            if (attachment.getUploadedAt() == null) {
                attachment.setUploadedAt(Instant.now());
            }
            attachment.setAttachmentId(UUID.randomUUID());
            em.persist(attachment);
            return attachment;
        }
        return em.merge(attachment);
    }

    @Transactional
    public void delete(UUID attachmentId) {
        if (attachmentId == null) return;
        Attachment attachment = em.find(Attachment.class, attachmentId);
        if (attachment != null) {
            em.remove(attachment);
        }
    }

    private void validateAttachment(Attachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("Attachment is required.");
        }
        String url = attachment.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Attachment URL is required.");
        }
        if (PATH_TRAVERSAL.matcher(url).find() || url.startsWith("file:")) {
            throw new IllegalArgumentException("Potential path traversal in attachment URL.");
        }
    }
}
