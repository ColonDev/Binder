package com.binder.demo.controllers.classroompost;

import com.binder.demo.services.ClassroomService;
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
     * Classroom service used for submissions.
     */
    private final ClassroomService classroomService;

    /**
     * Creates a controller with required services.
     *
     * @param classroomService classroom service
     */
    public SubmissionController(ClassroomService classroomService) {
        this.classroomService = classroomService;
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
                                         HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "STUDENT".equals(role)) {
            if (classroomService.isUserInClass(classroomId, userId, com.binder.demo.user.Role.STUDENT)) {
                classroomService.submitAssignment(classroomId, assignmentId, userId, file, Boolean.TRUE.equals(markComplete));
            }
        }

        return "redirect:/classrooms/" + classroomId;
    }
}
