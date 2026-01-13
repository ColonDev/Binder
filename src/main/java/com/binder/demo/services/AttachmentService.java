package com.binder.demo.services;

import com.binder.demo.attachments.Attachment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.binder.demo.attachments.AttachmentType;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Handles create, update, lookup, and removal operations for attachments.
 */
@Service
public class AttachmentService {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(^|[\\\\/])\\.\\.([\\\\/]|$)");
    private static final int MAX_URL_LENGTH = 2048;

    /**
     * JPA entity manager used for attachment persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Persists a new attachment after validation and populating missing fields.
     * Generates an attachment ID and upload timestamp when absent.
     *
     * @param attachment attachment to persist
     * @return persisted attachment
     */
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

    /**
     * Retrieves an attachment by ID.
     *
     * @param attachmentId attachment id to find
     * @return optional attachment
     */
    @Transactional(readOnly = true)
    public Optional<Attachment> get(UUID attachmentId) {
        if (attachmentId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Attachment.class, attachmentId));
    }

    /**
     * Updates an attachment, persisting a new row when the ID is missing.
     *
     * @param attachment attachment to update
     * @return updated attachment
     */
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

    /**
     * Deletes an attachment if present.
     *
     * @param attachmentId attachment id to delete
     */
    @Transactional
    public void delete(UUID attachmentId) {
        if (attachmentId == null) return;
        Attachment attachment = em.find(Attachment.class, attachmentId);
        if (attachment != null) {
            em.remove(attachment);
        }
    }

    /**
     * Validates required fields and performs basic URL safety checks.
     *
     * @param attachment attachment to validate
     * @throws IllegalArgumentException when required fields are missing or unsafe
     */
    private void validateAttachment(Attachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("Attachment is required.");
        }
        if (attachment.getAttachmentType() == null) {
            throw new IllegalArgumentException("Attachment type is required.");
        }
        if (attachment.getUserOwner() == null) {
            throw new IllegalArgumentException("Attachment owner is required.");
        }
        String url = attachment.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Attachment URL is required.");
        }
        String trimmedUrl = url.trim();
        if (trimmedUrl.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Attachment URL is too long.");
        }
        for (int i = 0; i < trimmedUrl.length(); i++) {
            char ch = trimmedUrl.charAt(i);
            if (ch <= 0x1F || ch == 0x7F) {
                throw new IllegalArgumentException("Attachment URL contains control characters.");
            }
        }
        if (trimmedUrl.contains("\\")) {
            throw new IllegalArgumentException("Attachment URL contains invalid characters.");
        }
        if (PATH_TRAVERSAL.matcher(trimmedUrl).find() || PATH_TRAVERSAL.matcher(decodeUrl(trimmedUrl)).find()) {
            throw new IllegalArgumentException("Potential path traversal in attachment URL.");
        }
        URI uri = parseUri(trimmedUrl);
        if (uri.isAbsolute()) {
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Unsupported attachment URL scheme.");
            }
        } else if (attachment.getAttachmentType() != AttachmentType.FILE
                && attachment.getAttachmentType() != AttachmentType.IMAGE) {
            throw new IllegalArgumentException("Attachment URL must be absolute for this type.");
        }
    }

    private static URI parseUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Attachment URL is invalid.", ex);
        }
    }

    /**
     * Decodes a URL for additional traversal checks.
     *
     * @param url raw URL value
     * @return decoded URL, or original input if decoding fails
     */
    private static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return url;
        }
    }

    /**
     * Extracts the filename from a URL or path.
     *
     * @param url attachment URL or path
     * @return filename portion or a fallback label
     */
    public static String filenameFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "attachment";
        }
        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed);
            String query = uri.getQuery();
            String nameFromQuery = queryFilename(query);
            if (nameFromQuery != null && !nameFromQuery.isBlank()) {
                return nameFromQuery;
            }
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                String name = slash >= 0 ? path.substring(slash + 1) : path;
                if (!name.isBlank()) {
                    return name;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to decoding below.
        }
        String decoded = decodeUrl(trimmed);
        int slash = Math.max(decoded.lastIndexOf('/'), decoded.lastIndexOf('\\'));
        String name = slash >= 0 ? decoded.substring(slash + 1) : decoded;
        return name.isBlank() ? "attachment" : name;
    }

    private static String queryFilename(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = part.substring(0, idx);
            if (!"name".equalsIgnoreCase(key)) {
                continue;
            }
            String value = part.substring(idx + 1);
            if (value.isBlank()) {
                return null;
            }
            return decodeUrl(value);
        }
        return null;
    }
}
