package com.binder.demo.controllers.classroompost;

import com.binder.demo.classroom.Assignment;
import com.binder.demo.services.AttachmentService;
import com.binder.demo.services.ClassroomPostService;
import com.binder.demo.services.ClassroomService;
import com.binder.demo.user.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles assignment post endpoints.
 */
@Controller
public class AssignmentController {

    /**
     * Service used to manage posts.
     */
    private final ClassroomPostService classroomPostService;
    /**
     * Service used to validate classroom membership.
     */
    private final ClassroomService classroomService;
    /**
     * Helper used to manage post attachments.
     */
    private final PostAttachmentHelper attachmentHelper;

    /**
     * Creates a controller with required services.
     *
     * @param classroomPostService post service
     * @param classroomService classroom service
     * @param attachmentService attachment service
     * @param storageDir base directory for uploaded files
     */
    public AssignmentController(ClassroomPostService classroomPostService,
                                ClassroomService classroomService,
                                AttachmentService attachmentService,
                                @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.classroomPostService = classroomPostService;
        this.classroomService = classroomService;
        this.attachmentHelper = new PostAttachmentHelper(attachmentService,
                Path.of(storageDir).toAbsolutePath().normalize());
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
        attachmentHelper.attachFilesIfPresent(assignment, file, userId, false);

        classroomPostService.addAssignment(assignment);
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
                                 @RequestParam(required = false) List<UUID> removeAttachmentIds,
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
        attachmentHelper.removeAttachmentsIfPresent(assignment, removeAttachmentIds);
        attachmentHelper.attachFilesIfPresent(assignment, file, userId, false);

        classroomPostService.updateAssignment(assignment);
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
}
