package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles classroom persistence operations.
 */
@Service
public class ClassroomService {

    /**
     * JPA entity manager used for classroom persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Enrollment service used to link the creator to the classroom.
     */
    private final ClassroomEnrollmentService enrollmentService;

    public ClassroomService(ClassroomEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
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

        if (creatorTeacherId != null) {
            enrollmentService.addTeacherToClass(classroom.getClassId(), creatorTeacherId);
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
}
