package com.binder.demo.controllers;

import com.binder.demo.classroom.Assignment;
import com.binder.demo.classroom.ClassroomPost;
import com.binder.demo.classroom.Resource;
import com.binder.demo.attachments.Attachment;
import com.binder.demo.attachments.AttachmentType;
import com.binder.demo.services.AttachmentService;
import com.binder.demo.services.ClassroomPostService;
import com.binder.demo.services.ClassroomService;
import com.binder.demo.user.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles create and edit endpoints for classroom posts.
 */
@Controller
public class ClassroomPostController {

    /**
     * Service used to manage posts.
     */
    private final ClassroomPostService classroomPostService;
    /**
     * Service used to validate classroom membership.
     */
    private final ClassroomService classroomService;
    /**
     * Service used to persist attachments.
     */
    private final AttachmentService attachmentService;
    /**
     * Base directory for uploaded files.
     */
    private final Path storageDir;

    /**
     * Creates a controller with required services.
     *
     * @param classroomPostService post service
     * @param classroomService classroom service
     * @param attachmentService attachment service
     * @param storageDir base directory for uploaded files
     */
    public ClassroomPostController(ClassroomPostService classroomPostService,
                                   ClassroomService classroomService,
                                   AttachmentService attachmentService,
                                   @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.classroomPostService = classroomPostService;
        this.classroomService = classroomService;
        this.attachmentService = attachmentService;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Creates a new assignment post.
     *
     * @param classroomId classroom id
     * @param title post title
     * @param description post description
     * @param timeToComplete time estimate
     * @param dueDate due date as ISO string
     * @param maxMarks maximum marks
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/assignment/add")
    public String addAssignment(@RequestParam UUID classroomId,
                                @RequestParam String title,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String timeToComplete,
                                @RequestParam(required = false) String dueDate,
                                @RequestParam(required = false) Integer maxMarks,
                                @RequestParam(required = false) MultipartFile[] file,
                                HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (!isTeacherInClass(userId, role, classroomId)) {
            return "redirect:/classrooms/" + classroomId;
        }

        Assignment assignment = new Assignment();
        assignment.setClassId(classroomId);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setTimeToComplete(blankToNull(timeToComplete));
        assignment.setDueDate(parseInstant(dueDate));
        assignment.setMaxMarks(maxMarks);
        assignment.setCreatorTeacherId(userId);
        assignment.setCreatedAt(Instant.now());
        attachFilesIfPresent(assignment, file, userId, false);

        classroomPostService.addAssignment(assignment);
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Creates a new resource post.
     *
     * @param classroomId classroom id
     * @param title post title
     * @param description post description
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/resource/add")
    public String addResource(@RequestParam UUID classroomId,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) MultipartFile[] file,
                              HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (!isTeacherInClass(userId, role, classroomId)) {
            return "redirect:/classrooms/" + classroomId;
        }

        Resource resource = new Resource();
        resource.setClassId(classroomId);
        resource.setTitle(title);
        resource.setDescription(description);
        resource.setCreatorTeacherId(userId);
        resource.setCreatedAt(Instant.now());
        attachFilesIfPresent(resource, file, userId, true);

        classroomPostService.addResource(resource);
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Deletes an assignment post.
     *
     * @param classroomId classroom id
     * @param assignmentId assignment id
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/assignment/remove")
    public String removeAssignment(@RequestParam UUID classroomId,
                                   @RequestParam UUID assignmentId,
                                   HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (isTeacherInClass(userId, role, classroomId)) {
            classroomPostService.removeAssignment(assignmentId);
        }
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Deletes a resource post.
     *
     * @param classroomId classroom id
     * @param resourceId resource id
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/resource/remove")
    public String removeResource(@RequestParam UUID classroomId,
                                 @RequestParam UUID resourceId,
                                 HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (isTeacherInClass(userId, role, classroomId)) {
            classroomPostService.removeResource(resourceId);
        }
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Updates an assignment post.
     *
     * @param classroomId classroom id
     * @param postId assignment id
     * @param title post title
     * @param description post description
     * @param timeToComplete time estimate
     * @param dueDate due date as ISO string
     * @param maxMarks maximum marks
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/assignment/edit")
    public String editAssignment(@RequestParam UUID classroomId,
                                 @RequestParam UUID postId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String timeToComplete,
                                 @RequestParam(required = false) String dueDate,
                                 @RequestParam(required = false) Integer maxMarks,
                                 @RequestParam(required = false) MultipartFile[] file,
                                 HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (!isTeacherInClass(userId, role, classroomId)) {
            return "redirect:/classrooms/" + classroomId;
        }

        Optional<Assignment> assignmentOpt = classroomPostService.findAssignment(postId);
        if (assignmentOpt.isEmpty()) {
            return "redirect:/classrooms/" + classroomId;
        }

        Assignment assignment = assignmentOpt.get();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setTimeToComplete(blankToNull(timeToComplete));
        assignment.setDueDate(parseInstant(dueDate));
        assignment.setMaxMarks(maxMarks);
        attachFilesIfPresent(assignment, file, userId, false);

        classroomPostService.updateAssignment(assignment);
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Updates a resource post.
     *
     * @param classroomId classroom id
     * @param postId resource id
     * @param title post title
     * @param description post description
     * @param session current session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/resource/edit")
    public String editResource(@RequestParam UUID classroomId,
                               @RequestParam UUID postId,
                               @RequestParam String title,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) MultipartFile[] file,
                               @RequestParam(required = false) Boolean replaceAttachments,
                               HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");
        if (!isTeacherInClass(userId, role, classroomId)) {
            return "redirect:/classrooms/" + classroomId;
        }

        Optional<Resource> resourceOpt = classroomPostService.findResource(postId);
        if (resourceOpt.isEmpty()) {
            return "redirect:/classrooms/" + classroomId;
        }

        Resource resource = resourceOpt.get();
        resource.setTitle(title);
        resource.setDescription(description);
        attachFilesIfPresent(resource, file, userId, Boolean.TRUE.equals(replaceAttachments));

        classroomPostService.updateResource(resource);
        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Checks if the current user is a teacher in the classroom.
     *
     * @param userId user id
     * @param role role from session
     * @param classroomId classroom id
     * @return true when the user is a teacher in the classroom
     */
    private boolean isTeacherInClass(UUID userId, String role, UUID classroomId) {
        return userId != null
                && Role.TEACHER.name().equals(role)
                && classroomService.isUserInClass(classroomId, userId, Role.TEACHER);
    }

    /**
     * Parses an ISO-8601 time instant string.
     *
     * @param value ISO string
     * @return parsed instant or null when invalid
     */
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Converts blank strings to null.
     *
     * @param value input value
     * @return trimmed value or null when blank
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * Attaches files to a post when provided, optionally replacing existing attachments.
     *
     * @param post classroom post
     * @param files uploaded files
     * @param userId owner id
     * @param replaceExisting true to replace the current attachments
     */
    private void attachFilesIfPresent(ClassroomPost post,
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
