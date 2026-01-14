package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import com.binder.demo.classroom.Assignment;
import com.binder.demo.classroom.AssignmentSubmission;
import com.binder.demo.attachments.Attachment;
import com.binder.demo.attachments.AttachmentType;
import com.binder.demo.user.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles classroom persistence and enrollment operations.
 */
@Service
public class ClassroomService {

    /**
     * JPA entity manager used for classroom persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * User service used for enrollment checks and updates.
     */
    private final UserService userService;
    /**
     * Attachment service used for submission files.
     */
    private final AttachmentService attachmentService;
    /**
     * Base directory for uploaded files.
     */
    private final Path storageDir;

    /**
     * Creates a new classroom service.
     *
     * @param userService user service for enrollment operations
     */
    public ClassroomService(UserService userService,
                            AttachmentService attachmentService,
                            @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.userService = userService;
        this.attachmentService = attachmentService;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Creates a classroom and optionally enrolls the creator as a teacher.
     *
     * @param classroom classroom to persist
     * @param creatorTeacherId teacher id to link to the classroom
     * @return persisted classroom
     */
    @Transactional
    public Classroom createClass(Classroom classroom, UUID creatorTeacherId) {
        if (classroom == null) throw new IllegalArgumentException("classroom is null");

        em.persist(classroom);
        em.flush(); // ensures classId is generated now

        // link the creator teacher so it shows up on their dashboard
        if (creatorTeacherId != null) {
            userService.addTeacherToClass(classroom.getClassId(), creatorTeacherId);
        }

        return classroom;
    }

    /**
     * Finds a classroom by id.
     *
     * @param classId classroom id
     * @return optional classroom
     */
    public Optional<Classroom> getClassById(UUID classId) {
        if (classId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Classroom.class, classId));
    }

    /**
     * Loads all classrooms ordered by creation time.
     *
     * @return list of classrooms
     */
    public List<Classroom> getAllClassrooms() {
        return em.createQuery("select c from Classroom c order by c.createdAt desc", Classroom.class)
                .getResultList();
    }

    /**
     * Updates a classroom record.
     *
     * @param classroom classroom to update
     */
    @Transactional
    public void updateClass(Classroom classroom) {
        if (classroom == null) return;
        em.merge(classroom);
    }

    /**
     * Deletes a classroom by id.
     *
     * @param classId classroom id to delete
     */
    @Transactional
    public void removeClass(UUID classId) {
        if (classId == null) return;
        Classroom c = em.find(Classroom.class, classId);
        if (c != null) em.remove(c);
    }

    /**
     * Enrolls students by a comma or space separated list of emails.
     *
     * @param classId classroom id
     * @param emails list of student emails
     */
    public void enrollStudentsByEmails(UUID classId, String emails) {
        userService.enrollStudentsByEmails(classId, emails);
    }

    /**
     * Enrolls teachers by a comma or space separated list of emails.
     *
     * @param classId classroom id
     * @param emails list of teacher emails
     */
    public void enrollTeachersByEmails(UUID classId, String emails) {
        userService.enrollTeachersByEmails(classId, emails);
    }

    /**
     * Enrolls students and returns any emails that do not match the role.
     *
     * @param classId classroom id
     * @param emails list of emails
     * @return list of emails that are not students
     */
    public List<String> enrollStudentsByEmailsWithValidation(UUID classId, String emails) {
        return userService.enrollUsersByEmailsWithRoleValidation(classId, emails, Role.STUDENT);
    }

    /**
     * Enrolls teachers and returns any emails that do not match the role.
     *
     * @param classId classroom id
     * @param emails list of emails
     * @return list of emails that are not teachers
     */
    public List<String> enrollTeachersByEmailsWithValidation(UUID classId, String emails) {
        return userService.enrollUsersByEmailsWithRoleValidation(classId, emails, Role.TEACHER);
    }

    /**
     * Checks whether a user is enrolled in a classroom for a role.
     *
     * @param classId classroom id
     * @param userId user id
     * @param role expected role
     * @return true when the user is enrolled for the role
     */
    public boolean isUserInClass(UUID classId, UUID userId, Role role) {
        return userService.isUserInClass(classId, userId, role);
    }

    /**
     * Returns classrooms where the user is a student or teacher.
     *
     * @param userId user id
     * @return list of classrooms
     */
    @SuppressWarnings("unchecked")
    public List<Classroom> getClassroomsForUser(UUID userId) {
        if (userId == null) return List.of();

        return em.createNativeQuery("""
            SELECT DISTINCT c.*
            FROM classrooms c
            LEFT JOIN enrollments e ON c.class_id = e.class_id
            LEFT JOIN classroom_teachers ct ON c.class_id = ct.class_id
            WHERE e.student_id = :userId OR ct.teacher_id = :userId
            ORDER BY c.created_at DESC
            """, Classroom.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Loads enrolled student emails for a classroom.
     *
     * @param classId classroom id
     * @return list of student emails
     */
    @SuppressWarnings("unchecked")
    public List<String> getEnrolledStudentEmails(UUID classId) {
        if (classId == null) return List.of();

        return em.createNativeQuery("""
            SELECT u.email
            FROM users u
            JOIN enrollments e ON u.user_id = e.student_id
            WHERE e.class_id = :classId
            """, String.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    /**
     * Loads enrolled teacher emails for a classroom.
     *
     * @param classId classroom id
     * @return list of teacher emails
     */
    @SuppressWarnings("unchecked")
    public List<String> getEnrolledTeacherEmails(UUID classId) {
        if (classId == null) return List.of();

        return em.createNativeQuery("""
            SELECT u.email
            FROM users u
            JOIN classroom_teachers ct ON u.user_id = ct.teacher_id
            WHERE ct.class_id = :classId
            """, String.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    /**
     * Removes a student from a classroom by email.
     *
     * @param classId classroom id
     * @param email student email
     */
    @Transactional
    public void removeStudentFromClassByEmail(UUID classId, String email) {
        userService.removeStudentFromClassByEmail(classId, email);
    }

    /**
     * Submits or updates an assignment submission for a student.
     *
     * @param classroomId classroom id
     * @param assignmentId assignment id
     * @param studentId student id
     * @param file optional submission file
     * @param markComplete whether to submit without a file
     */
    @Transactional
    public void submitAssignment(UUID classroomId,
                                 UUID assignmentId,
                                 UUID studentId,
                                 MultipartFile file,
                                 boolean markComplete) {
        if (classroomId == null || assignmentId == null || studentId == null) return;
        Assignment assignment = em.find(Assignment.class, assignmentId);
        if (assignment == null || !classroomId.equals(assignment.getClassId())) {
            return;
        }
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasFile && !markComplete) {
            return;
        }

        AssignmentSubmission submission = em.createQuery(
                "select s from AssignmentSubmission s where s.assignmentId = :assignmentId and s.studentId = :studentId",
                AssignmentSubmission.class
        ).setParameter("assignmentId", assignmentId)
                .setParameter("studentId", studentId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (submission == null) {
            submission = new AssignmentSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
        }

        if (hasFile) {
            Attachment attachment = storeSubmissionAttachment(file, studentId);
            if (attachment != null) {
                submission.setAttachment(attachment);
            }
        }
        submission.setSubmissionTime(Instant.now());

        if (submission.getSubmissionId() == null) {
            em.persist(submission);
        } else {
            em.merge(submission);
        }
    }

    private Attachment storeSubmissionAttachment(MultipartFile file, UUID userId) {
        if (file == null || file.isEmpty() || userId == null) return null;
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
