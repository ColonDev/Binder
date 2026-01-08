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

@Service
public class UserService {

    @PersistenceContext
    private EntityManager em;

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

    public Optional<User> findById(UUID userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(em.find(User.class, userId));
    }

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

    @Transactional
    public void removeStudentFromClassByEmail(UUID classId, String email) {
        if (classId == null || email == null || email.isBlank()) return;

        findIdByEmail(email).ifPresent(studentId -> {
            removeStudentFromClass(classId, studentId);
        });
    }
}
