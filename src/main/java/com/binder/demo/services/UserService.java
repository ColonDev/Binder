package com.binder.demo.services;

import com.binder.demo.user.Role;
import com.binder.demo.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides user lookup and enrollment operations.
 */
@Service
public class UserService {

    /**
     * JPA entity manager used for user persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Finds a user by email.
     *
     * @param email email address to search
     * @return optional user
     */
    public Optional<User> findByEmail(String email) {
        email = (email == null) ? "" : email.trim();
        if (email.isBlank()) return Optional.empty();

        TypedQuery<User> q = em.createQuery(
                "select u from User u where u.email = :email",
                User.class
        );
        q.setParameter("email", email);

        List<User> results = q.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds a user id by email.
     *
     * @param email email address to search
     * @return optional user id
     */
    public Optional<UUID> findIdByEmail(String email) {
        email = (email == null) ? "" : email.trim();
        if (email.isBlank()) return Optional.empty();

        var q = em.createQuery(
                "select u.userId from User u where u.email = :email",
                UUID.class
        );
        q.setParameter("email", email);

        List<UUID> results = q.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds a user by id.
     *
     * @param userId user id to search
     * @return optional user
     */
    public Optional<User> findById(UUID userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(em.find(User.class, userId));
    }

    /**
     * Adds a student to a classroom if not already enrolled.
     *
     * @param classId classroom id
     * @param studentId student id
     */
    @Transactional
    public void addStudentToClass(UUID classId, UUID studentId) {
        if (classId == null || studentId == null) return;

        em.createNativeQuery("""
            INSERT INTO enrollments (class_id, student_id)
            VALUES (:classId, :studentId)
            ON CONFLICT DO NOTHING
            """)
                .setParameter("classId", classId)
                .setParameter("studentId", studentId)
                .executeUpdate();
    }

    /**
     * Adds a teacher to a classroom if not already enrolled.
     *
     * @param classId classroom id
     * @param teacherId teacher id
     */
    @Transactional
    public void addTeacherToClass(UUID classId, UUID teacherId) {
        if (classId == null || teacherId == null) return;

        em.createNativeQuery("""
            INSERT INTO classroom_teachers (class_id, teacher_id)
            VALUES (:classId, :teacherId)
            ON CONFLICT DO NOTHING
            """)
                .setParameter("classId", classId)
                .setParameter("teacherId", teacherId)
                .executeUpdate();
    }

    /**
     * Removes a student enrollment from a classroom.
     *
     * @param classId classroom id
     * @param studentId student id
     */
    @Transactional
    public void removeStudentFromClass(UUID classId, UUID studentId) {
        if (classId == null || studentId == null) return;

        em.createNativeQuery("""
            DELETE FROM enrollments
            WHERE class_id = :classId AND student_id = :studentId
            """)
                .setParameter("classId", classId)
                .setParameter("studentId", studentId)
                .executeUpdate();
    }

    /**
     * Removes a teacher enrollment from a classroom.
     *
     * @param classId classroom id
     * @param teacherId teacher id
     */
    @Transactional
    public void removeTeacherFromClass(UUID classId, UUID teacherId) {
        if (classId == null || teacherId == null) return;

        em.createNativeQuery("""
            DELETE FROM classroom_teachers
            WHERE class_id = :classId AND teacher_id = :teacherId
            """)
                .setParameter("classId", classId)
                .setParameter("teacherId", teacherId)
                .executeUpdate();
    }

    /**
     * Enrolls students from a comma or space separated email list.
     *
     * @param classId classroom id
     * @param studentEmails list of student emails
     */
    @Transactional
    public void enrollStudentsByEmails(UUID classId, String studentEmails) {
        if (classId == null) return;
        if (studentEmails == null || studentEmails.isBlank()) return;

        String[] emails = studentEmails.split("[,\\s]+");
        for (String email : emails) {
            String e = (email == null) ? "" : email.trim();
            if (e.isBlank()) continue;

            findByEmail(e).ifPresent(user -> {
                if (user.getRole() == Role.STUDENT) {
                    addStudentToClass(classId, user.getUserId());
                }
            });
        }
    }

    /**
     * Enrolls teachers from a comma or space separated email list.
     *
     * @param classId classroom id
     * @param teacherEmails list of teacher emails
     */
    @Transactional
    public void enrollTeachersByEmails(UUID classId, String teacherEmails) {
        if (classId == null) return;
        if (teacherEmails == null || teacherEmails.isBlank()) return;

        String[] emails = teacherEmails.split("[,\\s]+");
        for (String email : emails) {
            String e = (email == null) ? "" : email.trim();
            if (e.isBlank()) continue;

            findByEmail(e).ifPresent(user -> {
                if (user.getRole() == Role.TEACHER) {
                    addTeacherToClass(classId, user.getUserId());
                }
            });
        }
    }

    /**
     * Enrolls users while checking that their role matches the expected role.
     *
     * @param classId classroom id
     * @param emails list of emails
     * @param expectedRole expected role for enrollment
     * @return list of emails that did not match the expected role
     */
    @Transactional
    public List<String> enrollUsersByEmailsWithRoleValidation(UUID classId, String emails, Role expectedRole) {
        if (classId == null || expectedRole == null) return List.of();
        if (emails == null || emails.isBlank()) return List.of();

        List<String> mismatched = new java.util.ArrayList<>();
        String[] split = emails.split("[,\\s]+");
        for (String email : split) {
            String e = (email == null) ? "" : email.trim();
            if (e.isBlank()) continue;

            findByEmail(e).ifPresent(user -> {
                if (user.getRole() != expectedRole) {
                    mismatched.add(e);
                } else if (expectedRole == Role.STUDENT) {
                    addStudentToClass(classId, user.getUserId());
                } else if (expectedRole == Role.TEACHER) {
                    addTeacherToClass(classId, user.getUserId());
                }
            });
        }

        return mismatched;
    }

    /**
     * Checks whether a user is enrolled in a classroom for a given role.
     *
     * @param classId classroom id
     * @param userId user id
     * @param role role to verify
     * @return true when enrolled for the role
     */
    public boolean isUserInClass(UUID classId, UUID userId, Role role) {
        if (classId == null || userId == null || role == null) return false;

        if (role == Role.TEACHER) {
            Number n = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM classroom_teachers
                WHERE class_id = :classId AND teacher_id = :userId
                """)
                    .setParameter("classId", classId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return n.longValue() > 0;
        }

        if (role == Role.STUDENT) {
            Number n = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM enrollments
                WHERE class_id = :classId AND student_id = :userId
                """)
                    .setParameter("classId", classId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return n.longValue() > 0;
        }

        return false;
    }

    /**
     * Removes a student from a classroom using their email address.
     *
     * @param classId classroom id
     * @param email student email
     */
    @Transactional
    public void removeStudentFromClassByEmail(UUID classId, String email) {
        if (classId == null || email == null || email.isBlank()) return;

        findIdByEmail(email).ifPresent(studentId -> {
            removeStudentFromClass(classId, studentId);
        });
    }
}
