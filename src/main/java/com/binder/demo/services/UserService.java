package com.binder.demo.services;

import com.binder.demo.user.ROLE;
import com.binder.demo.user.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/*
    UserService
    This service manages all user related database operations.
 */
@Service
public class UserService {

    /* Used to run SQL queries and updates on the database */
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Row mapper used to convert a row from the users table into a User object.
     */
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        String rawRole = rs.getString("role");
        if (rawRole == null) {
            throw new IllegalStateException("role is NULL for user row");
        }

        ROLE role;
        try {
            role = ROLE.valueOf(rawRole.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unknown role in DB: " + rawRole, ex);
        }

        User user = new User(role);
        user.setUserId(rs.getObject("user_id", UUID.class));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        return user;
    };

    /*
        Finds a full User object by email.
        - Returns Optional.empty() if the email is blank
        - Returns Optional.empty() if the user does not exist
        - Returns the User if found
     */
    public Optional<User> findByEmail(String email) {
        email = (email == null) ? "" : email.trim();
        if (email.isBlank()) return Optional.empty();

        String sql = """
            SELECT user_id, email, full_name, role
            FROM users
            WHERE email = ?
            """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, email));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /*
        Finds only a user's UUID by email.
     */
    public Optional<UUID> findIdByEmail(String email) {
        email = (email == null) ? "" : email.trim();
        if (email.isBlank()) return Optional.empty();

        String sql = "SELECT user_id FROM users WHERE email = ?";

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, UUID.class, email));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /*
        Finds a full User object by UUID.
     */
    public Optional<User> findById(UUID userId) {
        if (userId == null) return Optional.empty();

        String sql = """
            SELECT user_id, email, full_name, role
            FROM users
            WHERE user_id = ?
            """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, userId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /*
        Adds a student to a classroom.
     */
    @Transactional
    public void addStudentToClass(UUID classId, UUID studentId) {
        if (classId == null || studentId == null) return;

        jdbcTemplate.update(
                "INSERT INTO enrollments (class_id, student_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                classId, studentId
        );
    }

    /*
        Adds a teacher to a classroom.
     */
    @Transactional
    public void addTeacherToClass(UUID classId, UUID teacherId) {
        if (classId == null || teacherId == null) return;

        jdbcTemplate.update(
                "INSERT INTO classroom_teachers (class_id, teacher_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                classId, teacherId
        );
    }

    /*
        Removes a student from a classroom.
     */
    public void removeStudentFromClass(UUID classId, UUID studentId) {
        jdbcTemplate.update(
                "DELETE FROM enrollments WHERE class_id = ? AND student_id = ?",
                classId, studentId
        );
    }

    /*
        Removes a teacher from a classroom.
     */
    public void removeTeacherFromClass(UUID classId, UUID teacherId) {
        jdbcTemplate.update(
                "DELETE FROM classroom_teachers WHERE class_id = ? AND teacher_id = ?",
                classId, teacherId
        );
    }

    /*
        Enrolls students using a comma or whitespace separated list of emails.

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
                if (user.getRole() == ROLE.STUDENT) {
                    addStudentToClass(classId, user.getUserId());
                }
            });
        }
    }

    /*
        Enrolls teachers using a comma or whitespace separated list of emails.
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
                if (user.getRole() == ROLE.TEACHER) {
                    addTeacherToClass(classId, user.getUserId());
                }
            });
        }
    }

    /*
        Checks if a user is a member of a classroom.
        This is used to prevent users accessing classrooms by inputting a URL.
     */
    public boolean isUserInClass(UUID classId, UUID userId, ROLE role) {
        if (classId == null || userId == null || role == null) return false;

        if (role == ROLE.TEACHER) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM classroom_teachers WHERE class_id = ? AND teacher_id = ?",
                    Integer.class,
                    classId, userId
            );
            return count != null && count > 0;
        }

        if (role == ROLE.STUDENT) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM enrollments WHERE class_id = ? AND student_id = ?",
                    Integer.class,
                    classId, userId
            );
            return count != null && count > 0;
        }

        return false;
    }
}