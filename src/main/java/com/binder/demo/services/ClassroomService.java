package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import com.binder.demo.user.ROLE;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;



@Service
public class ClassroomService {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    private static final RowMapper<Classroom> CLASSROOM_ROW_MAPPER = (rs, rowNum) -> {
        Classroom classroom = new Classroom();
        classroom.setClassId(rs.getObject("class_id", UUID.class));
        classroom.setName(rs.getString("name"));
        classroom.setDescription(rs.getString("description"));
        return classroom;
    };

    public ClassroomService(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

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
            userService.addTeacherToClass(classroom.getClassId(), classroom.getCreatorId());
        }
    }

    public Optional<Classroom> getClassById(UUID classId) {
        String sql = "SELECT class_id, name, description FROM classrooms WHERE class_id = ?";
        return jdbcTemplate.query(sql, CLASSROOM_ROW_MAPPER, classId).stream().findFirst();
    }

    public List<Classroom> getAllClassrooms() {
        String sql = "SELECT class_id, name, description FROM classrooms";
        return jdbcTemplate.query(sql, CLASSROOM_ROW_MAPPER);
    }

    public void updateClass(Classroom classroom) {
        jdbcTemplate.update(
                "UPDATE classrooms SET name = ?, description = ? WHERE class_id = ?",
                classroom.getName(),
                classroom.getDescription(),
                classroom.getClassId()
        );
    }

    public void removeClass(UUID classId) {
        jdbcTemplate.update("DELETE FROM classrooms WHERE class_id = ?", classId);
    }

    public void enrollStudentsByEmails(UUID classId, String emails) {
        userService.enrollStudentsByEmails(classId, emails);
    }

    public void enrollTeachersByEmails(UUID classId, String emails) {
        userService.enrollTeachersByEmails(classId, emails);
    }

    public boolean isUserInClass(UUID classId, UUID userId, ROLE role) {
        return userService.isUserInClass(classId, userId, role);
    }

    /*
        Returns every classroom the user is linked to.
        Students are linked through enrollments.
        Teachers are linked through classroom_teachers.
     */
    public List<Classroom> getClassroomsForUser(UUID userId) {
        String sql =
                "SELECT DISTINCT c.class_id, c.name, c.description " +
                        "FROM classrooms c " +
                        "LEFT JOIN enrollments e ON c.class_id = e.class_id " +
                        "LEFT JOIN classroom_teachers ct ON c.class_id = ct.class_id " +
                        "WHERE e.student_id = ? OR ct.teacher_id = ?";

        return jdbcTemplate.query(sql, CLASSROOM_ROW_MAPPER, userId, userId);
    }

}
