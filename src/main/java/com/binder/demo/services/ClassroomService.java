package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import com.binder.demo.user.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Creates a new classroom service.
     *
     * @param userService user service for enrollment operations
     */
    public ClassroomService(UserService userService) {
        this.userService = userService;
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
    public List getClassroomsForUser(UUID userId) {
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
}
