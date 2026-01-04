package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class that provides operations for managing classrooms,
 * This class interacts with the database using JdbcTemplate to perform SQL queries.
 */
@Service
public class ClassroomService {

    /** Used to run SQL queries and updates on the database */
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Classroom> classroomRowMapper = (rs, rowNum) -> {
        Classroom classroom = new Classroom();
        classroom.setClassId(UUID.fromString(rs.getString("class_id")));
        classroom.setName(rs.getString("name"));
        classroom.setDescription(rs.getString("description"));
        return classroom;
    };

    public ClassroomService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a new classroom in the database.
     * @param classroom a container Classroom object containing the details of the new classroom.
     * When creating a new classroom, a teacher is automatically enrolled in the classroom.
     */
    @Transactional
    public void createClass(Classroom classroom) {
        if (classroom.getClassId() == null) {
            classroom.setClassId(UUID.randomUUID());
        }

        jdbcTemplate.update(
                "INSERT INTO classrooms (class_id, name, description) VALUES (?, ?, ?)",
                classroom.getClassId(), 
                classroom.getName(), 
                classroom.getDescription()
        );

        if (classroom.getCreatorId() != null) {
            addTeacher(classroom.getClassId(), classroom.getCreatorId());
        }
    }

    /**
     * Retrieves a classroom by its ID.
     * @param classId unique identifier of the classroom
     * @return an Optional containing the classroom if found, or empty otherwise
     */
    public Optional<Classroom> getClassById(UUID classId) {
        String sql = "SELECT * FROM classrooms WHERE class_id = ?";
        List<Classroom> results = jdbcTemplate.query(sql, classroomRowMapper, classId);
        return results.stream().findFirst();
    }

    /**
     * Retrieves all classrooms.
     * @return a list of all classrooms
     */
    public List<Classroom> getAllClassrooms() {
        String sql = "SELECT * FROM classrooms";
        return jdbcTemplate.query(sql, classroomRowMapper);
    }

    /**
     * Updates an existing classroom's details.
     * @param classroom Classroom object containing the updated details.
     */
    public void updateClass(Classroom classroom) {
        jdbcTemplate.update(
                "UPDATE classrooms SET name = ?, description = ? WHERE class_id = ?",
                classroom.getName(),
                classroom.getDescription(),
                classroom.getClassId()
        );
    }

    /**
     * Deletes a classroom from the database.
     * @param classId unique identifier of the classroom to be deleted.
     */
    public void removeClass(UUID classId) {
        jdbcTemplate.update("DELETE FROM classrooms WHERE class_id = ?", classId);
    }

    /**
     * Adds a teacher to the list of teachers enrolled in a classroom.
     * @param classId unique identifier of the classroom
     * @param teacherId unique identifier of the teacher to be added
     */
    public void addTeacher(UUID classId, UUID teacherId) {
        jdbcTemplate.update(
                "INSERT INTO classroom_teachers (class_id, teacher_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING", classId, teacherId
        );
    }

    /**
     * Adds a student to the list of students enrolled in a classroom.
     * @param classId unique identifier of the classroom
     * @param studentId unique identifier of the student to be added
     */
    public void addStudent(UUID classId, UUID studentId) {
        jdbcTemplate.update(
                "INSERT INTO enrollments (class_id, student_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING", classId, studentId
        );
    }

    /**
     * Removes a teacher from the list of teachers enrolled in a classroom.
     * @param classId unique identifier of the classroom
     * @param teacherId unique identifier of the teacher to be added
     */
    public void removeTeacher(UUID classId, UUID teacherId) {
        jdbcTemplate.update(
                "DELETE FROM classroom_teachers WHERE class_id = ? AND teacher_id = ?",
                classId, teacherId
        );
    }

    /**
     * Removes a student from the list of students enrolled in a classroom.
     * @param classId unique identifier of the classroom
     * @param studentId unique identifier of the student to be added
     */
    public void removeStudent(UUID classId, UUID studentId) {
        jdbcTemplate.update(
                "DELETE FROM enrollments WHERE class_id = ? AND student_id = ?",
                classId, studentId
        );
    }
    /**
     * Finds a user by their email address.
     * @param email the email to search for
     * @return an Optional containing the user_id if found
     */
    public Optional<UUID> findUserByEmail(String email) {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        List<UUID> results = jdbcTemplate.query(sql, (rs, rowNum) -> 
            UUID.fromString(rs.getString("user_id")), email);
        return results.stream().findFirst();
    }

    /**
     * Enrolls students into a classroom using their email addresses.
     * @param classId unique identifier of the classroom
     * @param studentEmails comma or space separated list of emails
     */
    @Transactional
    public void enrollStudentsByEmails(UUID classId, String studentEmails) {
        if (studentEmails == null || studentEmails.isBlank()) return;

        String[] emails = studentEmails.split("[,\\s]+");
        for (String email : emails) {
            if (!email.isBlank()) {
                findUserByEmail(email.trim()).ifPresent(studentId -> 
                    addStudent(classId, studentId)
                );
            }
        }
    }

    /**
     * Adds teachers to a classroom using their email addresses.
     * @param classId unique identifier of the classroom
     * @param teacherEmails comma or space separated list of emails
     */
    @Transactional
    public void enrollTeachersByEmails(UUID classId, String teacherEmails) {
        if (teacherEmails == null || teacherEmails.isBlank()) return;

        String[] emails = teacherEmails.split("[,\\s]+");
        for (String email : emails) {
            if (!email.isBlank()) {
                findUserByEmail(email.trim()).ifPresent(teacherId -> 
                    addTeacher(classId, teacherId)
                );
            }
        }
    }
}
