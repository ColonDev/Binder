package com.binder.demo.controllers.classroompost;

import com.binder.demo.attachments.Attachment;
import com.binder.demo.attachments.AttachmentType;
import com.binder.demo.classroom.Assignment;
import com.binder.demo.classroom.ClassroomPost;
import com.binder.demo.classroom.Resource;
import com.binder.demo.services.AttachmentService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class PostAttachmentHelper {

    /**
     * Service used to persist attachments.
     */
    private final AttachmentService attachmentService;
    /**
     * Base directory for uploaded files.
     */
    private final Path storageDir;

    /**
     * Creates an attachment helper.
     *
     * @param attachmentService attachment service
     * @param storageDir base directory for uploaded files
     */
    PostAttachmentHelper(AttachmentService attachmentService, Path storageDir) {
        this.attachmentService = attachmentService;
        this.storageDir = storageDir;
    }

    /**
     * Attaches files to a post when provided, optionally replacing existing attachments.
     *
     * @param post classroom post
     * @param files uploaded files
     * @param userId owner id
     * @param replaceExisting true to replace the current attachments
     */
    void attachFilesIfPresent(ClassroomPost post,
                              MultipartFile[] files,
                              UUID userId,
                              boolean replaceExisting) {
        if (post == null || files == null || files.length == 0 || userId == null) {
            return;
        }
        Set<Attachment> stored = storeAttachments(files, userId);
        if (stored.isEmpty()) return;
        if (post instanceof Assignment assignment) {
            updateAttachments(assignment, stored, replaceExisting);
            return;
        }
        if (post instanceof Resource resource) {
            updateAttachments(resource, stored, replaceExisting);
        }
    }

    /**
     * Removes attachments from a post when requested.
     *
     * @param post classroom post to update
     * @param removeAttachmentIds attachment ids to remove
     */
    void removeAttachmentsIfPresent(ClassroomPost post, List<UUID> removeAttachmentIds) {
        if (post == null || removeAttachmentIds == null || removeAttachmentIds.isEmpty()) {
            return;
        }
        Set<UUID> ids = new HashSet<>(removeAttachmentIds);
        if (ids.isEmpty()) return;
        if (post instanceof Assignment assignment) {
            removeAttachmentsFromSet(assignment.getAttachments(), ids);
            return;
        }
        if (post instanceof Resource resource) {
            removeAttachmentsFromSet(resource.getAttachments(), ids);
        }
    }

    private void removeAttachmentsFromSet(Set<Attachment> attachments, Set<UUID> removeIds) {
        if (attachments == null || attachments.isEmpty()) return;
        attachments.removeIf((att) -> {
            UUID id = att == null ? null : att.getAttachmentId();
            return id != null && removeIds.contains(id);
        });
    }

    /**
     * Adds attachments to an assignment, optionally replacing existing attachments.
     *
     * @param assignment assignment to update
     * @param attachments new attachments
     * @param replaceExisting true to replace current attachments
     */
    private void updateAttachments(Assignment assignment,
                                   Set<Attachment> attachments,
                                   boolean replaceExisting) {
        if (replaceExisting) {
            assignment.setAttachments(attachments);
            return;
        }
        assignment.getAttachments().addAll(attachments);
    }

    /**
     * Adds attachments to a resource, optionally replacing existing attachments.
     *
     * @param resource resource to update
     * @param attachments new attachments
     * @param replaceExisting true to replace current attachments
     */
    private void updateAttachments(Resource resource,
                                   Set<Attachment> attachments,
                                   boolean replaceExisting) {
        if (replaceExisting) {
            resource.setAttachments(attachments);
            return;
        }
        resource.getAttachments().addAll(attachments);
    }

    /**
     * Writes uploaded files to disk and persists attachment metadata.
     *
     * @param files uploaded files
     * @param userId owner id
     * @return persisted attachments
     */
    private Set<Attachment> storeAttachments(MultipartFile[] files, UUID userId) {
        Set<Attachment> stored = new HashSet<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            Attachment attachment = storeAttachment(file, userId);
            if (attachment != null) {
                stored.add(attachment);
            }
        }
        return stored;
    }

    private Attachment storeAttachment(MultipartFile file, UUID userId) {
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        String attachmentId = UUID.randomUUID().toString();
        String extension = extensionFromContentType(contentType);
        String filename = attachmentId + extension;
        String displayName = safeDisplayName(file.getOriginalFilename(), filename);
        Path baseDir = storageDir.resolve("attachments").normalize();
        String relativePath = "attachments/" + filename + "?name=" + URLEncoder.encode(displayName, StandardCharsets.UTF_8);
        Path destination = baseDir.resolve(filename).normalize();

        if (!destination.startsWith(baseDir)) {
            return null;
        }

        try {
            Files.createDirectories(baseDir);
            Path realBase = baseDir.toRealPath();
            Path parent = destination.getParent();
            if (parent == null) {
                return null;
            }
            Path realParent = parent.toRealPath();
            if (!realBase.equals(realParent)) {
                return null;
            }
            if (Files.exists(destination)) {
                return null;
            }
            try (InputStream input = file.getInputStream()) {
                try (var output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                    input.transferTo(output);
                }
            }
        } catch (IOException ex) {
            return null;
        }

        Attachment attachment = new Attachment();
        attachment.setAttachmentType(contentType.startsWith("image/") ? AttachmentType.IMAGE : AttachmentType.FILE);
        attachment.setUrl(relativePath);
        attachment.setUserOwner(userId);
        return attachmentService.upload(attachment);
    }

    /**
     * Maps a content type to a safe filename extension.
     *
     * @param contentType uploaded content type
     * @return extension with a leading dot when known, otherwise empty
     */
    private String extensionFromContentType(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        return switch (type) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            default -> "";
        };
    }

    /**
     * Normalizes uploaded filenames for safe storage.
     *
     * @param originalName raw filename
     * @param fallback fallback name when missing
     * @return sanitized filename
     */
    private String safeDisplayName(String originalName, String fallback) {
        String baseName = Optional.ofNullable(originalName).orElse("");
        String fileName = baseName.isBlank() ? fallback : Path.of(baseName).getFileName().toString();
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        while (sanitized.contains("..")) {
            sanitized = sanitized.replace("..", ".");
        }
        sanitized = sanitized.replaceAll("^\\.+", "");
        if (sanitized.isBlank()) {
            sanitized = fallback;
        }
        if (sanitized.length() > 120) {
            sanitized = sanitized.substring(0, 120);
        }
        return sanitized;
    }
}
