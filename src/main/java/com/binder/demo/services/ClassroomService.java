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

@Service
public class ClassroomService {

    @PersistenceContext
    private EntityManager em;

    private final UserService userService;

    public ClassroomService(UserService userService) {
        this.userService = userService;
    }

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

    public Optional<Classroom> getClassById(UUID classId) {
        if (classId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Classroom.class, classId));
    }

    public List<Classroom> getAllClassrooms() {
        return em.createQuery("select c from Classroom c order by c.createdAt desc", Classroom.class)
                .getResultList();
    }

    @Transactional
    public void updateClass(Classroom classroom) {
        if (classroom == null) return;
        em.merge(classroom);
    }

    @Transactional
    public void removeClass(UUID classId) {
        if (classId == null) return;
        Classroom c = em.find(Classroom.class, classId);
        if (c != null) em.remove(c);
    }

    public void enrollStudentsByEmails(UUID classId, String emails) {
        userService.enrollStudentsByEmails(classId, emails);
    }

    public void enrollTeachersByEmails(UUID classId, String emails) {
        userService.enrollTeachersByEmails(classId, emails);
    }

    public List<String> enrollStudentsByEmailsWithValidation(UUID classId, String emails) {
        return userService.enrollUsersByEmailsWithRoleValidation(classId, emails, Role.STUDENT);
    }

    public List<String> enrollTeachersByEmailsWithValidation(UUID classId, String emails) {
        return userService.enrollUsersByEmailsWithRoleValidation(classId, emails, Role.TEACHER);
    }

    public boolean isUserInClass(UUID classId, UUID userId, Role role) {
        return userService.isUserInClass(classId, userId, role);
    }

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

    @Transactional
    public void removeStudentFromClassByEmail(UUID classId, String email) {
        userService.removeStudentFromClassByEmail(classId, email);
    }
}
