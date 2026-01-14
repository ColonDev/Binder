package com.binder.demo.controllers.classroompost;

import com.binder.demo.services.ClassroomEnrollmentService;
import com.binder.demo.services.ClassroomSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Handles assignment submission endpoints.
 */
@Controller
public class SubmissionController {

    /**
     * Submission service used for assignment submissions.
     */
    private final ClassroomSubmissionService submissionService;
    /**
     * Enrollment service used for membership checks.
     */
    private final ClassroomEnrollmentService enrollmentService;

    /**
     * Creates a controller with required services.
     *
     * @param submissionService submission service
     * @param enrollmentService enrollment service
     */
    public SubmissionController(ClassroomSubmissionService submissionService,
                                ClassroomEnrollmentService enrollmentService) {
        this.submissionService = submissionService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Handles assignment submissions for students.
     *
     * @param classroomId classroom id
     * @param assignmentId assignment id
     * @param file optional submission file
     * @param markComplete submit without file when true
     * @param session current HTTP session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/assignment/submit")
    public String handleSubmitAssignment(@RequestParam UUID classroomId,
                                         @RequestParam UUID assignmentId,
                                         @RequestParam(required = false) MultipartFile file,
                                         @RequestParam(required = false) Boolean markComplete,
                                         @RequestParam(required = false) Boolean removeAttachment,
                                         HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "STUDENT".equals(role)) {
            if (enrollmentService.isUserInClass(classroomId, userId, com.binder.demo.user.Role.STUDENT)) {
                submissionService.submitAssignment(classroomId, assignmentId, userId, file,
                        Boolean.TRUE.equals(markComplete), Boolean.TRUE.equals(removeAttachment));
            }
        }

        return "redirect:/classrooms/" + classroomId;
    }

    /**
     * Applies a grade to a submission.
     *
     * @param classroomId classroom id
     * @param submissionId submission id
     * @param marksScored score awarded
     * @param feedback optional feedback
     * @param session current HTTP session
     * @return redirect to classroom
     */
    @PostMapping("/classroom/post/assignment/grade")
    public String handleGradeSubmission(@RequestParam UUID classroomId,
                                        @RequestParam UUID submissionId,
                                        @RequestParam(required = false) Integer marksScored,
                                        @RequestParam(required = false) String feedback,
                                        HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "TEACHER".equals(role)) {
            if (enrollmentService.isUserInClass(classroomId, userId, com.binder.demo.user.Role.TEACHER)) {
                submissionService.gradeSubmission(classroomId, submissionId, userId, marksScored, feedback);
            }
        }

        return "redirect:/classrooms/" + classroomId;
    }
}
