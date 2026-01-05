package com.binder.demo.controllers;

import com.binder.demo.classroom.Classroom;
import com.binder.demo.services.ClassroomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller class to manage application operations related to Classroom entities.
 * This controller handles HTTP requests for all CRUD operations
 */
@Controller
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    /**
     * Handles classroom creation requests.
     * @param name        name of the classroom
     * @param description description of the classroom
     * @param session     current HTTP session used to identify the user
     * @return redirect to the dashboard after processing
     */
    @PostMapping("/classrooms/create")
    public String handleCreateClassroom(@RequestParam String name,
                                        @RequestParam String description,
                                        @RequestParam(required = false) String studentEmails,
                                        @RequestParam(required = false) String teacherEmails,
                                        HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "TEACHER".equals(role)) {
            Classroom classroom = new Classroom();
            classroom.setName(name);
            classroom.setDescription(description);
            classroom.setCreatorId(userId);
            classroomService.createClass(classroom);

            if (studentEmails != null && !studentEmails.isBlank()) {
                classroomService.enrollStudentsByEmails(classroom.getClassId(), studentEmails);
            }
            if (teacherEmails != null && !teacherEmails.isBlank()) {
                classroomService.enrollTeachersByEmails(classroom.getClassId(), teacherEmails);
            }
        }

        return "redirect:/dashboard";
    }

    /**
     * Retrieves all classrooms.
     * @return list of all classrooms
     */
    @GetMapping("/classrooms")
    @ResponseBody
    public List<Classroom> getAllClassrooms() {
        return classroomService.getAllClassrooms();
    }

    /**
     * Retrieves a single classroom by its unique identifier and displays the classroom page.
     * @param id unique identifier of the classroom
     * @param model Spring UI model
     * @param session current HTTP session
     * @return classroom view or redirect to dashboard if not found
     */
    @GetMapping("/classrooms/{id}")
    public String getClassroomById(@PathVariable UUID id, Model model, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Classroom> classroom = classroomService.getClassById(id);
        if (classroom.isPresent()) {
            model.addAttribute("classroom", classroom.get());
            model.addAttribute("name", session.getAttribute("userName"));
            model.addAttribute("role", session.getAttribute("userRole"));
            // Add empty lists for posts to avoid null checks in thymeleaf if they aren't implemented yet
            model.addAttribute("posts", List.of()); 
            return "classroom";
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles classroom update requests.
     * Updates the name and description of an existing classroom.
     * Only users with the TEACHER role are permitted to perform this action.
     * @param classId     unique identifier of the classroom to update
     * @param name        new classroom name
     * @param description new classroom description
     * @param session     current HTTP session used to identify the user
     * @return redirect to the dashboard after processing
     */
    @PostMapping("/classrooms/update")
    public String handleUpdateClassroom(@RequestParam UUID classId,
                                        @RequestParam String name,
                                        @RequestParam String description,
                                        HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "TEACHER".equals(role)) {
            Optional<Classroom> existing = classroomService.getClassById(classId);
            if (existing.isPresent()) {
                Classroom classroom = existing.get();
                classroom.setName(name);
                classroom.setDescription(description);
                classroomService.updateClass(classroom);
            }
        }

        return "redirect:/dashboard";
    }

    /**
     * Handles classroom deletion requests.
     * Removes a classroom from the system. This action is restricted
     * to users with the TEACHER role.
     * @param classId unique identifier of the classroom to delete
     * @param session current HTTP session used to identify the user
     * @return redirect to the dashboard after processing
     */
    @PostMapping("/classrooms/delete")
    public String handleDeleteClassroom(@RequestParam UUID classId,
                                        HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId != null && "TEACHER".equals(role)) {
            classroomService.removeClass(classId);
        }

        return "redirect:/dashboard";
    }

}


