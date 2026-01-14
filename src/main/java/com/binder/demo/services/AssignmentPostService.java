package com.binder.demo.services;

import com.binder.demo.classroom.Assignment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles assignment post persistence operations.
 */
@Service
public class AssignmentPostService {

    /**
     * JPA entity manager used for assignment persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Persists a new assignment post.
     *
     * @param assignment assignment to save
     */
    @Transactional
    public void addAssignment(Assignment assignment) {
        if (assignment == null) return;
        if (assignment.getCreatedAt() == null) assignment.setCreatedAt(Instant.now());
        em.persist(assignment);
    }

    /**
     * Deletes an assignment by id.
     *
     * @param assignmentId assignment id to delete
     */
    @Transactional
    public void removeAssignment(UUID assignmentId) {
        if (assignmentId == null) return;
        Assignment assignment = em.find(Assignment.class, assignmentId);
        if (assignment != null) em.remove(assignment);
    }

    /**
     * Finds an assignment by id.
     *
     * @param assignmentId assignment id to find
     * @return optional assignment
     */
    @Transactional(readOnly = true)
    public Optional<Assignment> findAssignment(UUID assignmentId) {
        if (assignmentId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Assignment.class, assignmentId));
    }

    /**
     * Updates an assignment with new values.
     *
     * @param assignment assignment to update
     */
    @Transactional
    public void updateAssignment(Assignment assignment) {
        if (assignment == null) return;
        em.merge(assignment);
    }
}
