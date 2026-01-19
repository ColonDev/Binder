package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import com.binder.demo.user.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Handles classroom enrollment and membership lookups.
 */
@Service
public class ClassroomEnrollmentService {

    /**
     * JPA entity manager used for classroom enrollment queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * User service used for enrollment changes.
     */
    private final UserService userService;

    public ClassroomEnrollmentService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Adds a teacher to a classroom.
     *
     * @param classId classroom id
     * @param teacherId teacher id
     */
    public void addTeacherToClass(UUID classId, UUID teacherId) {
        userService.addTeacherToClass(classId, teacherId);
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
    public List<String> getEnrolledStudentEmails(UUID classId) {
        if (classId == null) return List.of();

        return getClassroomEmails("""
            SELECT u.email
            FROM users u
            JOIN enrollments e ON u.user_id = e.student_id
            WHERE e.class_id = :classId
            """, classId);
    }

    /**
     * Loads enrolled teacher emails for a classroom.
     *
     * @param classId classroom id
     * @return list of teacher emails
     */
    public List<String> getEnrolledTeacherEmails(UUID classId) {
        if (classId == null) return List.of();

        return getClassroomEmails("""
            SELECT u.email
            FROM users u
            JOIN classroom_teachers ct ON u.user_id = ct.teacher_id
            WHERE ct.class_id = :classId
            """, classId);
    }

    /**
     * Removes a student from a classroom by email.
     *
     * @param classId classroom id
     * @param email student email
     */
    public void removeStudentFromClassByEmail(UUID classId, String email) {
        userService.removeStudentFromClassByEmail(classId, email);
    }

    private List<String> getClassroomEmails(String sql, UUID classId) {
        return em.createNativeQuery(sql, String.class)
                .setParameter("classId", classId)
                .getResultList();
    }
}
