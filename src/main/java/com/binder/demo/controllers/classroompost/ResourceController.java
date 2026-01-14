package com.binder.demo.controllers.classroompost;

import com.binder.demo.classroom.Resource;
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
 * Handles resource post endpoints.
 */
@Controller
public class ResourceController {

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
    public ResourceController(ClassroomPostService classroomPostService,
                              ClassroomService classroomService,
                              AttachmentService attachmentService,
                              @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.classroomPostService = classroomPostService;
        this.classroomService = classroomService;
        this.attachmentHelper = new PostAttachmentHelper(attachmentService,
                Path.of(storageDir).toAbsolutePath().normalize());
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
        attachmentHelper.attachFilesIfPresent(resource, file, userId, true);

        classroomPostService.addResource(resource);
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
                               @RequestParam(required = false) List<UUID> removeAttachmentIds,
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
        attachmentHelper.removeAttachmentsIfPresent(resource, removeAttachmentIds);
        attachmentHelper.attachFilesIfPresent(resource, file, userId, Boolean.TRUE.equals(replaceAttachments));

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
}
