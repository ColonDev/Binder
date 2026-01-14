package com.binder.demo.services;

import com.binder.demo.attachments.Attachment;
import com.binder.demo.attachments.AttachmentType;
import com.binder.demo.classroom.Assignment;
import com.binder.demo.classroom.AssignmentSubmission;
import com.binder.demo.classroom.Grade;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles assignment submission and grading operations.
 */
@Service
public class ClassroomSubmissionService {

    /**
     * JPA entity manager used for submission persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Attachment service used for submission files.
     */
    private final AttachmentService attachmentService;

    /**
     * Base directory for uploaded files.
     */
    private final Path storageDir;

    public ClassroomSubmissionService(AttachmentService attachmentService,
                                      @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.attachmentService = attachmentService;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Loads submission reviews for all assignments in a classroom.
     *
     * @param classId classroom id
     * @return list of submission reviews sorted by student name.
     */
    @Transactional(readOnly = true)
    public List<SubmissionReview> getSubmissionReviewsForClassroom(UUID classId) {
        if (classId == null) return List.of();

        List<Object[]> rows = fetchRows("""
            SELECT s.submission_id,
                   s.assignment_id,
                   a.title,
                   a.maximum_marks,
                   s.student_id,
                   u.full_name,
                   u.email,
                   s.submission_time,
                   s.attachment_id,
                   att.url,
                   g.marks_scored,
                   g.feedback
            FROM assignment_submissions s
            JOIN assignments a ON s.assignment_id = a.assignment_id
            JOIN users u ON s.student_id = u.user_id
            LEFT JOIN grades g ON g.submission_id = s.submission_id
            LEFT JOIN attachments att ON s.attachment_id = att.attachment_id
            WHERE a.class_id = :classId
            ORDER BY u.full_name, s.submission_time DESC
            """, (query) -> query.setParameter("classId", classId));

        return mapRows(rows, SubmissionReview::fromRow);
    }

    /**
     * Applies or updates a grade for a submission.
     *
     * @param classroomId classroom id
     * @param submissionId submission id
     * @param teacherId teacher id
     * @param marksScored score awarded
     * @param feedback optional feedback
     */
    @Transactional
    public void gradeSubmission(UUID classroomId,
                                UUID submissionId,
                                UUID teacherId,
                                Integer marksScored,
                                String feedback) {
        if (classroomId == null || submissionId == null || teacherId == null) return;

        List<Object[]> rows = fetchRows("""
            SELECT s.assignment_id, a.class_id, a.maximum_marks
            FROM assignment_submissions s
            JOIN assignments a ON s.assignment_id = a.assignment_id
            WHERE s.submission_id = :submissionId
            """, (query) -> query.setParameter("submissionId", submissionId));

        if (rows.isEmpty()) return;
        Object[] row = rows.get(0);
        if (row == null || row.length < 2) return;
        UUID assignmentClassId = (UUID) row[1];
        if (!classroomId.equals(assignmentClassId)) return;

        Integer maxMarks = row[2] == null ? null : ((Number) row[2]).intValue();
        AssignmentSubmission submission = em.find(AssignmentSubmission.class, submissionId);
        if (submission == null) return;

        Integer clampedMarks = clampMarks(marksScored, maxMarks);
        Grade grade = em.find(Grade.class, submissionId);
        if (grade == null) {
            grade = new Grade();
            grade.setSubmission(submission);
            grade.setSubmissionId(submissionId);
            grade.setTeacherId(teacherId);
            grade.setMarksScored(clampedMarks);
            grade.setFeedback(blankToNull(feedback));
            em.persist(grade);
            return;
        }

        grade.setTeacherId(teacherId);
        grade.setMarksScored(clampedMarks);
        grade.setFeedback(blankToNull(feedback));
        em.merge(grade);
    }

    /**
     * Loads assignment ids the student has submitted in a classroom.
     *
     * @param classId   classroom id
     * @param studentId student id
     * @return list of submitted assignment ids
     */
    @Transactional(readOnly = true)
    public List getSubmittedAssignmentIds(UUID classId, UUID studentId) {
        if (classId == null || studentId == null) return List.of();

        return em.createNativeQuery("""
            SELECT s.assignment_id
            FROM assignment_submissions s
            JOIN assignments a ON s.assignment_id = a.assignment_id
            WHERE a.class_id = :classId AND s.student_id = :studentId
            """, UUID.class)
                .setParameter("classId", classId)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    /**
     * Loads submission results for a student in a classroom.
     *
     * @param classId classroom id
     * @param studentId student id
     * @return list of submission results sorted by assignment title.
     */
    @Transactional(readOnly = true)
    public List<StudentSubmissionResult> getSubmissionResultsForStudent(UUID classId, UUID studentId) {
        if (classId == null || studentId == null) return List.of();

        List<Object[]> rows = fetchRows("""
            SELECT s.submission_id,
                   s.assignment_id,
                   a.title,
                   a.maximum_marks,
                   s.submission_time,
                   s.attachment_id,
                   att.url,
                   g.marks_scored,
                   g.feedback
            FROM assignment_submissions s
            JOIN assignments a ON s.assignment_id = a.assignment_id
            LEFT JOIN grades g ON g.submission_id = s.submission_id
            LEFT JOIN attachments att ON s.attachment_id = att.attachment_id
            WHERE a.class_id = :classId AND s.student_id = :studentId
            ORDER BY a.title, s.submission_time DESC
            """, (query) -> {
                query.setParameter("classId", classId);
                query.setParameter("studentId", studentId);
            });

        return mapRows(rows, StudentSubmissionResult::fromRow);
    }

    /**
     * Submits or updates an assignment submission for a student.
     *
     * @param classroomId classroom id
     * @param assignmentId assignment id
     * @param studentId student id
     * @param file optional submission file
     * @param markComplete whether to submit without a file
     * @param removeAttachment whether to remove an existing attachment
     */
    @Transactional
    public void submitAssignment(UUID classroomId,
                                 UUID assignmentId,
                                 UUID studentId,
                                 MultipartFile file,
                                 boolean markComplete,
                                 boolean removeAttachment) {
        if (classroomId == null || assignmentId == null || studentId == null) return;
        Assignment assignment = em.find(Assignment.class, assignmentId);
        if (assignment == null || !classroomId.equals(assignment.getClassId())) {
            return;
        }
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasFile && !markComplete && !removeAttachment) {
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
        } else if (removeAttachment) {
            submission.setAttachment(null);
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

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private Integer clampMarks(Integer marksScored, Integer maxMarks) {
        if (marksScored == null) return null;
        int clamped = Math.max(0, marksScored);
        if (maxMarks != null) {
            clamped = Math.min(clamped, maxMarks);
        }
        return clamped;
    }

    private List<Object[]> fetchRows(String sql, java.util.function.Consumer<jakarta.persistence.Query> binder) {
        var query = em.createNativeQuery(sql);
        binder.accept(query);
        List<?> raw = query.getResultList();
        List<Object[]> rows = new ArrayList<>();
        for (Object row : raw) {
            if (row instanceof Object[] arr) {
                rows.add(arr);
            }
        }
        return rows;
    }

    private <T> List<T> mapRows(List<Object[]> rows, java.util.function.Function<Object[], T> mapper) {
        List<T> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(mapper.apply(row));
        }
        return results;
    }

    /**
     * Summary view of a submission for teachers.
     */
    public record SubmissionReview(UUID submissionId,
                                   UUID assignmentId,
                                   String assignmentTitle,
                                   Integer maxMarks,
                                   UUID studentId,
                                   String studentName,
                                   String studentEmail,
                                   Instant submissionTime,
                                   UUID attachmentId,
                                   String attachmentUrl,
                                   Integer marksScored,
                                   String feedback) {
        static SubmissionReview fromRow(Object[] row) {
            CommonSubmissionFields common = CommonSubmissionFields.fromRow(row, 7, 8, 9, 10, 11);
            UUID studentId = (UUID) row[4];
            String studentName = row[5] == null ? "" : row[5].toString();
            String studentEmail = row[6] == null ? "" : row[6].toString();
            return new SubmissionReview(common.submissionId(), common.assignmentId(), common.assignmentTitle(),
                    common.maxMarks(), studentId, studentName, studentEmail, common.submissionTime(),
                    common.attachmentId(), common.attachmentUrl(), common.marksScored(), common.feedback());
        }
    }

    /**
     * Summary view of a student's submission results.
     */
    public record StudentSubmissionResult(UUID submissionId,
                                          UUID assignmentId,
                                          String assignmentTitle,
                                          Integer maxMarks,
                                          Instant submissionTime,
                                          UUID attachmentId,
                                          String attachmentUrl,
                                          Integer marksScored,
                                          String feedback) {
        static StudentSubmissionResult fromRow(Object[] row) {
            CommonSubmissionFields common = CommonSubmissionFields.fromRow(row, 4, 5, 6, 7, 8);
            return new StudentSubmissionResult(common.submissionId(), common.assignmentId(),
                    common.assignmentTitle(), common.maxMarks(), common.submissionTime(), common.attachmentId(),
                    common.attachmentUrl(), common.marksScored(), common.feedback());
        }
    }

    private record CommonSubmissionFields(UUID submissionId,
                                          UUID assignmentId,
                                          String assignmentTitle,
                                          Integer maxMarks,
                                          Instant submissionTime,
                                          UUID attachmentId,
                                          String attachmentUrl,
                                          Integer marksScored,
                                          String feedback) {
        static CommonSubmissionFields fromRow(Object[] row,
                                              int submissionTimeIndex,
                                              int attachmentIdIndex,
                                              int attachmentUrlIndex,
                                              int marksIndex,
                                              int feedbackIndex) {
            UUID submissionId = (UUID) row[0];
            UUID assignmentId = (UUID) row[1];
            String assignmentTitle = row[2] == null ? "" : row[2].toString();
            Integer maxMarks = row[3] == null ? null : ((Number) row[3]).intValue();
            Instant submissionTime = toInstant(row[submissionTimeIndex]);
            UUID attachmentId = (UUID) row[attachmentIdIndex];
            String attachmentUrl = row[attachmentUrlIndex] == null ? "" : row[attachmentUrlIndex].toString();
            Integer marksScored = row[marksIndex] == null ? null : ((Number) row[marksIndex]).intValue();
            String feedback = row[feedbackIndex] == null ? "" : row[feedbackIndex].toString();
            return new CommonSubmissionFields(submissionId, assignmentId, assignmentTitle, maxMarks,
                    submissionTime, attachmentId, attachmentUrl, marksScored, feedback);
        }
    }

    private static Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
        return null;
    }
}
